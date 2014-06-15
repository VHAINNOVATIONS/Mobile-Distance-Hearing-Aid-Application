// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

public final class UiLockingFragment extends Fragment
{
    private static final String TAG = UiLockingFragment.class.getName();

    private boolean _uiLocked;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void lockUi()
    {
        if (_uiLocked)
        {
            Log.w(TAG, "Redundant lockUi");
        }

        _uiLocked = true;
    }

    public void unlockUi()
    {
        if (!_uiLocked)
        {
            Log.w(TAG, "Redundant unlockUi");
        }

        _uiLocked = false;
    }

    public boolean isUiLocked()
    {
        return _uiLocked;
    }
}
