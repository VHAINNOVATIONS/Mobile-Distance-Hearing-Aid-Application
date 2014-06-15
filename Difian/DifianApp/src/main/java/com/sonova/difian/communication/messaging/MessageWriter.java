// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

import android.util.Log;
import android.util.Xml;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public final class MessageWriter
{
    private static final String TAG = MessageWriter.class.getName();
    private static final MessageWriter _instance = new MessageWriter();

    private MessageWriter()
    {
    }

    public static MessageWriter getInstance()
    {
        return _instance;
    }

    @SuppressWarnings("UnnecessaryCodeBlock")
    public void transmit(OutputStream stream, String host, String id, Message message) throws IOException
    {
        if (stream == null)
        {
            throw new IllegalArgumentException("stream == null in transmit");
        }
        if (host == null)
        {
            throw new IllegalArgumentException("host == null");
        }
        if (id == null)
        {
            throw new IllegalArgumentException("id == null");
        }
        if (message == null)
        {
            throw new IllegalArgumentException("message == null");
        }
        if ((message.getType() != MessageType.USER_INFO_REQUEST) && (message.getType() != MessageType.SMART_REPLY))
        {
            throw new IllegalArgumentException(String.format("Unsupported message type while writing: %s", message.getType()));
        }

        StringWriter messageString = new StringWriter();

        XmlSerializer x = Xml.newSerializer();
        x.setOutput(messageString);

        // Prepare message.
        x.startDocument("ASCII", true);
        {
            x.startTag("", XmlConstants.MESSAGE);
            {
                x.attribute("", "v", "3");
                //noinspection UnnecessaryToStringCall
                x.attribute("", "guid", UUID.randomUUID().toString());
                x.attribute("", "date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(new Date()));
                if (message.getType() == MessageType.USER_INFO_REQUEST)
                {
                    x.attribute("", "type", "user_info_request");
                }
                else if (message.getType() == MessageType.SMART_REPLY)
                {
                    x.attribute("", "type", "smart_reply");

                    SmartReplyMessage m = (SmartReplyMessage)message;

                    if (m.getReMessage() != null)
                    {
                        x.attribute("", XmlConstants.RE_MESSAGE_GUID, m.getReMessage().getGuid());
                    }
                    else
                    {
                        x.attribute("", XmlConstants.RE_MESSAGE_GUID, "00000000-0000-0000-0000-000000000000");
                    }

                    String sel;
                    String text;

                    if (m.getSelectionCount() != 0)
                    {
                        StringBuilder selBuilder = new StringBuilder();
                        StringBuilder textBuilder = new StringBuilder();
                        for (int i = 0; i < m.getSelectionCount(); i++)
                        {
                            if (i != 0)
                            {
                                selBuilder.append(';');
                                textBuilder.append(';');
                            }
                            selBuilder.append(m.getSelection(i).getId());
                            textBuilder.append(m.getSelection(i).getText());
                        }
                        sel = selBuilder.toString();
                        text = textBuilder.toString();
                    }
                    else
                    {
                        text = m.getReText();
                        sel = "";
                    }

                    x.startTag("", "re_text");
                    {
                        x.text(text);
                    }
                    x.endTag("", "re_text");
                    x.startTag("", XmlConstants.SELECTION);
                    {
                        x.attribute("", XmlConstants.OPTION_IDS, sel);
                    }
                    x.endTag("", XmlConstants.SELECTION);
                }
            }
            x.endTag("", XmlConstants.MESSAGE);
        }
        x.endDocument();

        String m = messageString.toString();
        Log.v(MessageWriter.class.getSimpleName(), m);

        // Send HTTP header.
        stream.write(String.format("PUT /%s_-1 HTTP/1.1\r\n", id).getBytes());
        stream.write("Content-Type: text/xml; charset=utf-8\r\n".getBytes());
        stream.write(String.format("Host: %s\r\n", host).getBytes());
        stream.write(String.format("Content-Length: %d\r\n", m.length()).getBytes());
        stream.write("\r\n".getBytes());
        stream.write(m.getBytes());
    }
}
