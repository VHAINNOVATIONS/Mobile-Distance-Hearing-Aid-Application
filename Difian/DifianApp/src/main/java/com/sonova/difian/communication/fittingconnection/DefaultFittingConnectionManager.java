// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.fittingconnection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import com.sonova.difian.utilities.BufferPrinter;
import com.sonova.difian.utilities.Contract;
import org.yaler.AcceptCallback;
import org.yaler.AcceptCallbackState;
import org.yaler.YalerSSLServerSocket;
import org.yaler.YalerStreamingServerSocket;
import org.yaler.util.KeepAliveSocket;
import org.yaler.util.KeepAliveSocket.KeepAliveFailedListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class DefaultFittingConnectionManager implements FittingConnectionManager {

    private static final String TAG = DefaultFittingConnectionManager.class.getName();
    private static final int BUFFERSIZE = 4096;
    private static final int YALER_RETRY_ACCEPT_INTERVAL = 5000;

    private final FittingConnectionManagerCallback _listener;
    private final String _serialAddress;
    private final CompletionService<Object> _executor = new ExecutorCompletionService<Object>(Executors.newCachedThreadPool());
    private final String _relayHost;
    private final Object _lockObject = new Object();
    private final byte[] _serialRxBuffer = new byte[BUFFERSIZE];
    private final byte[] _networkRxBuffer = new byte[BUFFERSIZE];
    private final KeepAliveFailedListener _keepAliveFailedListener = new KeepAliveFailedListener() {
        @Override
        public void keepAliveFailed(Object sender) {
            synchronized (_lockObject) {
                if (sender == _network) {
                    disconnectWithError(FittingConnectionError.NETWORK_WRITE_FAILED);
                }
            }

            notifyListener();
        }
    };
    private final AcceptCallback _acceptCallback = new AcceptCallback() {
        @Override
        public void statusChanged(AcceptCallbackState state) {
            synchronized (_lockObject) {
                if ((state == AcceptCallbackState.Accessible) || (state == AcceptCallbackState.Connected)) {
                    if (_state == FittingConnectionState.CONNECTING_NETWORK) {
                        _state = FittingConnectionState.CONNECTING_WAITING;

                        // When a previous connection attempt failed during connection, clear the error.
                        // We are again accessible.
                        if ((_error == FittingConnectionError.NETWORK_OPEN_FAILED) && (_server != null)) {
                            _error = FittingConnectionError.NONE;
                        }
                    }
                } else {
                    Contract.check(state == AcceptCallbackState.Undefined);
                }
            }

            notifyListener();
        }
    };

    // Guarded by _lockObject:
    private FittingConnectionState _state = FittingConnectionState.NOT_CONNECTED;
    private FittingConnectionError _error = FittingConnectionError.NONE;
    private boolean _handlingReconnect;
    private BluetoothSocket _serial;
    private YalerStreamingServerSocket _server;
    private KeepAliveSocket _network;
    private String _id;
    private Future<Object> _connectSerialFuture;
    private Future<Object> _acceptNetworkFuture;
    private Future<Object> _readSerialFuture;
    private Future<Object> _readNetworkFuture;
    private Future<Object> _verifySerialFuture;

    DefaultFittingConnectionManager(String serialAddress, String relayHost, FittingConnectionManagerCallback listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        if (!BluetoothAdapter.checkBluetoothAddress(serialAddress)) {
            throw new IllegalArgumentException("serialAddress is not valid");
        }
        _serialAddress = serialAddress;
        _relayHost = relayHost;
        _listener = listener;
    }

    // State machine:
    //
    // state := (_state, _error, _id, _network)
    //
    // STOPPING := (STOPPING, ?, null, null)
    //
    // Old state                           Transition                   New state
    // =================================================================================================================
    // NOT_CONNECTED                    -- start() -->                  CONNECTING_SERIAL
    //                                                                  error = NONE
    //                                                                  id = null
    //
    // CONNECTING_SERIAL                -- BT unavailable -->           STOPPING
    //                                                                  error = SERIAL_OPEN_FAILED
    //
    // CONNECTING_SERIAL                -- BT socket create failed -->  STOPPING
    //                                                                  error = SERIAL_OPEN_FAILED
    //
    // CONNECTING_SERIAL                -- connect serial failed -->    STOPPING
    //                                                                  error = SERIAL_OPEN_FAILED
    //
    // CONNECTING_SERIAL                -- connect serial ok -->        CONNECTING_NETWORK
    // error = SERIAL_OPEN_FAILED                                       error = NONE
    //                                                                  id != null
    //
    // CONNECTING_SERIAL                -- connect serial ok -->        CONNECTING_NETWORK
    // error = SERIAL_READ_FAILED                                       error = NONE
    //                                                                  id != null
    //
    // CONNECTING_SERIAL                -- connect serial ok -->        CONNECTING_NETWORK
    // error = SERIAL_WRITE_FAILED                                      error = NONE
    //                                                                  id != null
    //
    // CONNECTING_SERIAL                -- connect serial ok -->        CONNECTING_NETWORK
    //                                                                  id != null
    //
    // CONNECTING_NETWORK               -- accept network failed -->    STOPPING
    //                                                                  error = NETWORK_OPEN_FAILED
    //
    // CONNECTING_NETWORK               -- accept network ok -->        network != null
    //
    // CONNECTING_NETWORK               -- verify serial failed -->     STOPPING
    //                                                                  error = SERIAL_OPEN_FAILED
    //
    // CONNECTING_NETWORK               -- verify serial ok -->         CONNECTED
    // network != null                                                  error = NONE
    //
    // CONNECTED                        -- read serial failed -->       STOPPING
    //                                                                  error = SERIAL_READ_FAILED
    //
    // CONNECTED                        -- read serial -->              STOPPING
    //                                     failed to write to network   error = NETWORK_WRITE_FAILED
    //
    // CONNECTED                        -- read network -->             STOPPING
    //                                     failed to write to serial    error = SERIAL_WRITE_FAILED
    //

    @Override
    public void start() {
        synchronized (_lockObject) {
            Contract.check(_state == FittingConnectionState.NOT_CONNECTED);
            Contract.check(!isResultOutstanding());
            _state = FittingConnectionState.CONNECTING_SERIAL;
            _error = FittingConnectionError.NONE;
            _id = null;
        }

        notifyListener();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean running = true;
                    while (running) {

                        // Begin connecting to fitting device.
                        synchronized (_lockObject) {
                            if (_state == FittingConnectionState.STOPPING) {
                                // We were stopped before any work was started.
                                running = false;
                            } else {
                                Contract.check(_state == FittingConnectionState.CONNECTING_SERIAL);

                                _serial = BluetoothSocketProvider.getSerialPort(_serialAddress);
                                if (_serial == null) {
                                    disconnectWithError(FittingConnectionError.SERIAL_OPEN_FAILED);
                                } else {
                                    _connectSerialFuture = _executor.submit(createConnectSerialTask(_serial));
                                }
                            }
                        }

                        notifyListener();

                        // Process events.
                        while (true) {
                            synchronized (_lockObject) {
                                if (!isResultOutstanding()) {
                                    //noinspection BreakStatement
                                    break;
                                }
                            }

                            Future<Object> event = _executor.take();
                            synchronized (_lockObject) {
                                processEvent(event);
                            }

                            notifyListener();
                        }

                        // Reconnect on error. Complete session when user-initiated finish.
                        synchronized (_lockObject) {
                            Contract.check(_state == FittingConnectionState.STOPPING);
                            Contract.check(!isResultOutstanding());

                            if (_error == FittingConnectionError.NONE) {
                                // Finish session.
                                running = false;
                                _state = FittingConnectionState.NOT_CONNECTED;
                            } else {
                                // Reconnect.
                                _state = FittingConnectionState.CONNECTING_SERIAL;
                                _id = null;
                            }
                        }

                        notifyListener();

                        //noinspection BusyWait
                        Thread.sleep(1000);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Uncaught exception in fitting thread: " + t, t);
                    throw new RuntimeException(t);
                }
            }
        }).start();
    }

    @Override
    public void stop() {
        synchronized (_lockObject) {
            _error = FittingConnectionError.NONE;
            if ((_state == FittingConnectionState.CONNECTING_SERIAL) ||
                (_state == FittingConnectionState.CONNECTING_NETWORK) ||
                (_state == FittingConnectionState.CONNECTING_WAITING) ||
                (_state == FittingConnectionState.CONNECTED)) {

                _state = FittingConnectionState.STOPPING;
                // shutdown must not run on main thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            synchronized (_lockObject) {
                                if (_state == FittingConnectionState.STOPPING) {
                                    shutdown();
                                }
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "Uncaught exception in stop " + TAG, t);
                            throw new RuntimeException(t);
                        }
                    }
                }).start();
            }
        }

        notifyListener();
    }

    private Callable<Object> createConnectSerialTask(final BluetoothSocket serial) {
        if (serial == null) {
            throw new IllegalArgumentException("serial is null");
        }

        return new Callable<Object>() {
            @SuppressWarnings("ProhibitedExceptionDeclared")
            @Override
            public Object call() throws Exception {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null) {
                    Log.e(TAG, "Bluetooth unavailable. (createConnectSerialTask)");
                    return null;
                }
                if (!adapter.isEnabled()) {
                    Log.e(TAG, "Bluetooth not enabled. (createConnectSerialTask)");
                    return null;
                }

                // Cancel discovery, as it may interfere with concurrent connect requests, slowing them down.
                adapter.cancelDiscovery();

                // Try to connect BluetoothSocket.
                if (!tryConnectSerial(serial)) {
                    Log.e(TAG, "Serial connect failed. (createConnectSerialTask)");
                    return null;
                }

                // Complete handshake.
                try {
                    if (!RelayIdHelper.tryReadPrefix(serial)) {
                        Log.e(TAG, "Prefix reading failed. (createConnectSerialTask)");
                        return null;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Prefix reading failed. (createConnectSerialTask)", e);
                    return null;
                }

                // Retrieve ID.
                try {
                    return RelayIdHelper.getId(serial);
                } catch (IOException e) {
                    Log.e(TAG, "ID retrieval failed. (createConnectSerialTask)", e);
                    return null;
                }
            }
        };
    }

    private void handleConnectSerialCompleted(String id) throws IOException {
        if (_state == FittingConnectionState.STOPPING) {
            return;
        }

        Contract.check(_state == FittingConnectionState.CONNECTING_SERIAL);

        _id = id;

        // Check for errors while connecting.
        if (id == null) {
            disconnectWithError(FittingConnectionError.SERIAL_OPEN_FAILED);
            return;
        }

        // Clear serial errors from previous connections (reconnect logic).
        if ((_error == FittingConnectionError.SERIAL_OPEN_FAILED) ||
            (_error == FittingConnectionError.SERIAL_READ_FAILED) ||
            (_error == FittingConnectionError.SERIAL_WRITE_FAILED)) {

            _error = FittingConnectionError.NONE;
        }

        // Transition to next state.
        _state = FittingConnectionState.CONNECTING_NETWORK;

        // WORKAROUND Cleanup hanging Yaler sockets from previous session.
        cleanupYalerConnections();

        // Create server socket, and start listening for connections immediately.
        _server = new YalerStreamingServerSocket(new YalerSSLServerSocket(_relayHost, 443, _id));
        _acceptNetworkFuture = _executor.submit(createAcceptNetworkTask(0, _server));

        // Verify that the fitting device is still connected while we wait for a network connection.
        _verifySerialFuture = _executor.submit(createVerifySerialTask(1500, _serial));
    }

    private Callable<Object> createAcceptNetworkTask(final long delay, final YalerStreamingServerSocket server) {
        if (server == null) {
            throw new IllegalArgumentException("server is null.");
        }

        return new Callable<Object>() {
            @SuppressWarnings("ProhibitedExceptionDeclared")
            @Override
            public Object call() throws Exception {
                if (delay != 0) {
                    Thread.sleep(delay);
                }

                // Accept remote client.
                Socket remoteClient = null;
                try {
                    remoteClient = server.accept(_acceptCallback);
                } catch (IOException e) {
                    Log.i(TAG, String.format("accept failed: %s", e.getMessage()));
                }

                if (remoteClient == null) {
                    return null;
                }

                // Set-up and return keep-alive socket.
                int keepAliveInterval = 5000;
                return new KeepAliveSocket(remoteClient, keepAliveInterval, _keepAliveFailedListener);
            }
        };
    }

    private void handleAcceptNetworkCompleted(KeepAliveSocket network) throws IOException {
        switch (_state) {
            case STOPPING:
                return;

            case CONNECTING_NETWORK:
            case CONNECTING_WAITING:
                _network = network;

                Contract.check(_verifySerialFuture != null);

                if (_network == null) {
                    disconnectWithError(FittingConnectionError.NETWORK_OPEN_FAILED);
                }

                // As the BluetoothSocket can not be used concurrently,
                // we start relaying only once the verify serial task completes.
                // This adds about 750ms[+/-750ms] connect latency.

                break;

            case CONNECTED:
                // Retry accept if it failed.
                if (network == null) {
                    _acceptNetworkFuture = _executor.submit(createAcceptNetworkTask(YALER_RETRY_ACCEPT_INTERVAL, _server));
                    //noinspection BreakStatement
                    break;
                }

                // Target reconnected.

                // Abort pending I/O on old network socket.
                _network.close(); // Must not run on main thread.

                // Reconnect to the new network socket.
                _network = network;

                // If there is a pending read request to the old socket, we need to wait for it to complete,
                // and discard any data it may receive as we are no longer interested in it.
                if (_readNetworkFuture != null) {
                    _handlingReconnect = true;
                    //noinspection BreakStatement
                    break;
                }

                // Resume relaying.
                _readNetworkFuture = _executor.submit(createReadNetworkTask(_network));

                // Start accepting a new connection to handle reconnects by the fitting software.
                _acceptNetworkFuture = _executor.submit(createAcceptNetworkTask(0, _server));

                break;

            case NOT_CONNECTED:
            case CONNECTING_SERIAL:
                Contract.check(false);
        }
    }

    private Callable<Object> createVerifySerialTask(final long delay, final BluetoothSocket serial) {
        if (serial == null) {
            throw new IllegalArgumentException("serial is null.");
        }

        return new Callable<Object>() {
            @SuppressWarnings("ProhibitedExceptionDeclared")
            @Override
            public Object call() throws Exception {
                //noinspection SleepWhileHoldingLock
                Thread.sleep(delay);
                try {
                    return RelayIdHelper.getId(serial);
                } catch (IOException e) {
                    Log.i(TAG, String.format("getId failed in verifySerial: %s", e.getMessage()));
                    return null;
                }
            }
        };
    }

    private void handleVerifySerialCompleted(String id) {
        if (_state == FittingConnectionState.STOPPING) {
            return;
        }

        Contract.check((_state == FittingConnectionState.CONNECTING_NETWORK) ||
            (_state == FittingConnectionState.CONNECTING_WAITING));

        if (id == null) {
            disconnectWithError(FittingConnectionError.SERIAL_OPEN_FAILED);
            return;
        }

        if (_network == null) {
            // No connection accepted yet. Keep verifying that the fitting device is still here.
            _verifySerialFuture = _executor.submit(createVerifySerialTask(1500, _serial));
            return;
        }

        // Start relaying.
        _state = FittingConnectionState.CONNECTED;
        _error = FittingConnectionError.NONE;
        _readSerialFuture = _executor.submit(createReadSerialTask(_serial));
        _readNetworkFuture = _executor.submit(createReadNetworkTask(_network));

        // Start accepting a new connection to handle reconnects by the fitting software.
        _acceptNetworkFuture = _executor.submit(createAcceptNetworkTask(0, _server));
    }

    private Callable<Object> createReadSerialTask(final BluetoothSocket serial) {
        if (serial == null) {
            throw new IllegalArgumentException("serial is null.");
        }

        return new Callable<Object>() {
            @SuppressWarnings("ProhibitedExceptionDeclared")
            @Override
            public Object call() throws Exception {
                return serial.getInputStream().read(_serialRxBuffer);
            }
        };
    }

    private void handleReadSerialCompleted(int read) {
        if (_state == FittingConnectionState.STOPPING) {
            return;
        }

        Contract.check(_state == FittingConnectionState.CONNECTED);

        Contract.check(read != 0);
        if (read == -1) {
            disconnectWithError(FittingConnectionError.SERIAL_READ_FAILED);
            return;
        }

        BufferPrinter.printBuffer(TAG + " >>>", _serialRxBuffer, read);

        // Write data from serial to network.
        try {
            _network.getOutputStream().write(_serialRxBuffer, 0, read);
        } catch (IOException e) {
            disconnectWithError(FittingConnectionError.NETWORK_WRITE_FAILED);
            return;
        }

        // Restart reading from serial.
        _readSerialFuture = _executor.submit(createReadSerialTask(_serial));
    }

    private Callable<Object> createReadNetworkTask(final KeepAliveSocket network) {
        if (network == null) {
            throw new IllegalArgumentException("network is null.");
        }

        return new Callable<Object>() {
            @SuppressWarnings("ProhibitedExceptionDeclared")
            @Override
            public Object call() throws Exception {
                return network.getInputStream().read(_networkRxBuffer);
            }
        };
    }

    private void handleReadNetworkCompleted(int read) {
        if (_state == FittingConnectionState.STOPPING) {
            return;
        }

        Contract.check(_state == FittingConnectionState.CONNECTED);

        if (_handlingReconnect) {
            // A new socket was accepted and this event is no longer valuable.
            // Swap to the new socket and drop this event.
            // Because this event came from the old socket, we are not interested in its data.
            _handlingReconnect = false;
            _readNetworkFuture = _executor.submit(createReadNetworkTask(_network));
            _acceptNetworkFuture = _executor.submit(createAcceptNetworkTask(0, _server));
            return;
        }

        if (read == -1) {
            // This is expected behaviour. We cannot distinguish between intended disconnects and connection loss.
            Log.w(TAG, "Target closed connection. Reconnect imminent.");
            return;
        }

        BufferPrinter.printBuffer(TAG + " <<<", _networkRxBuffer, read);

        // Write data from network to serial.
        try {
            _serial.getOutputStream().write(_networkRxBuffer, 0, read);
        } catch (IOException e) {
            disconnectWithError(FittingConnectionError.SERIAL_WRITE_FAILED);
            return;
        }

        // Restart reading from network.
        _readNetworkFuture = _executor.submit(createReadNetworkTask(_network));
    }

    private void processEvent(Future<Object> event) throws ExecutionException, InterruptedException, IOException {
        if (event == null) {
            throw new IllegalArgumentException("event is null.");
        }

        if (event == _connectSerialFuture) {
            Log.d(TAG, "Processing future: _connectSerialFuture");
            _connectSerialFuture = null;
            handleConnectSerialCompleted((String)event.get());
        } else if (event == _verifySerialFuture) {
            Log.d(TAG, "Processing future: _verifySerialFuture");
            _verifySerialFuture = null;
            handleVerifySerialCompleted((String)event.get());
        } else if (event == _acceptNetworkFuture) {
            Log.d(TAG, "Processing future: _acceptNetworkFuture");
            _acceptNetworkFuture = null;
            handleAcceptNetworkCompleted((KeepAliveSocket)event.get());
        } else if (event == _readSerialFuture) {
            Log.d(TAG, "Processing future: _readSerialFuture");
            _readSerialFuture = null;
            int read = -1;
            try {
                read = (Integer)event.get();
            } catch (ExecutionException e) {
                Log.i(TAG, "readSerial failed", e.getCause());
            }
            handleReadSerialCompleted(read);
        } else if (event == _readNetworkFuture) {
            Log.d(TAG, "Processing future: _readNetworkFuture");
            _readNetworkFuture = null;
            int read = -1;
            try {
                read = (Integer)event.get();
            } catch (ExecutionException e) {
                Log.i(TAG, "readNetwork failed", e.getCause());
            }
            handleReadNetworkCompleted(read);
        }
    }

    private boolean isResultOutstanding() {
        synchronized (_lockObject) {
            return (_connectSerialFuture != null) ||
                (_acceptNetworkFuture != null) ||
                (_readSerialFuture != null) ||
                (_readNetworkFuture != null) ||
                (_verifySerialFuture != null);
        }
    }

    private void disconnectWithError(FittingConnectionError error) {
        synchronized (_lockObject) {
            switch (_state) {
                case CONNECTING_SERIAL:
                case CONNECTING_NETWORK:
                case CONNECTING_WAITING:
                case CONNECTED:
                    _state = FittingConnectionState.STOPPING;
                    _error = error;
                    shutdown();
                    break;

                case STOPPING:
                    // When the user stops the connection manager, there is a brief amount of time between the state
                    // moving to STOPPING and shutdown() being called.
                    // If an error occurs during this gap, it should be suppressed, as shutdown() is already enqueued
                    // to be called eventually.
                    break;

                case NOT_CONNECTED:
                    Contract.check(false);
                    break;
            }
        }
    }

    private void shutdown() {
        synchronized (_lockObject) {
            Contract.check(_state == FittingConnectionState.STOPPING);
            if (_serial != null) {
                try {
                    _serial.getInputStream().close();
                } catch (IOException e) {
                    Log.i(TAG, "Exception during serial in close", e);
                }
                try {
                    _serial.getOutputStream().close();
                } catch (IOException e) {
                    Log.i(TAG, "Exception during serial out close", e);
                }
                try {
                    _serial.close();
                } catch (IOException e) {
                    Log.i(TAG, "Exception during serial close", e);
                }
                _serial = null;
            }
            if (_network != null) {
                try {
                    _network.close(); // must not run on main thread
                } catch (IOException e) {
                    Log.i(TAG, "Exception during network close", e);
                }
                _network = null;
            }
            if (_server != null) {
                _server.close();
                _server = null;
            }
            _id = null;
            _handlingReconnect = false;
        }
    }

    private void notifyListener() {
        FittingConnectionState state;
        FittingConnectionError error;
        String id;
        synchronized (_lockObject) {
            state = _state;
            error = _error;
            id = _id;
        }

        Log.i(TAG, String.format("DefaultFittingConnectionManager: state = %s, error = %s, id = %s", state, error, id));
        _listener.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(state, error, id));
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private boolean tryConnectSerial(BluetoothSocket serial) throws InterruptedException {
        if (serial == null) {
            throw new IllegalArgumentException("serial is null");
        }

        for (int retryCount = 0; retryCount < 3; retryCount++) {
            Log.v(TAG, "Attempting to connect BluetoothSocket.");
            IOException exception = null;
            try {
                serial.connect();
            } catch (IOException e) {
                Log.i(TAG, String.format("Failed to connect BluetoothSocket: %s %s", e.getClass().getSimpleName(), e.getMessage()));

                exception = e;
            }

            if (exception == null) {
                Log.v(TAG, "BluetoothSocket successfully connected.");
                return true;
            }

            // There are cases where Bluetooth locks up for several seconds.
            // To aid with recovery, we try to not spam too many requests in case of failure.
            if ("Unable to start Service Discovery".equals(exception.getMessage())) {
                // In this case, Bluetooth usually locks up for a longer-than-usual time.
                //noinspection BusyWait
                Thread.sleep(4500);
            } else if ("Connection timed out".equals(exception.getMessage())) {
                // In this case, a new socket is typically required.
                return false;
            } else {
                //noinspection BusyWait
                Thread.sleep(1000);
            }
        }

        Log.v(TAG, "Given up connecting BluetoothSocket.");
        return false;
    }

    private void cleanupYalerConnections() throws IOException {
        String urlString = String.format("https://%s/%s_1", _relayHost, _id);
        for (int i = 0; i < 5; i++) {
            Log.w(TAG, "Cleaning up connection");
            Log.w(TAG, "Url = " + urlString);
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setInstanceFollowRedirects(true); // does not work properly.
            connection.setRequestMethod("HEAD");
            try {
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == 504) {
                    Log.w(TAG, "Cleanup ok (504)");
                    return;
                } else if (responseCode == 307) {
                    // Handle redirect - do not count as a cleanup attempt.

                    urlString = connection.getHeaderField("Location");
                    //noinspection AssignmentToForLoopParameter
                    i--;
                } else {
                    Log.w(TAG, "Cleaned up yaler connection (status code == " + responseCode + ')');
                }
            } catch (SocketTimeoutException e) {
                Log.w(TAG, "Cleanup ok (no response from Yaler)");
                return;
            } catch (IOException e) {
                Log.w(TAG, "Cleaned up yaler connection (Exception == " + e + ')');
                return;
            }
        }

        Log.w(TAG, "Failed to clean up yaler connections.");
    }
}
