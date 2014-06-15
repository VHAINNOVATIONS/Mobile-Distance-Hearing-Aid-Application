// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.fittingconnection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

final class BluetoothSocketProvider {
    private static final String TAG = BluetoothSocketProvider.class.getName();

    private BluetoothSocketProvider() {
    }

    public static BluetoothSocket getSerialPort(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            throw new IllegalArgumentException("Invalid address specified");
        }

        BluetoothSocket result = null;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            BluetoothDevice device = adapter.getRemoteDevice(address);
            if (device != null) {
                try {
                    result = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                } catch (IOException e) {
                    Log.i(TAG, "Could not createRfcommSocketToServiceRecord", e);
                }
            }
        }
        return result;
    }
}
