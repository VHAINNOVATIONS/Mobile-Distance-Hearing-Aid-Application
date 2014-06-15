// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

import com.sonova.difian.communication.chat.ChatMessage;
import com.sonova.difian.utilities.Contract;

import java.util.ArrayList;
import java.util.List;

public final class SmartReplyMessage extends Message
{
    private final String _reText;

    private final SmartMessage _reMessage;
    private final List<SmartMessageOption> _selection = new ArrayList<SmartMessageOption>();

    private final ChatMessage _message;

    public SmartReplyMessage(ChatMessage message)
    {
        Contract.check(message != null);
        _message = message;

        if (message.getMessage() == null)
        {
            _reText = message.getText();
            _reMessage = null;
        }
        else
        {
            _reText = null;
            _reMessage = message.getMessage();
            for (SmartMessageOption o : message.getSelection())
            {
                addSelection(o);
            }
        }
    }

    @Override
    public MessageType getType()
    {
        return MessageType.SMART_REPLY;
    }

    @SuppressWarnings("WeakerAccess")
    public void addSelection(SmartMessageOption selection)
    {
        Contract.check(_reMessage != null);

        _selection.add(selection);
    }

    public String getReText()
    {
        return _reText;
    }

    public SmartMessage getReMessage()
    {
        return _reMessage;
    }

    public int getSelectionCount()
    {
        return _selection.size();
    }

    public SmartMessageOption getSelection(int id)
    {
        return _selection.get(id);
    }

    public void ack()
    {
        _message.ack();
    }

    public void retry()
    {
        _message.retry();
    }

    public boolean isFailed()
    {
        return _message.isFailed();
    }
}
