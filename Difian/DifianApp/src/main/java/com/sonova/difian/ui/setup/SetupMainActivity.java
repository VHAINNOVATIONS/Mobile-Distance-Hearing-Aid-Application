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
import com.sonova.difian.ui.FittingInfoHelpers;
import com.sonova.difian.ui.UiLockingFragment;
import com.sonova.difian.ui.settings.SettingsActivity;
import com.sonova.difian.ui.test.TestRunningActivity;

public final class SetupMainActivity extends Activity
{
    private static final String UI_LOCKING_FRAGMENT_TAG = SetupMainActivity.class.getName() + ">0";
    private UiLockingFragment _uiLocking;

    @Override
    public String toString()
    {
        return "SetupMainActivity{" +
                "_uiLocking=" + _uiLocking +
                '}';
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.com_sonova_difian_ui_setup_mainactivity);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.actions_com_sonova_difian_ui_setup, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void testConnection (View view) {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            startActivity(new Intent(this, TestRunningActivity.class));
        }
    }

    public void setupFittingDevice (View view) {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            startActivity(new Intent(this, SetupPowerOnActivity.class));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean result = false;
        if (!_uiLocking.isUiLocked())
        {
            if (item.getItemId() == R.id.settings)
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
        }
        return result;
    }
}
