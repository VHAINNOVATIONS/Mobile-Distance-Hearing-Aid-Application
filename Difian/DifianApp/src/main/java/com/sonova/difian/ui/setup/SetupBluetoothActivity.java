// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.setup;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.sonova.difian.R;
import com.sonova.difian.utilities.Contract;

public final class SetupBluetoothActivity extends Activity
{
    private static final String TAG = SetupBluetoothActivity.class.getName();
    private static final int PICK_RELAY_REQUEST = 0;
    private static final int RETRIEVE_ID_REQUEST = 1;

    private boolean _lockedIn;
    private boolean _delayUnlock;
    private boolean _waitingForActivityResult;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_sonova_difian_ui_setup_setupbluetoothactivity);
        getActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v(TAG, "onResume"); //$NON-NLS-1$
        if (!_waitingForActivityResult)
        {
            if (_delayUnlock)
            {
                _delayUnlock = false;
            }
            else
            {
                _lockedIn = false;
            }
        }
    }

    @Override
    protected void onPause()
    {
        Log.v(TAG, "onPause"); //$NON-NLS-1$
        _lockedIn = true;
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        if (!_lockedIn)
        {
            _lockedIn = true;
            super.onBackPressed();
        }
    }

    public void previous (View view) {
        if (!_lockedIn)
        {
            _lockedIn = true;
            finish();
        }
    }

    public void next (View view) {
        if (!_lockedIn)
        {
            _lockedIn = true;
            _waitingForActivityResult = true;
            startActivityForResult(new Intent(this, FittingDevicePickerActivity.class), PICK_RELAY_REQUEST);
        }
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.v(TAG, "onActivityResult"); //$NON-NLS-1$
        if (requestCode == PICK_RELAY_REQUEST)
        {
            _waitingForActivityResult = false;
            if ((resultCode == RESULT_OK) && (data != null))
            {
                BluetoothDevice device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Contract.check(device != null);
                _lockedIn = true;
                _waitingForActivityResult = true;
                Intent intent = new Intent(this, RelayIdActivity.class);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                startActivityForResult(intent, RETRIEVE_ID_REQUEST);
            }
        }
        else if (requestCode == RETRIEVE_ID_REQUEST)
        {
            _waitingForActivityResult = false;
            if ((resultCode == RESULT_OK) && (data != null))
            {
                Log.i(TAG, "Retrieved fitting device ID: " + data.getStringExtra(BluetoothDevice.EXTRA_NAME)); //$NON-NLS-1$
                _lockedIn = true;
                _delayUnlock = true;
                Intent intent = new Intent(this, SetupSuccessActivity.class);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                intent.putExtra(BluetoothDevice.EXTRA_NAME, data.getStringExtra(BluetoothDevice.EXTRA_NAME));
                startActivity(intent);
            }
        }
    }
}
