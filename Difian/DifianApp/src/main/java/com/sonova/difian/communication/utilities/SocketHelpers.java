// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.utilities;

import android.util.Log;
import com.sonova.difian.utilities.Contract;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public final class SocketHelpers
{
    private SocketHelpers()
    {
    }

    public static Socket createSocket(SocketFactory factory, int soTimeout)
    {
        Socket result = null;
        try
        {
            result = factory.createSocket();
        }
        catch (IOException e)
        {
            Log.e(SocketHelpers.class.getSimpleName(), "Exception while creating socket.", e);
            throw new RuntimeException(e);
        }

        try
        {
            result.setSoTimeout(soTimeout);
        }
        catch (SocketException e)
        {
            Log.e(SocketHelpers.class.getSimpleName(), "Exception while setting socket timeout.", e);
            throw new RuntimeException(e);
        }

        try
        {
            result.setTcpNoDelay(true);
        }
        catch (SocketException e)
        {
            Log.e(SocketHelpers.class.getSimpleName(), "Exception while setting TcpNoDelay option.", e);
            throw new RuntimeException(e);
        }

        return result;
    }
}
