// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public final class StreamHelpers
{
    private static final String TAG = StreamHelpers.class.getName();

    private StreamHelpers()
    {
    }

    public static String readLine(InputStream stream) throws IOException
    {
        if (stream == null)
        {
            throw new IllegalArgumentException("stream == null in readLine");
        }
        StringBuilder line = new StringBuilder();
        int b;
        do
        {
            b = stream.read();
            if (b != -1)
            {
                if (line.length() < 4096)
                {
                    line.append((char)b);
                } else {
                    throw new IOException("line too long", null);
                }
            }
            else
            {
                throw new IOException("unexpected end of stream", null);
            }
        } while (b != '\n');
        String result;
        if ((line.length() >= 2) && (line.charAt(line.length() - 2) == '\r')) {
            result = line.substring(0, line.length() - 2).trim();
        } else {
            result = line.substring(0, line.length() - 1).trim();
        }
        return result;
    }

    public static boolean consumeHttpHeaders (InputStream stream) throws IOException
    {
        boolean continueExpected = false;
        String line = readLine(stream);
        while (!line.equals(""))
        {
            Log.v(TAG, line);
            if (line.equals("Expect: 100-continue"))
            {
                continueExpected = true;
            }
            line = readLine(stream);
        }
        return continueExpected;
    }
}