// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.fittingconnection.test.mock;

import com.sonova.difian.communication.fittingconnection.FittingConnectionManager;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManagerCallback;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManagerFactory;

public final class MockFittingConnectionManagerFactory implements FittingConnectionManagerFactory
{
    @Override
    public FittingConnectionManager createFittingConnectionManager(String serialAddress, String relayHost, FittingConnectionManagerCallback listener)
    {
        return new MockFittingConnectionManager(serialAddress, relayHost, listener);
    }
}
