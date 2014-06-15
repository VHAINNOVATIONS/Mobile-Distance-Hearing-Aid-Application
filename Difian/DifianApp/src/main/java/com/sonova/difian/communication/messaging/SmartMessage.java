// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SmartMessage extends Message
{
    private String _text;
    private String _image;
    private SmartMessageOptionsType _optionsType;
    private final List<SmartMessageOption> _options = new ArrayList<SmartMessageOption>();

    SmartMessage(XmlPullParser parser) throws MessageReaderException, XmlPullParserException, IOException
    {
        while (parser.nextTag() == XmlPullParser.START_TAG)
        {
            if ("text".equals(parser.getName()))
            {
                setText(parser.nextText());
                parser.require(XmlPullParser.END_TAG, null, "text");
            }
            else if (XmlConstants.IMAGE_BASE64.equals(parser.getName()))
            {
                setImage(parser.nextText());
                parser.require(XmlPullParser.END_TAG, null, XmlConstants.IMAGE_BASE64);
            }
            else if (XmlConstants.OPTIONS.equals(parser.getName()))
            {
                String optionsType = parser.getAttributeValue(null, "type");
                if ("button".equals(optionsType))
                {
                    setOptionsType(SmartMessageOptionsType.BUTTON);
                }
                else if ("radio".equals(optionsType))
                {
                    setOptionsType(SmartMessageOptionsType.RADIO);
                }
                else if ("checkbox".equals(optionsType))
                {
                    setOptionsType(SmartMessageOptionsType.CHECKBOX);
                }
                else if ("text".equals(optionsType))
                {
                    setOptionsType(SmartMessageOptionsType.TEXT);
                }
                else
                {
                    throw new MessageReaderException("Invalid type in \"smart\" <message> \"options\".");
                }

                while (parser.nextTag() == XmlPullParser.START_TAG)
                {
                    if (XmlConstants.OPTION.equals(parser.getName()))
                    {
                        String optionId = parser.getAttributeValue(null, "id");
                        String optionText = null;
                        String optionImage = null;
                        if ((optionId == null) || !optionId.matches("^[a-z0-9]+$"))
                        {
                            throw new MessageReaderException(String.format("Invalid option id: %s.", optionId));
                        }
                        while (parser.nextTag() == XmlPullParser.START_TAG)
                        {
                            if ("text".equals(parser.getName()))
                            {
                                optionText = parser.nextText();
                                parser.require(XmlPullParser.END_TAG, null, "text");
                            }
                            else if (XmlConstants.IMAGE_BASE64.equals(parser.getName()))
                            {
                                optionImage = parser.nextText();
                                parser.require(XmlPullParser.END_TAG, null, XmlConstants.IMAGE_BASE64);
                            }
                            else
                            {
                                throw new MessageReaderException("Invalid tag in \"smart\" <message> \"option\".");
                            }
                        }
                        if (optionText == null)
                        {
                            throw new MessageReaderException("No option text in \"smart\" <message>.");
                        }
                        addOption(new SmartMessageOption(optionId, optionText, optionImage));
                        parser.require(XmlPullParser.END_TAG, null, XmlConstants.OPTION);
                    }
                    else
                    {
                        throw new MessageReaderException("Invalid tag in \"smart\" <message> \"options\".");
                    }
                }
                parser.require(XmlPullParser.END_TAG, null, XmlConstants.OPTIONS);
            }
            else
            {
                throw new MessageReaderException("Invalid tag in \"smart\" <message>.");
            }
        }
        if (getText() == null)
        {
            throw new MessageReaderException("No text in \"smart\" <message>.");
        }
    }

    @Override
    public MessageType getType()
    {
        return MessageType.SMART;
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

    public SmartMessageOptionsType getOptionsType()
    {
        return _optionsType;
    }

    private void setOptionsType(SmartMessageOptionsType optionsType)
    {
        _optionsType = optionsType;
    }

    public int getOptionsCount()
    {
        return _options.size();
    }

    public SmartMessageOption getOption(int id)
    {
        return _options.get(id);
    }

    private void addOption(SmartMessageOption option)
    {
        _options.add(option);
    }
}
