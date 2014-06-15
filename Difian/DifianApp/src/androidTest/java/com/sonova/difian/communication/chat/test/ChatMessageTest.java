// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.chat.test;

import com.sonova.difian.communication.chat.ChatMessage;
import com.sonova.difian.communication.chat.ChatMessageSource;
import com.sonova.difian.communication.messaging.MessageReader;
import com.sonova.difian.communication.messaging.MessageReaderException;
import com.sonova.difian.communication.messaging.SmartMessage;
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

public final class ChatMessageTest extends TestCase
{
    @SuppressWarnings("ProhibitedExceptionDeclared")
    public void testAck() throws Exception
    {
        ChatMessage um = getUserMessage();
        assertFalse("User chat message is initially not acked.", um.isAcked());
        um.ack();
        assertTrue("User chat message can be acked.", um.isAcked());

        ChatMessage sm = getSystemMessage();
        try
        {
            sm.isAcked();
            fail("System chat message does not have ack() status.");
        }
        catch (IllegalStateException ignored)
        {
        }
        try
        {
            sm.ack();
            fail("System chat message cannot be acked.");
        }
        catch (IllegalStateException ignored)
        {
        }

        ChatMessage am = getAudiologistMessage();
        try
        {
            am.isAcked();
            fail("Audiologist chat message does not have ack() status.");
        }
        catch (IllegalStateException ignored)
        {
        }
        try
        {
            am.ack();
            fail("Audiologist chat message cannot be acked.");
        }
        catch (IllegalStateException ignored)
        {
        }
    }

    @SuppressWarnings("ProhibitedExceptionDeclared")
    public void testRetry() throws Exception
    {
        ChatMessage um = getUserMessage();
        assertFalse("User chat message is initially not failed.", um.isFailed());
        um.retry();

        ChatMessage sm = getSystemMessage();
        try
        {
            sm.isFailed();
            fail("System chat message cannot fail.");
        }
        catch (IllegalStateException ignored)
        {
        }
        try
        {
            sm.retry();
            fail("System chat message cannot be retried.");
        }
        catch (IllegalStateException ignored)
        {
        }

        ChatMessage am = getAudiologistMessage();
        try
        {
            am.isFailed();
            fail("Audiologist chat message cannot fail.");
        }
        catch (IllegalStateException ignored)
        {
        }
        try
        {
            am.retry();
            fail("Audiologist chat message cannot be retried.");
        }
        catch (IllegalStateException ignored)
        {
        }
    }

    public void testRetryCap()
    {
        ChatMessage um = getUserMessage();
        assertFalse("Chat message is initially not failed.", um.isFailed());
        um.retry();
        assertFalse("Chat message is not failed after 1 delivery retry.", um.isFailed());
        um.retry();
        assertFalse("Chat message is not failed after 2 delivery retries.", um.isFailed());
        um.retry();
        assertFalse("Chat message is not failed after 3 delivery retries.", um.isFailed());
        um.retry();
        assertTrue("Chat message is failed after more than 3 delivery retries.", um.isFailed());
        try
        {
            um.retry();
            fail("Chat message cannot be retried after it failed.");
        }
        catch (IllegalStateException ignored)
        {
        }
    }

    @SuppressWarnings("ProhibitedExceptionDeclared")
    public void testAbort() throws Exception
    {
        ChatMessage um = getUserMessage();
        assertFalse("User chat message is initially not aborted.", um.isAborted());
        try
        {
            um.abort();
            fail("User chat message cannot be aborted before it has failed.");
        }
        catch (IllegalStateException ignored)
        {
        }
        um.retry();
        um.retry();
        um.retry();
        um.retry();
        um.abort();
        assertTrue("User chat message is failed.", um.isFailed());
        assertTrue("User chat message is aborted.", um.isAborted());

        ChatMessage sm = getSystemMessage();
        try
        {
            sm.isAborted();
            fail("System chat message does not have an aborted status.");
        }
        catch (IllegalStateException ignored)
        {
        }
        try
        {
            sm.abort();
            fail("System chat message cannot be aborted.");
        }
        catch (IllegalStateException ignored)
        {
        }

        ChatMessage am = getAudiologistMessage();
        try
        {
            am.isAborted();
            fail("Audiologist chat message does not have an aborted status.");
        }
        catch (IllegalStateException ignored)
        {
        }
        try
        {
            am.abort();
            fail("Audiologist chat message cannot be aborted.");
        }
        catch (IllegalStateException ignored)
        {
        }
    }

    private ChatMessage getAudiologistMessage() throws XmlPullParserException, IOException, MessageReaderException
    {
        String uuid = UUID.randomUUID().toString();
        String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(new Date());
        String message = "PUT /Test-ID HTTP/1.1\r\n\r\n<?xml version=\"1.0\" encoding=\"UTF-8\" ?><message v=\"3\" guid=\"" + uuid + "\" date=\"" + date + "\" type=\"smart\"><text>Test-Message</text><options type=\"text\"><option id=\"0\"><text>Send</text></option></options></message>";

        InputStream is = new ByteArrayInputStream(message.getBytes());
        StreamHelpers.consumeHttpHeaders(is);
        SmartMessage m = (SmartMessage)MessageReader.getInstance().parse(is);

        return new ChatMessage(ChatMessageSource.AUDIOLOGIST, m);
    }

    private ChatMessage getSystemMessage()
    {
        return new ChatMessage(ChatMessageSource.SYSTEM, ChatMessage.TEXT_CONNECTED);
    }

    private ChatMessage getUserMessage()
    {
        return new ChatMessage(ChatMessageSource.USER, "Test");
    }
}
