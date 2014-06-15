// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging.test;

import com.sonova.difian.communication.messaging.Message;
import com.sonova.difian.communication.messaging.MessageReader;
import com.sonova.difian.communication.messaging.MessageReaderException;
import com.sonova.difian.communication.messaging.MessageType;
import com.sonova.difian.communication.messaging.SmartMessage;
import com.sonova.difian.communication.messaging.SmartMessageOptionsType;
import com.sonova.difian.communication.messaging.StreamHelpers;
import junit.framework.TestCase;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public final class MessageReaderTest extends TestCase
{
    @SuppressWarnings("ProhibitedExceptionDeclared")
    public void testSmartMessageTextOnly() throws Exception
    {
        String uuid = UUID.randomUUID().toString();
        String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(new Date());
        String message = "PUT /Test-ID HTTP/1.1\r\n\r\n<?xml version=\"1.0\" encoding=\"UTF-8\" ?><message v=\"3\" guid=\"" + uuid + "\" date=\"" + date + "\" type=\"smart\"><text>Test-Message</text><options type=\"text\"><option id=\"0\"><text>Send</text></option></options></message>";

        SmartMessage m = (SmartMessage)readMessage(message);

        assertEquals("Message is SmartMessage.", MessageType.SMART, m.getType());
        assertEquals("Message has options type text.", SmartMessageOptionsType.TEXT, m.getOptionsType());
        assertEquals("Message has 1 option.", 1, m.getOptionsCount());
        assertEquals("Message option has id 0.", "0", m.getOption(0).getId());
        assertEquals("Message option has text Send.", "Send", m.getOption(0).getText());
        assertNull("Message option has no image.", m.getOption(0).getImage());
        assertEquals("Message has correct text.", "Test-Message", m.getText());
        assertNull("Message has no image.", m.getImage());
    }

    private Message readMessage(String message) throws XmlPullParserException, IOException, MessageReaderException
    {
        InputStream is = new ByteArrayInputStream(message.getBytes());
        StreamHelpers.consumeHttpHeaders(is);
        return MessageReader.getInstance().parse(is);
    }
}
