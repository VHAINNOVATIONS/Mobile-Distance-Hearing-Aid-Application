// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.fittingconnection.test.mock;

import com.sonova.difian.communication.fittingconnection.FittingConnectionManager;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManagerCallback;

public final class MockFittingConnectionManager implements FittingConnectionManager
{
    private static FittingConnectionManagerCallback _lastListener;

    @SuppressWarnings("UnusedParameters")
    MockFittingConnectionManager(String serialAddress, String relayHost, FittingConnectionManagerCallback listener)
    {
        // noinspection AssignmentToStaticFieldFromInstanceMethod
        _lastListener = listener;
    }

    public static FittingConnectionManagerCallback getLastListener()
    {
        return _lastListener;
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }
}
