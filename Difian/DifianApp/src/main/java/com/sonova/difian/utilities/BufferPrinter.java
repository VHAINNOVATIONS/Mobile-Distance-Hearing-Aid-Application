// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.utilities;

import android.util.Log;

public final class BufferPrinter
{
    private BufferPrinter()
    {
    }

    public static void printBuffer(String tag, byte[] buffer, int count)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++)
        {
            sb.append(String.format("%02X ", buffer[i]));
        }
        //noinspection UnnecessaryToStringCall
        Log.v(tag, sb.toString());
    }
}
