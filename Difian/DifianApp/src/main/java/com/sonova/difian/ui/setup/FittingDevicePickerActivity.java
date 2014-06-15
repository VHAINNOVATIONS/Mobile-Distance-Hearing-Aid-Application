// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.setup;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import com.sonova.difian.R;
import com.sonova.difian.utilities.Contract;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class FittingDevicePickerActivity extends Activity
{
    private static final String TAG = "FittingDevicePickerActivity";
    private static final int REQUEST_ENABLE_BT = 0;
    private static final Method createBond;
    private static final Method setPin;
    private static final Method convertPinToBytes;
    private static final String ACTION_PAIRING_REQUEST;
    private BluetoothAdapter _adapter;
    private BluetoothDevice _fittingDevice;

    static
    {
        try
        {
            createBond = BluetoothDevice.class.getMethod("createBond");
            setPin = BluetoothDevice.class.getMethod("setPin", byte[].class);
            convertPinToBytes = BluetoothDevice.class.getMethod("convertPinToBytes", String.class);
            ACTION_PAIRING_REQUEST = (String)BluetoothDevice.class.getField("ACTION_PAIRING_REQUEST").get(null);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
        Contract.check(createBond != null);
        Contract.check(setPin != null);
        Contract.check(convertPinToBytes != null);
        Contract.check(ACTION_PAIRING_REQUEST != null);
    }

    private final BroadcastReceiver _receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //noinspection UnnecessaryToStringCall
            Log.v(TAG, intent.toString());
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction()))
            {
                if (!_adapter.isEnabled())
                {
                    FittingDevicePickerActivity.this.setResult(RESULT_CANCELED);
                    finish();
                }
                else
                {
                    tryStartDiscovery();
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction()))
            {
                Log.i(TAG, "Bluetooth discovery started");
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction()))
            {
                tryStartDiscovery();
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction()))
            {
                if (_fittingDevice == null)
                {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if ("iCube".equals(device.getName()))
                    {
                        Log.i(TAG, "Relay found");
                        _fittingDevice = device;
                        _adapter.cancelDiscovery();
                        if (_fittingDevice.getBondState() == BluetoothDevice.BOND_NONE)
                        {
                            Log.i(TAG, "Initiate bonding");
                            boolean success = createBond();
                            if (!success)
                            {
                                Log.w(TAG, "Bonding failed");
                                _fittingDevice = null;
                                tryStartDiscovery();
                            }
                        }
                        else if (_fittingDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                        {
                            complete();
                        }
                    }
                }
            }
            else if (ACTION_PAIRING_REQUEST.equals(intent.getAction()))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.equals(_fittingDevice))
                {
                    if (_fittingDevice.getBondState() == BluetoothDevice.BOND_BONDING)
                    {
                        Log.i(TAG, "Initiate pairing dialog bypass");
                        boolean success = setPin(convertPinToBytes("0000"));
                        if (!success)
                        {
                            Log.w(TAG, "Pairing dialog bypass failed");
                            _fittingDevice = null;
                            tryStartDiscovery();
                        }
                    }
                }
            }
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction()))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.equals(_fittingDevice))
                {
                    if (_fittingDevice.getBondState() == BluetoothDevice.BOND_NONE)
                    {
                        _fittingDevice = null;
                        tryStartDiscovery();
                    }
                    else if (_fittingDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                    {
                        complete();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_sonova_difian_ui_setup_fittingdevicepickeractivity);
        _adapter = BluetoothAdapter.getDefaultAdapter();
        Contract.check(_adapter != null);
        if (!_adapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Log.v(TAG, "onRestart");
        if (!_adapter.isEnabled())
        {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(_receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(_receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(_receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        registerReceiver(_receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(_receiver, new IntentFilter(ACTION_PAIRING_REQUEST));
        registerReceiver(_receiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        tryStartDiscovery();
        if ((_fittingDevice != null) && (_fittingDevice.getBondState() == BluetoothDevice.BOND_BONDED))
        {
            complete();
        }
    }

    @Override
    protected void onPause()
    {
        unregisterReceiver(_receiver);
        if (_adapter.isDiscovering())
        {
            _adapter.cancelDiscovery();
        }
        super.onPause();
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_ENABLE_BT)
        {
            Log.v(TAG, "onActivityResult: " + Integer.toString(resultCode));
            if (resultCode == RESULT_CANCELED)
            {
                setResult(RESULT_CANCELED);
                finish();
            }
            else if (resultCode == RESULT_OK)
            {
                tryStartDiscovery();
            }
        }
    }

    private void tryStartDiscovery()
    {
        if ((_fittingDevice == null) && !isFinishing() && _adapter.isEnabled())
        {
            if (!_adapter.isDiscovering())
            {
                boolean success = _adapter.startDiscovery();
                if (!success)
                {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    private void complete()
    {
        Contract.check(_fittingDevice != null);
        Contract.check(_fittingDevice.getBondState() == BluetoothDevice.BOND_BONDED);
        Intent intent = new Intent();
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, _fittingDevice);
        setResult(RESULT_OK, intent);
        finish();
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private boolean createBond()
    {
        Contract.check(createBond != null);
        Contract.check(_fittingDevice != null);
        Contract.check(_fittingDevice.getBondState() == BluetoothDevice.BOND_NONE);
        boolean result;
        try
        {
            result = (Boolean)createBond.invoke(_fittingDevice);
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
        return result;
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private boolean setPin(byte[] pin)
    {
        Contract.check(setPin != null);
        Contract.check(_fittingDevice != null);
        Contract.check(_fittingDevice.getBondState() == BluetoothDevice.BOND_BONDING);
        Contract.check(pin != null);
        Contract.check((pin != null) && (pin.length > 0));
        boolean result;
        try
        {
            //noinspection PrimitiveArrayArgumentToVariableArgMethod
            result = (Boolean)setPin.invoke(_fittingDevice, pin);
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static byte[] convertPinToBytes(String pin)
    {
        Contract.check(convertPinToBytes != null);
        byte[] result;
        try
        {
            result = (byte[])convertPinToBytes.invoke(null, pin);
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
        return result;
    }
}
