// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import com.sonova.difian.R;
import com.sonova.difian.ui.ActivityHelpers;
import com.sonova.difian.ui.UiLockingFragment;

public final class ConfirmCancellationActivity extends Activity
{
    private static final String UI_LOCKING_FRAGMENT_TAG = ConfirmCancellationActivity.class.getName() + '>' + UiLockingFragment.class.getName();

    private UiLockingFragment _uiLocking;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.com_sonova_difian_ui_fitting_confirmcancellationactivity);

        _uiLocking = ActivityHelpers.attach(this, UiLockingFragment.class, UI_LOCKING_FRAGMENT_TAG);
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

    @SuppressWarnings("UnusedParameters")
    public void yes(View view)
    {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            setResult(RESULT_OK);
            finish();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void no(View view)
    {
        if (!_uiLocking.isUiLocked())
        {
            _uiLocking.lockUi();
            finish();
        }
    }
}
