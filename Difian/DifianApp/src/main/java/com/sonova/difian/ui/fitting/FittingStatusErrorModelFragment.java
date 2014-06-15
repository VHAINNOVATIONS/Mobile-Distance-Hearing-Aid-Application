// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.Fragment;
import android.os.Bundle;

public final class FittingStatusErrorModelFragment extends Fragment
{
    private String _message;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public String getErrorMessage()
    {
        return _message;
    }

    public void setErrorMessage(String message)
    {
        _message = message;
    }
}
