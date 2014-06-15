// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.chat;

import android.graphics.Bitmap;
import com.sonova.difian.utilities.ImageHelpers;

public final class AudiologistInfo
{
    private final String _audiologistName;
    private final Bitmap _audiologistPicture;

    public static final AudiologistInfo EMPTY = new AudiologistInfo();

    private AudiologistInfo()
    {
        _audiologistName = null;
        _audiologistPicture = null;
    }

    /**
     * @param picture Base 64 image from Smart message.
     */
    public AudiologistInfo(String name, String picture)
    {
        _audiologistName = name;
        _audiologistPicture = ImageHelpers.imageBase64ToBitmap(picture);
    }

    public String getName()
    {
        return _audiologistName;
    }

    public Bitmap getPicture()
    {
        return _audiologistPicture;
    }
}
