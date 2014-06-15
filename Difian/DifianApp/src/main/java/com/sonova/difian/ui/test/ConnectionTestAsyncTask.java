// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.test;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import com.sonova.difian.ui.FittingInfoHelpers;
import com.sonova.difian.utilities.Contract;
import org.yaler.YalerSSLServerSocket;
import org.yaler.YalerStreamingClient;
import org.yaler.YalerStreamingServerSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.UUID;

class ConnectionTestAsyncTask extends AsyncTask<Void, Void, ConnectionTestResult> {
    private static final String TAG = ConnectionTestAsyncTask.class.getSimpleName();
    private static final String EXTRA_CONN_TEST_SUCCESS = "com.sonova.difian.intent.extra.CONN_TEST_SUCCESS";
    private static final String EXTRA_CONN_TEST_LATENCY = "com.sonova.difian.intent.extra.CONN_TEST_LATENCY";
    private static final String EXTRA_CONN_TEST_ERROR_MSG = "com.sonova.difian.intent.extra.CONN_TEST_ERROR_MSG";

    private final Fragment _fragment;
    private final byte[] _testBuffer;

    private volatile YalerStreamingServerSocket _server;

    ConnectionTestAsyncTask (Fragment fragment) {
        _fragment = fragment;
        _testBuffer = new byte[1024];
        for (int i=0; i < _testBuffer.length; i++) {
            _testBuffer[i] = (byte) (Math.random() * 255);
        }
    }

    private String getRelayHost () {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(_fragment.getActivity());
        String result = preferences.getString(FittingInfoHelpers.RELAY_REGION_KEY, null);
        Contract.check(result != null);
        result = result.substring(0, result.indexOf(':'));
        return result;
    }

    @Override
    protected final ConnectionTestResult doInBackground(Void[] params)
    {
        ConnectionTestResult result = new ConnectionTestResult();

        String relayHost = getRelayHost();
        String relayDomain = String.format("difian-%s", UUID.randomUUID());

        _server = new YalerStreamingServerSocket(new YalerSSLServerSocket(relayHost, 443, relayDomain));

        Thread acceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = _server.accept();
                    if (socket != null) {
                        OutputStream stream = socket.getOutputStream();
                        Contract.check(stream != null);
                        stream.write(_testBuffer);
                        stream.close();
                        socket.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        });

        try {
            acceptThread.start();
            Thread.sleep(1337);
            Socket socket = YalerStreamingClient.connectSSLSocket(relayHost, 443, relayDomain, null);
            if (socket != null) {
                InputStream in = socket.getInputStream();
                int ch = in.read();
                int i = 0;
                while ((ch != -1) && ((byte) ch == _testBuffer[i])) {
                    ch = in.read();
                    i++;
                }
                in.close();
                socket.close();
                if ((ch == -1) && (i == _testBuffer.length)) {
                    result.state = ConnectionTestResult.STATE_SUCCESS;
                    result.latency = 0; // TODO
                } else {
                    result.state = ConnectionTestResult.STATE_ERROR;
                }
            } else {
                result.state = ConnectionTestResult.STATE_ERROR;
            }
        } catch (MalformedURLException e) {
            result.state = ConnectionTestResult.STATE_ERROR;
            result.exception = e;
        } catch (IOException e) {
            result.state = ConnectionTestResult.STATE_ERROR;
            result.exception = e;
        } catch (InterruptedException e) {
            result.state = ConnectionTestResult.STATE_ERROR;
            result.exception = e;
        } catch (Throwable t) { // TODO: remove?
            Log.v(TAG, t.toString());
        } finally {
            _server.close();
            try {
                acceptThread.join();
            } catch (InterruptedException e) {
                result.state = ConnectionTestResult.STATE_ERROR;
                result.exception = e;
            }
        }
        return result;
    }

    @Override
    protected final void onCancelled () {
        super.onCancelled();
        if (_server != null) {
            _server.close();
        }
    }

    @Override
    protected final void onPostExecute (ConnectionTestResult result) {
        super.onPostExecute(result);
        _fragment.getActivity().finish();
        Intent i = new Intent(_fragment.getActivity(), TestResultActivity.class);
        if (result.state == ConnectionTestResult.STATE_SUCCESS) {
            i.putExtra(EXTRA_CONN_TEST_SUCCESS, true);
            i.putExtra(EXTRA_CONN_TEST_LATENCY, result.latency);
        } else if (result.state == ConnectionTestResult.STATE_ERROR) {
            i.putExtra(EXTRA_CONN_TEST_SUCCESS, false);
            if (result.exception != null) {
                i.putExtra(EXTRA_CONN_TEST_ERROR_MSG, result.exception.getMessage());
            }
        } else {
            Contract.check(false);
        }
        _fragment.getActivity().startActivity(i);
    }
}
