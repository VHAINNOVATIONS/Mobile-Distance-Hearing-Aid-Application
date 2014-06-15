// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.chat.test;

import com.sonova.difian.communication.chat.ChatManager;
import com.sonova.difian.communication.chat.ChatManagerCallback;
import com.sonova.difian.communication.chat.ChatMessage;
import com.sonova.difian.communication.chat.ChatMessageSource;
import junit.framework.TestCase;

import java.util.UUID;

public final class ChatManagerTest extends TestCase
{
    private ChatManager _chat;
    private String _id;

    @SuppressWarnings("ProhibitedExceptionDeclared")
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        _id = "Test-ID-" + UUID.randomUUID();
    }

    @SuppressWarnings("ProhibitedExceptionDeclared")
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        if (_chat != null)
        {
            _chat.shutdown();
        }
        _chat = null;
    }

    public void testChatLogInitiallyEmpty()
    {
        _chat = new ChatManager(new ChatManagerCallback()
        {
            @Override
            public void chatManagerStateChanged()
            {
                // Do nothing.
            }
        });

        int mc = _chat.getChatMessageCount();
        assertEquals("Chat manager starts with 0 messages.", 0, mc);
    }

    public void testSmartReplyFailsEventuallyWhenRemoteNotAvailable()
    {
        final ChatMessage m = new ChatMessage(ChatMessageSource.USER, "Test-Message");

        _chat = new ChatManager(new ChatManagerCallback()
        {
            @Override
            public void chatManagerStateChanged()
            {
                int mc = _chat.getChatMessageCount();
                ChatMessage message = _chat.getChatMessage(0);

                assertEquals("Chat message is enqueued.", 1, mc);
                assertSame("Chat message is enqueued.", m, message);
            }
        });

        assertFalse("Message is not acked.", m.isAcked());
        assertFalse("Message is not failed.", m.isFailed());

        String host = "try.yaler.net";
        _chat.startup(_id, host);

        _chat.sendChatMessage(m);

        try
        {
            Thread.sleep(20000);
        }
        catch (InterruptedException ignored)
        {
        }

        assertFalse("Message is not acked.", m.isAcked());
        assertTrue("Message is failed.", m.isFailed());
    }
}
