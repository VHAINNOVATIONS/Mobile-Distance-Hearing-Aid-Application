// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

public abstract class Message
{
    private String _guid;

    public abstract MessageType getType();

    public final void setGuid(String guid)
    {
        _guid = guid;
    }

    public final String getGuid()
    {
        return _guid;
    }
}
