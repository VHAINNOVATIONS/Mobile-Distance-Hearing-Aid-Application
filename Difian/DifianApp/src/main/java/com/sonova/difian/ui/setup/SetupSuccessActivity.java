// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.setup;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.sonova.difian.R;
import com.sonova.difian.ui.MainActivity;

public final class SetupSuccessActivity extends Activity
{
    private boolean _lockedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_sonova_difian_ui_setup_setupsuccessactivity);
        getActionBar().setHomeButtonEnabled(true);
        ((TextView)findViewById(R.id.fittingDeviceId)).setText(getIntent().getStringExtra(BluetoothDevice.EXTRA_NAME));
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        _lockedIn = false;
    }

    @Override
    protected void onPause()
    {
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
            Intent intent = new Intent(this, MainActivity.class);
            intent.setAction(MainActivity.ACTION_SET_UP_FITTING_DEVICE);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
            intent.putExtra(BluetoothDevice.EXTRA_NAME, getIntent().getStringExtra(BluetoothDevice.EXTRA_NAME));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

}
