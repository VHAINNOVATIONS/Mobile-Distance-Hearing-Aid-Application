// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

import android.util.Log;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public final class MessageReader
{
    private static final String TAG = MessageReader.class.getName();

    private static final MessageReader _instance = new MessageReader();

    public static MessageReader getInstance()
    {
        return _instance;
    }

    private MessageReader()
    {
    }

    public Message parse(InputStream stream) throws XmlPullParserException, IOException, MessageReaderException
    {
        if (stream == null)
        {
            throw new IllegalArgumentException("stream == null in parse");
        }

        Message result;

        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(stream, null);
        parser.nextTag();

        parser.require(XmlPullParser.START_TAG, null, XmlConstants.MESSAGE);
        String v = parser.getAttributeValue(null, "v");
        if ((v == null) || !"3".equals(v))
        {
            if ((v != null) && "0".equals(v))
            {
                // ignore, treat like v = 3
                Log.w(TAG, "Received <message> with \"v\" = \"0\".");
            }
            else
            {
                throw new MessageReaderException(String.format("Invalid format at <message> attribute \"v\" : %s.", v));
            }
        }
        String guid = parser.getAttributeValue(null, "guid");
        String type = parser.getAttributeValue(null, "type");
        if (type == null)
        {
            throw new MessageReaderException("\"type\" attribute does not exist in <message>.");
        }
        if ("user_info".equals(type))
        {
            result = new UserInfoMessage(parser);
        }
        else if ("hi_status".equals(type))
        {
            result = new HiStatusMessage(parser);
        }
        else if ("smart".equals(type))
        {
            result = new SmartMessage(parser);
        }
        else if ("typing".equals(type))
        {
            result = new TypingMessage(parser);
        }
        else
        {
            Log.w(TAG, String.format("Skipped message of type %s", type));
            throw new MessageReaderException(String.format("Unsupported message type while reading: %s", type));
        }
        result.setGuid(guid);

        return result;
    }
}
