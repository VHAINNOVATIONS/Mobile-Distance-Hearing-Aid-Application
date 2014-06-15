// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.fittingconnection;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import com.sonova.difian.utilities.BufferPrinter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class RelayIdHelper
{
    private static final String TAG = RelayIdHelper.class.getName();

    private RelayIdHelper()
    {
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public static boolean tryReadPrefix(BluetoothSocket socket) throws IOException
    {
        boolean result = true;
        InputStream in = socket.getInputStream();
        if (result)
        {
            int x = in.read();
            if (x != 0x01)
            {
                result = false;
            }
        }
        if (result)
        {
            int x = in.read();
            if (x != 0x20)
            {
                result = false;
            }
        }
        if (result)
        {
            int x = in.read();
            if (x != 0x01)
            {
                result = false;
            }
        }
        if (result)
        {
            int x = in.read();
            if (x != 0x20)
            {
                result = false;
            }
        }

        if (!result)
        {
            Log.e(TAG, "Target device is not an iCube. Prefix reading failed.");
        }
        return result;
    }

    public static String getId(BluetoothSocket socket) throws IOException
    {
        String result = null;
        boolean success = true;
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        // Send ID request.
        if (success)
        {
            byte[] buffer = {(byte)0xA5, 0x5A, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x01};
            out.write(buffer);
        }
        // Receive ID response.
        while ((result == null) && success)
        {
            // Read magic bytes.
            if (success)
            {
                int x = in.read();
                if (x != 0xA5)
                {
                    success = false;
                }
            }
            if (success)
            {
                int x = in.read();
                if (x != 0x5A)
                {
                    success = false;
                }
            }
            // Read length.
            int length = 0;
            for (int i = 0; (i < 4) && success; i++)
            {
                int x = in.read();
                if (x == -1)
                {
                    success = false;
                }
                else
                {
                    length |= x << (8 * i);
                }
            }
            // Read flags.
            byte flags = 0;
            if (success)
            {
                int x = in.read();
                if (x == -1)
                {
                    success = false;
                }
                else
                {
                    flags = (byte)x;
                }
            }
            // Read payload.
            byte[] payload = new byte[length];
            if (length > 0)
            {
                int i = 0;
                while ((i < length) && success)
                {
                    int count = in.read(payload, i, length - i);
                    if (count == -1)
                    {
                        success = false;
                    }
                    else
                    {
                        i += count;
                    }
                }
            }
            // Process payload.
            if (success && (length >= 3) && (flags == 0x01) && (payload[1] == 0x01))
            {
                if ((payload[0] > 0x02) || (payload[0] < 0))
                {
                    Log.e(TAG, "Version mismatch");

                    success = false;
                }

                if (success && (length == 244) && (payload[2] == 2))
                {
                    if (payload[3] != 0x00)
                    {
                        Log.e(TAG, "Accessory ID request failed");

                        success = false;
                    }
                    else
                    {
                        StringBuilder id = new StringBuilder();
                        for (int i = 104; (i < 144) && (payload[i] != '\0'); i += 2)
                        {
                            id.append((char)payload[i]);
                        }
                        if (id.length() > 0)
                        {
                            result = id.toString();
                        }
                    }
                }
                else
                {
                    Log.i(TAG, "Skipping unsupported packet... (indication?)");
                    BufferPrinter.printBuffer(TAG, payload, length);
                }
            }
            else
            {
                success = false;
                Log.e(TAG, "Received malformed command.");
            }

            if (!success && (length > 0))
            {
                BufferPrinter.printBuffer(TAG, payload, length);
            }
        }
        return result;
    }
}
