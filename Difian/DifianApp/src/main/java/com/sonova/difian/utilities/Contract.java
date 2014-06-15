// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.utilities;

public final class Contract
{
    private Contract()
    {
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public static void check(boolean condition)
    {
        if (!condition)
        {
            throw new AssertionError();
        }
    }
}
