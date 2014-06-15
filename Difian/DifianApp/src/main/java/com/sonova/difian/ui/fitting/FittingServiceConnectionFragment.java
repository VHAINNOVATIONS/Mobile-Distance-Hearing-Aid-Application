// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import com.sonova.difian.communication.FittingBinder;
import com.sonova.difian.communication.FittingService;
import com.sonova.difian.communication.chat.AudiologistInfo;
import com.sonova.difian.communication.fittingconnection.FittingConnectionError;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManagerState;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.messaging.HiMuteStatus;
import com.sonova.difian.communication.messaging.HiSide;
import com.sonova.difian.utilities.Contract;

public final class FittingServiceConnectionFragment extends Fragment
{
    private FittingBinder _binder;

    private final ServiceConnection _conn = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            _binder = (FittingBinder)service;
            notifyStateUpdated();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            // See also http://developer.android.com/reference/android/app/Service.html
            Contract.check(false);
        }
    };

    private final BroadcastReceiver _receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            notifyStateUpdated();
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();

        boolean success = getActivity().bindService(new Intent(getActivity(), FittingService.class), _conn, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);

        Contract.check(success);

        LocalBroadcastManager m = LocalBroadcastManager.getInstance(getActivity());
        m.registerReceiver(_receiver, new IntentFilter(FittingBinder.ACTION_FITTING_STATE_CHANGED));
    }

    @Override
    public void onPause()
    {
        super.onPause();

        getActivity().unbindService(_conn);

        LocalBroadcastManager m = LocalBroadcastManager.getInstance(getActivity());
        m.unregisterReceiver(_receiver);
    }

    public FittingBinder getBinder()
    {
        return _binder;
    }

    public void requestUpdate()
    {
        notifyStateUpdated();
    }

    private void notifyStateUpdated()
    {
        if (_binder != null)
        {
            if (getActivity() instanceof FittingServiceConnectionFragmentCallback)
            {
                FittingConnectionManagerState state = _binder.getState();

                FittingConnectionState connectionState = state.getConnectionState();
                FittingConnectionError connectionError = state.getConnectionError();
                String id = state.getId();

                AudiologistInfo info = _binder.getAudiologistInfo();
                String audiologistName = info.getName();
                Bitmap audiologistPicture = info.getPicture();

                HiMuteStatus muteStatusLeft = _binder.getMuteStatus(HiSide.LEFT);
                HiMuteStatus muteStatusRight = _binder.getMuteStatus(HiSide.RIGHT);

                ((FittingServiceConnectionFragmentCallback)getActivity()).onFittingServiceStateUpdated(connectionState, connectionError, id, audiologistName, audiologistPicture, muteStatusLeft, muteStatusRight);
            }
        }
    }
}
