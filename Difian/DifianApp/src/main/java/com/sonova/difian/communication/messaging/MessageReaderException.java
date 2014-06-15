// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

@SuppressWarnings("SerializableHasSerializationMethods")
public final class MessageReaderException extends Exception
{
    private static final long serialVersionUID = 1L;

    MessageReaderException()
    {
    }

    MessageReaderException(String message)
    {
        super(message);
    }

    MessageReaderException(String message, Throwable cause)
    {
        super(message, cause);
    }

    MessageReaderException(Throwable cause)
    {
        super(cause);
    }
}
