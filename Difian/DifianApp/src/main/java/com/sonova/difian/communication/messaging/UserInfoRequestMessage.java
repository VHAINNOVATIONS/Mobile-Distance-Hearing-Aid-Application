// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

public final class UserInfoRequestMessage extends Message
{
    @Override
    public MessageType getType()
    {
        return MessageType.USER_INFO_REQUEST;
    }
}
