// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

public final class SmartMessageOption
{
    private final String _id;
    private final String _text;
    private final String _image;

    SmartMessageOption(String id, String text, String image)
    {
        _id = id;
        _text = text;
        _image = image;
    }

    public String getId()
    {
        return _id;
    }

    public String getText()
    {
        return _text;
    }

    public String getImage()
    {
        return _image;
    }
}
