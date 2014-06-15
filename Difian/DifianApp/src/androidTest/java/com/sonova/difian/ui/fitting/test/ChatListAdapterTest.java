// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting.test;

import android.test.AndroidTestCase;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.sonova.difian.R;
import com.sonova.difian.communication.chat.ChatMessage;
import com.sonova.difian.communication.chat.ChatMessageSource;
import com.sonova.difian.ui.fitting.ChatListAdapter;

import java.util.ArrayList;

public final class ChatListAdapterTest extends AndroidTestCase
{
    private ChatListAdapter _chat;
    private ArrayList<ChatMessage> _messages;

    @SuppressWarnings("ProhibitedExceptionDeclared")
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        _messages = new ArrayList<ChatMessage>();
        _chat = new ChatListAdapter(getContext(), _messages);
    }

    public void testPendingUserMessage()
    {
        ChatMessage m = new ChatMessage(ChatMessageSource.USER, "Test-Message");
        _messages.add(m);
        assertFalse("Message is pending (not acked, not failed, not aborted)", m.isAcked() || m.isFailed() || m.isAborted());
        _chat.notifyDataSetChanged();
        assertEquals("Chat now has 1 item", 1, _chat.getCount());
        assertEquals("Message was added", m, _chat.getItem(0));
        View view = _chat.getView(0, null, null);
        TextView text = (TextView)view.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_messagetext);
        ImageView failed = (ImageView)view.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_failed);
        ProgressBar pending = (ProgressBar)view.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_pending);
        assertEquals("Displayed message has correct text", m.getText(), text.getText());
        assertEquals("Pending indicator is visible", View.VISIBLE, pending.getVisibility());
        assertEquals("Failed indicator is invisible", View.GONE, failed.getVisibility());
    }

    public void testFailedUserMessage()
    {
        ChatMessage m = new ChatMessage(ChatMessageSource.USER, "Test-Message");
        m.retry();
        m.retry();
        m.retry();
        m.retry();
        _messages.add(m);
        assertTrue("Message is failed", m.isFailed());
        _chat.notifyDataSetChanged();
        assertEquals("Chat now has 1 item", 1, _chat.getCount());
        assertEquals("Message was added", m, _chat.getItem(0));
        View view = _chat.getView(0, null, null);
        TextView text = (TextView)view.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_messagetext);
        ImageView failed = (ImageView)view.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_failed);
        ProgressBar pending = (ProgressBar)view.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_pending);
        assertEquals("Displayed message has correct text", m.getText(), text.getText());
        assertEquals("Pending indicator is invisible", View.GONE, pending.getVisibility());
        assertEquals("Failed indicator is visible", View.VISIBLE, failed.getVisibility());
    }

    public void testAbortedUserMessage()
    {
        ChatMessage m = new ChatMessage(ChatMessageSource.USER, "Test-Message");
        m.retry();
        m.retry();
        m.retry();
        m.retry();
        m.abort();
        _messages.add(m);
        assertTrue("Message is aborted", m.isAborted());
        _chat.notifyDataSetChanged();
        assertEquals("Chat now has 1 item", 1, _chat.getCount());
        assertEquals("Message was added", m, _chat.getItem(0));
        View view = _chat.getView(0, null, null);
        assertTrue("View is invisible", (view.getHeight() == 0) && (view.getWidth() == 0));
    }
}
