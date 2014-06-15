// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.fittingconnection;

public final class DefaultFittingConnectionManagerFactory implements FittingConnectionManagerFactory
{
    @Override
    public FittingConnectionManager createFittingConnectionManager(String serialAddress, String relayHost, FittingConnectionManagerCallback listener)
    {
        return new DefaultFittingConnectionManager(serialAddress, relayHost, listener);
    }
}
