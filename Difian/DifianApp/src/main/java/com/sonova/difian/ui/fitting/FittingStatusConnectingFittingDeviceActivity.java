// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import com.sonova.difian.R;
import com.sonova.difian.communication.fittingconnection.FittingConnectionError;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.messaging.HiMuteStatus;
import com.sonova.difian.ui.ActivityHelpers;
import com.sonova.difian.ui.UiLockingFragment;
import com.sonova.difian.utilities.Contract;

public final class FittingStatusConnectingFittingDeviceActivity extends Activity implements FittingServiceConnectionFragmentCallback
{
    private static final String UI_LOCKING_FRAGMENT_TAG = FittingStatusConnectingFittingDeviceActivity.class.getName() + ">0";
    private static final String FITTING_SERVICE_CONNECTION_FRAGMENT_TAG = FittingStatusConnectingFittingDeviceActivity.class.getName() + ">1";

    private static final int CONFIRM_CANCELLATION_REQUEST = 0;

    private UiLockingFragment _uiLocking;
    private FittingServiceConnectionFragment _fittingServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.com_sonova_difian_ui_fitting_fittingstatusconnectingfittingdeviceactivity);

        _uiLocking = ActivityHelpers.attach(this, UiLockingFragment.class, UI_LOCKING_FRAGMENT_TAG);

        _fittingServiceConnection = ActivityHelpers.attach(this, FittingServiceConnectionFragment.class, FITTING_SERVICE_CONNECTION_FRAGMENT_TAG);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        visualizeFittingState();
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public void onBackPressed()
    {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            startActivityForResult(new Intent(this, ConfirmCancellationActivity.class), CONFIRM_CANCELLATION_REQUEST);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        boolean result;
        Rect bounds = new Rect();
        getWindow().getDecorView().getHitRect(bounds);
        if (!bounds.contains((int)ev.getX(), (int)ev.getY()))
        {
            result = true;
        }
        else
        {
            result = super.dispatchTouchEvent(ev);
        }
        return result;
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        _uiLocking.unlockUi();

        if (requestCode == CONFIRM_CANCELLATION_REQUEST)
        {
            if (resultCode == RESULT_OK)
            {
                _uiLocking.lockUi();
                finish();
            }
            else if (resultCode == RESULT_CANCELED)
            {
                visualizeFittingState();
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

    @Override
    public void onFittingServiceStateUpdated(FittingConnectionState state, FittingConnectionError error, String id, String audiologistName, Bitmap audiologistPicture, HiMuteStatus muteStatusLeft, HiMuteStatus muteStatusRight)
    {
        if (!_uiLocking.isUiLocked())
        {
            if (!((state == FittingConnectionState.CONNECTING_SERIAL) && (id == null) && (error == FittingConnectionError.NONE)))
            {
                _uiLocking.lockUi();
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    private void visualizeFittingState()
    {
        _fittingServiceConnection.requestUpdate();
    }
}
