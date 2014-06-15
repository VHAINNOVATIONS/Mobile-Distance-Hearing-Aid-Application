// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.utilities;

public final class StringHelpers
{
    private StringHelpers()
    {
    }

    public static byte[] convertToBytes(String source)
    {
        char[] chars = source.toCharArray();
        byte[] result = new byte[chars.length];
        for (int i = 0; i < chars.length; i++)
        {
            result[i] = (byte)chars[i];
        }
        return result;
    }
}
