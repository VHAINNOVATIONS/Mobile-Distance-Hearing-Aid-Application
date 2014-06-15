// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

import org.xmlpull.v1.XmlPullParser;

public final class TypingMessage extends Message
{
    private final boolean _typing;

    TypingMessage(XmlPullParser parser) throws MessageReaderException
    {
        String typingString = parser.getAttributeValue(null, "is_saving");
        if (typingString == null)
        {
            throw new MessageReaderException("is_saving attribute not present in typing message");
        }
        _typing = Boolean.parseBoolean(typingString);
    }

    @Override
    public MessageType getType()
    {
        return MessageType.TYPING;
    }

    public boolean isTyping()
    {
        return _typing;
    }
}
