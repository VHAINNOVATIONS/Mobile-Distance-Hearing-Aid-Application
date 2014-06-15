// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.fittingconnection;

public interface FittingConnectionManagerFactory
{
    FittingConnectionManager createFittingConnectionManager(String serialAddress, String relayHost, FittingConnectionManagerCallback listener);
}
