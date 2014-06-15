// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import com.sonova.difian.R;
import com.sonova.difian.communication.FittingBinder;
import com.sonova.difian.communication.chat.ChatMessage;
import com.sonova.difian.communication.chat.ChatMessageSource;
import com.sonova.difian.communication.fittingconnection.FittingConnectionError;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.messaging.HiMuteStatus;
import com.sonova.difian.ui.ActivityHelpers;
import com.sonova.difian.ui.FittingInfoHelpers;
import com.sonova.difian.ui.UiLockingFragment;
import com.sonova.difian.utilities.Contract;

public final class FittingMainActivity extends Activity implements FittingServiceConnectionFragmentCallback
{
    private static final String UI_LOCKING_FRAGMENT_TAG = FittingMainActivity.class.getName() + ">0";
    private static final String AUDIOLOGIST_INFO_FRAGMENT_TAG = AudiologistInfoFragment.class.getName() + ">1";
    private static final String FITTING_SERVICE_CONNECTION_FRAGMENT_TAG = FittingMainActivity.class.getName() + ">2";
    private static final int CONFIRM_CANCELLATION_REQUEST = 0;
    private static final int SHOW_FITTING_STATUS_REQUEST = 1;
    private boolean _confirmCancellationRequired = true;
    private UiLockingFragment _uiLocking;
    private AudiologistInfoFragment _audiologistInfoFragment;
    private FittingServiceConnectionFragment _fittingServiceConnection;
    private ChatFragment _chatFragment;
    private EditText _chatEditorMessage;
    private Button _chatEditorSend;
    private boolean _onSaveInstanceStateCalled;

    @Override
    public void onFittingServiceStateUpdated(FittingConnectionState state, FittingConnectionError error, String id, String audiologistName, Bitmap audiologistPicture, HiMuteStatus muteStatusLeft, HiMuteStatus muteStatusRight)
    {
        _confirmCancellationRequired = !_fittingServiceConnection.getBinder().isThankYou();

        if (_audiologistInfoFragment != null)
        {
            _audiologistInfoFragment.onFittingServiceStateUpdated(state, error, id, audiologistName, audiologistPicture, muteStatusLeft, muteStatusRight);
        }
        if (_chatFragment != null)
        {
            _chatFragment.updateChatMessages(_fittingServiceConnection.getBinder());
        }
        if (!_uiLocking.isUiLocked())
        {
            if ((error != FittingConnectionError.NONE) && (error != FittingConnectionError.NETWORK_READ_FAILED) && (error != FittingConnectionError.NETWORK_WRITE_FAILED))
            {
                _uiLocking.lockUi();
                startActivityForResult(new Intent(this, FittingStatusErrorActivity.class), SHOW_FITTING_STATUS_REQUEST);
            }
            if (state == FittingConnectionState.NOT_CONNECTED)
            {
                _uiLocking.lockUi();
                finish();
            }
        }
    }

    public FittingServiceConnectionFragment getFittingServiceConnection()
    {
        return _fittingServiceConnection;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_sonova_difian_ui_fitting_fittingmainactivity);
        getActionBar().setHomeButtonEnabled(true);

        _uiLocking = ActivityHelpers.attach(this, UiLockingFragment.class, UI_LOCKING_FRAGMENT_TAG);
        _fittingServiceConnection = ActivityHelpers.attach(this, FittingServiceConnectionFragment.class, FITTING_SERVICE_CONNECTION_FRAGMENT_TAG);
        _audiologistInfoFragment = ActivityHelpers.attach(this, R.id.com_sonova_difian_ui_fitting_fittingmainactivity_audiologistinfo_layout, AudiologistInfoFragment.class, AUDIOLOGIST_INFO_FRAGMENT_TAG);

        _chatFragment = (ChatFragment)getFragmentManager().findFragmentById(R.id.com_sonova_difian_ui_fitting_fittingmainactivity_chat_fragment);
        _chatEditorMessage = (EditText)findViewById(R.id.com_sonova_difian_ui_fitting_fittingmainactivity_chateditor_message);
        _chatEditorSend = (Button)findViewById(R.id.com_sonova_difian_ui_fitting_fittingmainactivity_chateditor_send);

        Contract.check(_chatFragment != null);
        Contract.check(_chatEditorMessage != null);
        Contract.check(_chatEditorSend != null);

        _chatEditorSend.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                FittingBinder binder = _fittingServiceConnection.getBinder();
                String message = _chatEditorMessage.getText().toString();
                if ((binder != null) && !message.isEmpty())
                {
                    StringBuilder finalMessageBuilder = new StringBuilder(message.length());
                    for (int i = 0; i < message.length(); i++)
                    {
                        char c = message.charAt(i);
                        boolean valid = ((c >= 0x20) && (c <= 0xd7ff)) || ((c >= 0xe000) && (c <= 0xfffd));
                        if (valid)
                        {
                            finalMessageBuilder.append(c);
                        }
                    }
                    String finalMessage = finalMessageBuilder.toString().trim();
                    if (!finalMessage.isEmpty()) {
                        binder.sendChatMessage(new ChatMessage(ChatMessageSource.USER, finalMessage));
                    }
                    _chatEditorMessage.setText("");
                }
            }
        });

        // Workaround to detect whether keyboard is shown:
        // http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android
        final View root = findViewById(R.id.com_sonova_difian_ui_fitting_fittingmainactivity_root);
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                if (!_onSaveInstanceStateCalled)
                {
                    int heightDiffPx = root.getRootView().getHeight() - root.getHeight();
                    // http://stackoverflow.com/questions/4605527/converting-pixels-to-dp-in-android
                    int heightDiff = (int)(heightDiffPx / (getResources().getDisplayMetrics().densityDpi / 160.0f));

                    if (heightDiff > 100)
                    {
                        // if more than 100 pixels, it's probably a keyboard...
                        getFragmentManager().beginTransaction().hide(_audiologistInfoFragment).commit();
                    }
                    else
                    {
                        getFragmentManager().beginTransaction().show(_audiologistInfoFragment).commit();
                    }
                }
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        _onSaveInstanceStateCalled = false;
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        FittingInfoHelpers.updateFittingDeviceIdAndRegion(this);
        visualizeFittingState();
    }

    private void visualizeFittingState()
    {
        _fittingServiceConnection.requestUpdate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        _onSaveInstanceStateCalled = true;
        super.onSaveInstanceState(outState);
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public void onBackPressed()
    {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            if (_confirmCancellationRequired)
            {
                startActivityForResult(new Intent(this, ConfirmCancellationActivity.class), CONFIRM_CANCELLATION_REQUEST);
            }
            else
            {
                finish();
            }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        else
        {
            super.onOptionsItemSelected(item);
        }
        return true;
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
        else if (requestCode == SHOW_FITTING_STATUS_REQUEST)
        {
            if (resultCode == RESULT_OK)
            {
                visualizeFittingState();
            }
            else if (resultCode == RESULT_CANCELED)
            {
                _uiLocking.lockUi();
                finish();
            }
            else
            {
                Contract.check(false);
            }
        }
        else
        {
            Log.e(FittingMainActivity.class.getSimpleName(), "Unknown activity result: " + resultCode + " / Intent = " + data);
            Contract.check(false);
        }
    }
}
