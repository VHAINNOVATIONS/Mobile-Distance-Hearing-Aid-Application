// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.TextView;
import com.sonova.difian.R;
import com.sonova.difian.communication.fittingconnection.FittingConnectionError;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.messaging.HiMuteStatus;
import com.sonova.difian.ui.ActivityHelpers;
import com.sonova.difian.ui.UiLockingFragment;
import com.sonova.difian.utilities.Contract;

public final class FittingStatusErrorActivity extends Activity implements FittingServiceConnectionFragmentCallback
{
    private static final String UI_LOCKING_FRAGMENT_TAG = FittingStatusErrorActivity.class.getName() + ">0";
    private static final String FITTING_SERVICE_CONNECTION_FRAGMENT_TAG = FittingStatusErrorActivity.class.getName() + ">1";
    private static final String MODEL_FRAGMENT_TAG = FittingStatusErrorActivity.class.getName() + ">2";

    private static final int CONFIRM_CANCELLATION_REQUEST = 0;
    private static final int REQUEST_ENABLE_BT = 1;

    private UiLockingFragment _uiLocking;
    private FittingServiceConnectionFragment _fittingServiceConnection;
    private FittingStatusErrorModelFragment _model;

    private TextView _messageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.com_sonova_difian_ui_fitting_fittingstatuserroractivity);

        _uiLocking = ActivityHelpers.attach(this, UiLockingFragment.class, UI_LOCKING_FRAGMENT_TAG);

        _fittingServiceConnection = ActivityHelpers.attach(this, FittingServiceConnectionFragment.class, FITTING_SERVICE_CONNECTION_FRAGMENT_TAG);

        _model = ActivityHelpers.attach(this, FittingStatusErrorModelFragment.class, MODEL_FRAGMENT_TAG);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        
        Contract.check(adapter != null);
        if ((adapter != null) && !adapter.isEnabled())
        {
            if (!_uiLocking.isUiLocked())
            {
                _uiLocking.lockUi();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        _messageTextView = (TextView)findViewById(R.id.com_sonova_difian_ui_fitting_fittingstatuserroractivity_textview);

        Contract.check(_messageTextView != null);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        visualizeFittingState();

        _messageTextView.setText(_model.getErrorMessage());
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
        else if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == RESULT_CANCELED)
            {
                _uiLocking.lockUi();
                finish();
            }
            else
            {
                Contract.check(resultCode == RESULT_OK);
                visualizeFittingState();
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
        String message = null;
        if (error == FittingConnectionError.NETWORK_OPEN_FAILED)
        {
            message = getResources().getString(R.string.com_sonova_difian_ui_fitting_fittingstatuserroractivity_message_networkfailed);

        }
        else if ((error == FittingConnectionError.SERIAL_OPEN_FAILED) || (error == FittingConnectionError.SERIAL_READ_FAILED) || (error == FittingConnectionError.SERIAL_WRITE_FAILED))
        {

            message = getResources().getString(R.string.com_sonova_difian_ui_fitting_fittingstatuserroractivity_message_serialfailed);
        }
        if (message != null)
        {
            _model.setErrorMessage(message);
        }
        _messageTextView.setText(_model.getErrorMessage());

        if (!_uiLocking.isUiLocked())
        {
            if (!((error != FittingConnectionError.NONE) && (error != FittingConnectionError.NETWORK_READ_FAILED) && (error != FittingConnectionError.NETWORK_WRITE_FAILED)))
            {

                _uiLocking.lockUi();
                setResult(RESULT_OK);
                finish();
            }
            else
            {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                
                Contract.check(adapter != null);
                if ((adapter != null) && !adapter.isEnabled())
                {
                    _uiLocking.lockUi();
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        }
    }

    private void visualizeFittingState()
    {
        _fittingServiceConnection.requestUpdate();
    }
}
