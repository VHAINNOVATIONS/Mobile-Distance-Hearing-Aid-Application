// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

public final class ImageHelpers
{
    private ImageHelpers()
    {
    }

    public static Bitmap imageBase64ToBitmap(String image)
    {
        Bitmap result = null;
        if (image != null)
        {
            byte[] decodedImage = null;
            try
            {
                decodedImage = Base64.decode(image, Base64.DEFAULT);
            }
            catch (IllegalArgumentException e)
            {
                Log.w(ImageHelpers.class.getSimpleName(), "Invalid padding in passed image.", e);
            }
            if (decodedImage != null)
            {
                result = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);
                if (result == null)
                {
                    Log.w(ImageHelpers.class.getSimpleName(), "Image could not be decoded.");
                }
            }
        }
        return result;
    }
}
