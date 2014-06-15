// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.sonova.difian.R;
import com.sonova.difian.ui.ActivityHelpers;
import com.sonova.difian.ui.UiLockingFragment;

public final class SetupPowerOnActivity extends Activity
{
    private static final String UI_LOCKING_FRAGMENT_TAG = SetupPowerOnActivity.class.getName() + ">0";

    private UiLockingFragment _uiLocking;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.com_sonova_difian_ui_setup_setuppoweronactivity);

        _uiLocking = ActivityHelpers.attach(this, UiLockingFragment.class, UI_LOCKING_FRAGMENT_TAG);

        getActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (_uiLocking.isUiLocked())
        {
            _uiLocking.unlockUi();
        }
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
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            startActivity(new Intent(this, SetupBluetoothActivity.class));
        }
    }

}
