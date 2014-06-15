// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.fittingconnection;

public final class FittingConnectionManagerState
{
    private final FittingConnectionState _connectionState;
    private final FittingConnectionError _connectionError;
    private final String _id;

    public static final FittingConnectionManagerState EMPTY = new FittingConnectionManagerState(FittingConnectionState.NOT_CONNECTED, FittingConnectionError.NONE, null);

    public FittingConnectionManagerState(FittingConnectionState connectionState, FittingConnectionError connectionError, String id)
    {
        _connectionState = connectionState;
        _connectionError = connectionError;
        _id = id;
    }

    public FittingConnectionState getConnectionState()
    {
        return _connectionState;
    }

    public FittingConnectionError getConnectionError()
    {
        return _connectionError;
    }

    public String getId()
    {
        return _id;
    }
}
