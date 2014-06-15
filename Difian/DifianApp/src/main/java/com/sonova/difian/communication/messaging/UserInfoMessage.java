// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public final class UserInfoMessage extends Message
{
    private String _name;
    private String _lang;
    private String _text;
    private String _image;

    UserInfoMessage(XmlPullParser parser) throws MessageReaderException, XmlPullParserException, IOException
    {
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, XmlConstants.USER_NAME);
        setName(parser.nextText());
        parser.require(XmlPullParser.END_TAG, null, XmlConstants.USER_NAME);
        while (parser.nextTag() == XmlPullParser.START_TAG)
        {
            if (XmlConstants.USER_LANG.equals(parser.getName()))
            {
                setLang(parser.nextText());
                parser.require(XmlPullParser.END_TAG, null, XmlConstants.USER_LANG);
            }
            else if (XmlConstants.USER_TEXT.equals(parser.getName()))
            {
                setText(parser.nextText());
                parser.require(XmlPullParser.END_TAG, null, XmlConstants.USER_TEXT);
            }
            else if (XmlConstants.IMAGE_BASE64.equals(parser.getName()))
            {
                setImage(parser.nextText());
                parser.require(XmlPullParser.END_TAG, null, XmlConstants.IMAGE_BASE64);
            }
            else
            {
                throw new MessageReaderException("Invalid tag in \"user_info\" <message>.");
            }
        }
    }

    @Override
    public MessageType getType()
    {
        return MessageType.USER_INFO;
    }

    public String getName()
    {
        return _name;
    }

    private void setName(String name)
    {
        _name = name;
    }

    @SuppressWarnings("WeakerAccess")
    public String getLang()
    {
        return _lang;
    }

    private void setLang(String lang)
    {
        _lang = lang;
    }

    @SuppressWarnings("WeakerAccess")
    public String getText()
    {
        return _text;
    }

    private void setText(String text)
    {
        _text = text;
    }

    public String getImage()
    {
        return _image;
    }

    private void setImage(String image)
    {
        _image = image;
    }
}
