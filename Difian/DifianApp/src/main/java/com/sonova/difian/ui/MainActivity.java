// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.sonova.difian.R;
import com.sonova.difian.communication.FittingBinder;
import com.sonova.difian.communication.fittingconnection.FittingConnectionError;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.messaging.HiMuteStatus;
import com.sonova.difian.ui.fitting.FittingPowerOnActivity;
import com.sonova.difian.ui.fitting.FittingServiceConnectionFragment;
import com.sonova.difian.ui.fitting.FittingServiceConnectionFragmentCallback;
import com.sonova.difian.ui.settings.SettingsActivity;
import com.sonova.difian.ui.setup.SetupMainActivity;
import com.sonova.difian.ui.setup.SetupPowerOnActivity;
import com.sonova.difian.ui.test.TestRunningActivity;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.Strings;
import net.hockeyapp.android.UpdateManager;

public final class MainActivity extends Activity implements FittingServiceConnectionFragmentCallback
{
    public static final String ACTION_SET_UP_FITTING_DEVICE = "com.sonova.difian.ui.mainactivity.action.SET_UP_FITTING_DEVICE";
    private static final String TAG = MainActivity.class.getName();
    private static final String UI_LOCKING_FRAGMENT_TAG = TAG + ">0";
    private static final String FITTING_SERVICE_CONNECTION_FRAGMENT_TAG = TAG + ">1";
    private static final String HOCKEY_KEY = "YOUR_HOCKEY_KEY"; // TODO
    private UiLockingFragment _uiLocking;
    private FittingServiceConnectionFragment _fittingServiceConnection;

    @Override
    public void onFittingServiceStateUpdated(FittingConnectionState state, FittingConnectionError error, String id, String audiologistName, Bitmap audiologistPicture, HiMuteStatus muteStatusLeft, HiMuteStatus muteStatusRight)
    {
        FittingBinder b = _fittingServiceConnection.getBinder();
        if (b != null)
        {
            if (state != FittingConnectionState.NOT_CONNECTED) {
                startSession(null);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_sonova_difian_ui_mainactivity);
        _uiLocking = ActivityHelpers.attach(this, UiLockingFragment.class, UI_LOCKING_FRAGMENT_TAG);
        _fittingServiceConnection = ActivityHelpers.attach(this, FittingServiceConnectionFragment.class, FITTING_SERVICE_CONNECTION_FRAGMENT_TAG);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (ACTION_SET_UP_FITTING_DEVICE.equals(getIntent().getAction()))
        {
            SharedPreferences.Editor editor = preferences.edit();
            BluetoothDevice device = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String id = getIntent().getStringExtra(BluetoothDevice.EXTRA_NAME);
            editor.putString(FittingInfoHelpers.FITTING_DEVICE_ADDRESS_KEY, device.getAddress());
            editor.putString(FittingInfoHelpers.FITTING_DEVICE_ID_KEY, id);
            editor.apply();
        }
        FittingInfoHelpers.updateFittingDeviceIdAndRegion(this);
        checkForUpdates();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (_uiLocking.isUiLocked())
        {
            _uiLocking.unlockUi();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String id = preferences.getString(FittingInfoHelpers.FITTING_DEVICE_ID_KEY, null);
        if (id == null)
        {
            startActivity(new Intent(this, SetupMainActivity.class));
        }
        else
        {
            FittingInfoHelpers.updateFittingDeviceIdAndRegion(this);
        }
        checkForCrashes();
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    private void checkForCrashes()
    {
        CrashManagerListener listener = new CrashManagerListener()
        {
            public String getStringForResource(int resourceID)
            {
                switch (resourceID)
                {
                    case Strings.CRASH_DIALOG_TITLE_ID:
                        return getResources().getString(R.string.crash_dialog_title);
                    case Strings.CRASH_DIALOG_MESSAGE_ID:
                        return getResources().getString(R.string.crash_dialog_message);
                    case Strings.CRASH_DIALOG_NEGATIVE_BUTTON_ID:
                        return getResources().getString(R.string.crash_dialog_negative_button);
                    case Strings.CRASH_DIALOG_POSITIVE_BUTTON_ID:
                        return getResources().getString(R.string.crash_dialog_positive_button);
                    default:
                        return null;
                }
            }
        };
        CrashManager.register(this, HOCKEY_KEY, listener);
    }

    @Override
    public void onDestroy()
    {
        UpdateManager.unregister();
        super.onDestroy();
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public void onBackPressed()
    {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.actions_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void startSession (View view) {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            startActivity(new Intent(this, FittingPowerOnActivity.class));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean result = false;
        if (item.getItemId() == R.id.testConnection)
        {
            if (!_uiLocking.isUiLocked())
            {
                _uiLocking.lockUi();
                startActivity(new Intent(this, TestRunningActivity.class));
            }
        }
        else if (item.getItemId() == R.id.changeFittingDevice)
        {
            if (!_uiLocking.isUiLocked())
            {
                _uiLocking.lockUi();
                startActivity(new Intent(this, SetupPowerOnActivity.class));
            }
            result = true;
        }
        else if (item.getItemId() == R.id.settings)
        {
            if (!_uiLocking.isUiLocked())
            {
                _uiLocking.lockUi();
                startActivity(new Intent(this, SettingsActivity.class));
            }
            result = true;
        }
        else
        {
            result = super.onOptionsItemSelected(item);
        }
        return result;
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    private void checkForUpdates()
    {
        // Remove this for store builds!
        UpdateManager.register(this, HOCKEY_KEY);
    }

    private void visualizeFittingState()
    {
        _fittingServiceConnection.requestUpdate();
    }
}
