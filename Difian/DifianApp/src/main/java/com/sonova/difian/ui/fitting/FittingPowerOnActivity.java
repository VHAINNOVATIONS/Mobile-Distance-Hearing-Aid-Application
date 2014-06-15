// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import com.sonova.difian.R;
import com.sonova.difian.communication.FittingBinder;
import com.sonova.difian.communication.FittingService;
import com.sonova.difian.communication.fittingconnection.FittingConnectionError;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.messaging.HiMuteStatus;
import com.sonova.difian.ui.ActivityHelpers;
import com.sonova.difian.ui.FittingInfoHelpers;
import com.sonova.difian.ui.UiLockingFragment;
import com.sonova.difian.utilities.Contract;

public final class FittingPowerOnActivity extends Activity  implements FittingServiceConnectionFragmentCallback
{
    private static final String TAG = FittingPowerOnActivity.class.getName();
    private static final String UI_LOCKING_FRAGMENT_TAG = FittingPowerOnActivity.class.getName() + ">0";
    private static final String POPUP_ACTIVE_FRAGMENT_TAG = TAG + ">1";
    private static final String FITTING_SERVICE_CONNECTION_FRAGMENT_TAG = TAG + ">2";
    private static final int SETUP_REQUEST = 0;
    private static final int SHOW_FITTING_STATUS_REQUEST = 1;

    private UiLockingFragment _uiLocking;
    private UiLockingFragment _popupActive;
    private FittingServiceConnectionFragment _fittingServiceConnection;
    private String _fittingDeviceAddress;
    private boolean _abortSession;

    @Override
    public void onFittingServiceStateUpdated(FittingConnectionState state, FittingConnectionError error, String id, String audiologistName, Bitmap audiologistPicture, HiMuteStatus muteStatusLeft, HiMuteStatus muteStatusRight)
    {
        if (_abortSession)
        {
            FittingBinder b = _fittingServiceConnection.getBinder();
            if (b != null)
            {
                b.stop();
                _abortSession = false;
            }
        }
        if (!_popupActive.isUiLocked())
        {
            FittingBinder b = _fittingServiceConnection.getBinder();
            if (b != null)
            {
                if (state == FittingConnectionState.NOT_CONNECTED)
                {
                    Contract.check(error == FittingConnectionError.NONE);
                    // Fitting session ended.
                    stopService(new Intent(this, FittingService.class));
                    if (_uiLocking.isUiLocked())
                    {
                        finish();
                    }
                }
                else
                {
                    if (!_uiLocking.isUiLocked())
                    {
                        _uiLocking.lockUi();
                    }
                    _popupActive.lockUi();
                    if ((error != FittingConnectionError.NONE) && (error != FittingConnectionError.NETWORK_READ_FAILED) && (error != FittingConnectionError.NETWORK_WRITE_FAILED))
                    {
                        startActivityForResult(new Intent(this, FittingStatusErrorActivity.class), SHOW_FITTING_STATUS_REQUEST);
                    }
                    else if (state == FittingConnectionState.CONNECTING_SERIAL)
                    {
                        Contract.check(id == null);
                        startActivityForResult(new Intent(this, FittingStatusConnectingFittingDeviceActivity.class), SHOW_FITTING_STATUS_REQUEST);
                    }
                    else if ((state == FittingConnectionState.CONNECTING_NETWORK) && (audiologistName == null) && (_fittingServiceConnection.getBinder().getChatMessageCount() == 0))
                    {
                        Contract.check(id != null);
                        startActivityForResult(new Intent(this, FittingStatusConnectingNetworkActivity.class), SHOW_FITTING_STATUS_REQUEST);
                    }
                    else if ((state == FittingConnectionState.CONNECTING_WAITING) && (audiologistName == null) && (_fittingServiceConnection.getBinder().getChatMessageCount() == 0))
                    {
                        Contract.check(id != null);
                        startActivityForResult(new Intent(this, FittingStatusWaitingForAudiologistActivity.class), SHOW_FITTING_STATUS_REQUEST);
                    }
                    else if ((((state == FittingConnectionState.CONNECTING_NETWORK) ||
                            (state == FittingConnectionState.CONNECTING_WAITING)) &&
                            ((audiologistName != null) ||
                            (_fittingServiceConnection.getBinder().getChatMessageCount() > 0))) ||
                            (state == FittingConnectionState.CONNECTED))
                    {
                        startActivityForResult(new Intent(this, FittingMainActivity.class), SHOW_FITTING_STATUS_REQUEST);
                    }
                    else if (state == FittingConnectionState.STOPPING)
                    {
                        startActivityForResult(new Intent(this, FittingStatusEndingSessionActivity.class), SHOW_FITTING_STATUS_REQUEST);
                    }
                    else
                    {
                        Log.e(TAG, String.format("State handler not implemented for state = %s, error = %s, id = %s, name = %s.", state, error, id, audiologistName));
                        Contract.check(false);
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_sonova_difian_ui_fitting_fittingpoweronactivity);
        _uiLocking = ActivityHelpers.attach(this, UiLockingFragment.class, UI_LOCKING_FRAGMENT_TAG);
        _popupActive = ActivityHelpers.attach(this, UiLockingFragment.class, POPUP_ACTIVE_FRAGMENT_TAG);
        _fittingServiceConnection = ActivityHelpers.attach(this, FittingServiceConnectionFragment.class, FITTING_SERVICE_CONNECTION_FRAGMENT_TAG);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        _fittingDeviceAddress = preferences.getString(FittingInfoHelpers.FITTING_DEVICE_ADDRESS_KEY, null);
        getActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (_uiLocking.isUiLocked())
        {
            _uiLocking.unlockUi();
            visualizeFittingState();
        }

        FittingInfoHelpers.updateFittingDeviceIdAndRegion(this);
    }

    @Override
    protected void onPause()
    {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
        }
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            super.onBackPressed();
        }
    }

    public void previous (View view) {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            finish();
        }
    }

    public void next (View view) {
        startFittingSession(null);
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SETUP_REQUEST)
        {
            _uiLocking.unlockUi();
        }
        else if (requestCode == SHOW_FITTING_STATUS_REQUEST)
        {
            _popupActive.unlockUi();
            if (resultCode == RESULT_OK)
            {
                visualizeFittingState();
            }
            else if (resultCode == RESULT_CANCELED)
            {
                FittingBinder b = _fittingServiceConnection.getBinder();
                if (b != null)
                {
                    b.stop();
                }
                else
                {
                    _abortSession = true;
                }
            }
            else
            {
                Contract.check(false);
            }
        }
        else
        {
            Contract.check(false);
        }
    }

    @SuppressWarnings("UnusedParameters")
    void startFittingSession(View view)
    {
        if (!_uiLocking.isUiLocked())
        {
            if (_fittingDeviceAddress != null)
            {
                FittingBinder b = _fittingServiceConnection.getBinder();
                if (b != null)
                {
                    _uiLocking.lockUi();
                    startService(new Intent(getApplicationContext(), FittingService.class));
                    b.setFittingDeviceAddress(_fittingDeviceAddress);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String host = preferences.getString(FittingInfoHelpers.RELAY_REGION_KEY, null);
                    Contract.check(host != null);
                    int i = host.indexOf(':');
                    host = host.substring(0, i);
                    b.setRelayConfiguration(host);
                    b.start();
                }
            }
        }
    }

    private void visualizeFittingState()
    {
        _fittingServiceConnection.requestUpdate();
    }
}
