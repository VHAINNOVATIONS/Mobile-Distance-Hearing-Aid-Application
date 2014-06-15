// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.Activity;
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

public final class FittingStatusEndingSessionActivity extends Activity implements FittingServiceConnectionFragmentCallback
{
    private static final String UI_LOCKING_FRAGMENT_TAG = FittingStatusEndingSessionActivity.class.getName() + ">0";
    private static final String FITTING_SERVICE_CONNECTION_FRAGMENT_TAG = FittingStatusEndingSessionActivity.class.getName() + ">1";

    private UiLockingFragment _uiLocking;
    private FittingServiceConnectionFragment _fittingServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.com_sonova_difian_ui_fitting_fittingstatusendingsessionactivity);

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
        // Do nothing.
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

    @Override
    public void onFittingServiceStateUpdated(FittingConnectionState state, FittingConnectionError error, String id, String audiologistName, Bitmap audiologistPicture, HiMuteStatus muteStatusLeft, HiMuteStatus muteStatusRight)
    {
        if (!_uiLocking.isUiLocked())
        {
            if (state != FittingConnectionState.STOPPING)
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
