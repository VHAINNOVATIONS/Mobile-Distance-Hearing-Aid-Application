// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.chat;

import android.graphics.Bitmap;
import com.sonova.difian.communication.messaging.SmartMessage;
import com.sonova.difian.communication.messaging.SmartMessageOption;
import com.sonova.difian.utilities.Contract;
import com.sonova.difian.utilities.ImageHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatMessage
{
    public static final String TEXT_CONNECTED = ChatMessage.class.getName() + ".CONNECTED";
    public static final String TEXT_DISCONNECTED = ChatMessage.class.getName() + ".DISCONNECTED";
    public static final String TEXT_HI_MUTED_LEFT = ChatMessage.class.getName() + ".HI_MUTED_LEFT";
    public static final String TEXT_HI_MUTED_RIGHT = ChatMessage.class.getName() + ".HI_MUTED_RIGHT";
    public static final String TEXT_HI_MUTED_BOTH = ChatMessage.class.getName() + ".HI_MUTED_BOTH";
    public static final String TEXT_HI_UNMUTED_LEFT = ChatMessage.class.getName() + ".HI_UNMUTED_LEFT";
    public static final String TEXT_HI_UNMUTED_RIGHT = ChatMessage.class.getName() + ".HI_UNMUTED_RIGHT";
    public static final String TEXT_HI_UNMUTED_BOTH = ChatMessage.class.getName() + ".HI_UNMUTED_BOTH";
    private final ChatMessageSource _source;
    private final String _text;
    private final Bitmap _image;
    private final SmartMessage _message;
    private final List<SmartMessageOption> _selection;
    private volatile boolean _acked;
    private volatile int _retries;
    private volatile boolean _aborted;

    public ChatMessage(ChatMessageSource source, String text)
    {
        Contract.check((source == ChatMessageSource.SYSTEM) || (source == ChatMessageSource.USER));

        _source = source;
        _text = text;
        _image = null;
        _message = null;
        _selection = null;
    }

    public ChatMessage(ChatMessageSource source, SmartMessage message)
    {
        Contract.check(source == ChatMessageSource.AUDIOLOGIST);

        _source = source;
        _text = message.getText();
        _image = ImageHelpers.imageBase64ToBitmap(message.getImage());
        _message = message;
        _selection = null;
    }

    public ChatMessage(ChatMessageSource source, SmartMessage message, List<SmartMessageOption> selection)
    {
        Contract.check(source == ChatMessageSource.USER);

        _source = source;
        _text = null;
        _image = null;
        _message = message;
        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        _selection = selection;
    }

    public ChatMessage(ChatMessage message)
    {
        if (message == null)
        {
            throw new IllegalArgumentException("message is null.");
        }

        _source = message.getSource();
        _text = message.getText();
        _image = message.getImage();
        _message = message.getMessage();
        if (message.getSelection() != null)
        {
            _selection = new ArrayList<SmartMessageOption>(message.getSelection());
        }
        else
        {
            _selection = null;
        }
    }

    public List<SmartMessageOption> getSelection()
    {
        List<SmartMessageOption> result = null;
        if (_selection != null)
        {
            result = Collections.unmodifiableList(_selection);
        }
        return result;
    }

    public SmartMessage getMessage()
    {
        return _message;
    }

    public Bitmap getImage()
    {
        return _image;
    }

    public String getText()
    {
        return _text;
    }

    public ChatMessageSource getSource()
    {
        return _source;
    }

    public void ack()
    {
        if ((getSource() == ChatMessageSource.AUDIOLOGIST) || (getSource() == ChatMessageSource.SYSTEM))
        {
            throw new IllegalStateException("Message cannot be acked.");
        }

        _acked = true;
    }

    /**
     * Volatile value. Can change only form false to true.
     */
    public boolean isAcked()
    {
        if ((getSource() == ChatMessageSource.AUDIOLOGIST) || (getSource() == ChatMessageSource.SYSTEM))
        {
            throw new IllegalStateException("Message cannot be acked.");
        }

        return _acked;
    }

    public void retry()
    {
        if ((getSource() == ChatMessageSource.AUDIOLOGIST) || (getSource() == ChatMessageSource.SYSTEM))
        {
            throw new IllegalStateException("Message cannot be retried.");
        }
        if (isFailed())
        {
            throw new IllegalStateException("Failed messages cannot be retried.");
        }

        //noinspection NonAtomicOperationOnVolatileField
        _retries++;
    }

    /**
     * Volatile value. Can change only form false to true.
     */
    public boolean isFailed()
    {
        if ((getSource() == ChatMessageSource.AUDIOLOGIST) || (getSource() == ChatMessageSource.SYSTEM))
        {
            throw new IllegalStateException("Message cannot fail.");
        }

        return _retries > 3;
    }

    public void abort()
    {
        if ((getSource() == ChatMessageSource.AUDIOLOGIST) || (getSource() == ChatMessageSource.SYSTEM))
        {
            throw new IllegalStateException("Message cannot be aborted.");
        }
        if (!isFailed())
        {
            throw new IllegalStateException("Only failed messages can be aborted.");
        }
        if (isAborted())
        {
            throw new IllegalStateException("Messages can only be aborted once.");
        }

        _aborted = true;
    }

    /**
     * Volatile value. Can change only form false to true.
     */
    public boolean isAborted()
    {
        if ((getSource() == ChatMessageSource.AUDIOLOGIST) || (getSource() == ChatMessageSource.SYSTEM))
        {
            throw new IllegalStateException("Message cannot be aborted.");
        }

        return _aborted;
    }
}
