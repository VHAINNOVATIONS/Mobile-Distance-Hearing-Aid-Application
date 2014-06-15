// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.sonova.difian.R;
import com.sonova.difian.communication.chat.AudiologistInfo;
import com.sonova.difian.communication.chat.ChatManager;
import com.sonova.difian.communication.chat.ChatManagerCallback;
import com.sonova.difian.communication.chat.ChatMessage;
import com.sonova.difian.communication.chat.ChatMessageSource;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManager;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManagerCallback;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManagerFactory;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManagerState;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.messaging.HiMuteStatus;
import com.sonova.difian.communication.messaging.HiSide;
import com.sonova.difian.communication.timer.SessionTimer;
import com.sonova.difian.ui.MainActivity;

public final class FittingBinder extends Binder
{
    public static final String ACTION_FITTING_STATE_CHANGED = FittingBinder.class.getName() + ".ACTION_FITTING_STATE_CHANGED";
    private static final int ONGOING_NOTIFICATION = 1;
    private static final int SESSION_AUTODISCONNECT_TIMEOUT = 90 * 60 * 1000;
    private static final String TAG = FittingBinder.class.getName();
    private final Service _service;
    private final Object _lockObject = new Object();
    private final ChatManager _chat;
    private int _oldChatMessageCount;
    private final FittingConnectionManagerFactory _fittingConnectionManagerFactory;
    private final SessionTimer _timer = new SessionTimer();
    private final Handler _autoDisconnectHandler = new Handler();
    private boolean _sessionStarted;
    private boolean _thankYou;
    // Guarded by _lockObject.
    private FittingConnectionManagerState _state = FittingConnectionManagerState.EMPTY;
    // Guarded by _lockObject.
    private String _deviceAddress;
    // Guarded by _lockObject.
    private String _relayHost;
    // Guarded by _lockObject.
    private FittingConnectionManager _fittingConnection;
    private final FittingConnectionManagerCallback _listener = new FittingConnectionManagerCallback()
    {
        @Override
        public void fittingConnectionManagerStateChanged(FittingConnectionManagerState state)
        {
            synchronized (_lockObject)
            {
                Log.v(TAG, String.format("fittingConnectionManagerStateChanged, state = %s", state));
                if (state.getConnectionState() == FittingConnectionState.NOT_CONNECTED)
                {
                    _chat.shutdown();
                    _oldChatMessageCount = 0;
                    _timer.reset();
                    _autoDisconnectHandler.removeCallbacksAndMessages(null);
                    _sessionStarted = false;
                    _thankYou = false;
                }
                else
                {
                    if (state.getId() != null)
                    {
                        Log.v(TAG, String.format(
                            "fittingConnectionManagerStateChanged, calling startup, id = %s", state.getId()));
                        _chat.startup(state.getId(), _relayHost);
                    }
                }

                if (_state.getConnectionState() != state.getConnectionState())
                {
                    if (state.getConnectionState() == FittingConnectionState.CONNECTED)
                    {
                        _autoDisconnectHandler.removeCallbacksAndMessages(null);
                        _chat.addMessage(new ChatMessage(ChatMessageSource.SYSTEM, ChatMessage.TEXT_CONNECTED));
                        _sessionStarted = true;
                        _timer.start();
                    }
                    else if (_state.getConnectionState() == FittingConnectionState.CONNECTED)
                    {
                        _chat.addMessage(new ChatMessage(ChatMessageSource.SYSTEM, ChatMessage.TEXT_DISCONNECTED));
                        _timer.pause();
                        _thankYou = true;
                    }

                    if ((state.getConnectionState() == FittingConnectionState.CONNECTING_SERIAL) ||
                        (state.getConnectionState() == FittingConnectionState.CONNECTING_NETWORK) ||
                        (state.getConnectionState() == FittingConnectionState.CONNECTING_WAITING))
                    {
                        _autoDisconnectHandler.removeCallbacksAndMessages(null);
                        _autoDisconnectHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "Disconnecting due to session timeout.");
                                stop();
                            }
                        }, SESSION_AUTODISCONNECT_TIMEOUT);
                    }
                }

                _state = state;

                broadcastState();
            }
        }
    };
    private final ChatManagerCallback _chatListener = new ChatManagerCallback()
    {
        @Override
        public void chatManagerStateChanged()
        {
            synchronized (_lockObject)
            {
                if (_oldChatMessageCount != getChatMessageCount())
                {
                    _oldChatMessageCount = getChatMessageCount();
                    if (!_sessionStarted)
                    {
                        _sessionStarted = true;
                        _timer.start();
                    }
                    _autoDisconnectHandler.removeCallbacksAndMessages(null);
                    _autoDisconnectHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(FittingBinder.class.getSimpleName(), "Disconnecting due to session timeout.");
                            stop();
                        }
                    }, SESSION_AUTODISCONNECT_TIMEOUT);
                }
                if (!AudiologistInfo.EMPTY.equals(_chat.getAudiologistInfo()))
                {
                    if (!_sessionStarted)
                    {
                        _sessionStarted = true;
                        _timer.start();
                    }
                }

                broadcastState();
            }
        }
    };

    public FittingBinder(Service service, FittingConnectionManagerFactory fittingConnectionManagerFactory)
    {
        if (fittingConnectionManagerFactory == null)
        {
            throw new IllegalArgumentException("fittingConnectionManagerFactory is null");
        }
        _fittingConnectionManagerFactory = fittingConnectionManagerFactory;

        _service = service;
        _chat = new ChatManager(_chatListener);
    }

    public void setFittingDeviceAddress(String address)
    {
        synchronized (_lockObject)
        {
            if (_state.getConnectionState() != FittingConnectionState.NOT_CONNECTED)
            {
                throw new IllegalStateException("Fitting device address cannot be reconfigured while fitting session running. Wait for NOT_CONNECTED state.");
            }
            _deviceAddress = address;
        }
    }

    public void setRelayConfiguration(String host)
    {
        synchronized (_lockObject)
        {
            if (_state.getConnectionState() != FittingConnectionState.NOT_CONNECTED)
            {
                throw new IllegalStateException("Relay cannot be reconfigured while fitting session is running. Wait for NOT_CONNECTED state.");
            }
            _relayHost = host;
        }
    }

    public void start()
    {
        synchronized (_lockObject)
        {
            if (_deviceAddress == null)
            {
                throw new IllegalStateException("setFittingDeviceAddress was not called prior to starting the session.");
            }

            if (_state.getConnectionState() != FittingConnectionState.NOT_CONNECTED)
            {
                throw new IllegalStateException("A fitting session is already running. Wait for NOT_CONNECTED state.");
            }

            Intent intent = new Intent(_service, MainActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            PendingIntent pi = PendingIntent.getActivity(_service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            String title = _service.getString(R.string.notification_fittingservice_title);
            String contentText = _service.getString(R.string.notification_fittingservice_text);

            Notification n = new Notification.Builder(_service).setSmallIcon(R.drawable.icon_bw).setContentTitle(title).setContentText(contentText).setContentIntent(pi).setOngoing(true).build();

            //noinspection ProhibitedExceptionCaught
            try
            {
                _service.startForeground(ONGOING_NOTIFICATION, n);
            }
            catch (NullPointerException ignored)
            {
                // WORKAROUND Occurs during testing, as startForeground is unavailable.
                // See https://code.google.com/p/android/issues/detail?id=12122
            }

            _fittingConnection = _fittingConnectionManagerFactory.createFittingConnectionManager(_deviceAddress, _relayHost, _listener);
            _fittingConnection.start();
        }
    }

    public void stop()
    {
        synchronized (_lockObject)
        {
            if (_fittingConnection != null)
            {
                _fittingConnection.stop();

                //noinspection ProhibitedExceptionCaught
                try
                {
                    _service.stopForeground(true);
                }
                catch (NullPointerException ignored)
                {
                    // WORKAROUND Occurs during testing, as stopForeground is unavailable.
                    // See https://code.google.com/p/android/issues/detail?id=12122
                }
            }
        }
    }

    public FittingConnectionManagerState getState()
    {
        synchronized (_lockObject)
        {
            return _state;
        }
    }

    public AudiologistInfo getAudiologistInfo()
    {
        return _chat.getAudiologistInfo();
    }

    public HiMuteStatus getMuteStatus(HiSide side)
    {
        return _chat.getMuteStatus(side);
    }

    // Cannot decrease, except when fitting session is stopped where it is reset to 0.
    public int getChatMessageCount()
    {
        return _chat.getChatMessageCount();
    }

    public ChatMessage getChatMessage(int index)
    {
        return _chat.getChatMessage(index);
    }

    // Volatile value.
    public boolean isAudiologistTyping()
    {
        return _chat.isTyping();
    }

    public void sendChatMessage(ChatMessage message)
    {
        _chat.sendChatMessage(message);
    }

    public long getSessionDurationSeconds()
    {
        return _timer.getSessionSeconds();
    }

    public boolean isThankYou()
    {
        return _thankYou;
    }

    private void broadcastState()
    {
        synchronized (_lockObject)
        {
            LocalBroadcastManager m = LocalBroadcastManager.getInstance(_service);
            m.sendBroadcast(new Intent(ACTION_FITTING_STATE_CHANGED));
        }
    }
}
