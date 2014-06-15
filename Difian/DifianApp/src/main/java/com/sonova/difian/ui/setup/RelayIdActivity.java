// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.setup;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.sonova.difian.R;
import com.sonova.difian.communication.fittingconnection.RelayIdHelper;
import com.sonova.difian.utilities.BackgroundOperations.BackgroundOperation;
import com.sonova.difian.utilities.BackgroundOperations.BackgroundTask;
import com.sonova.difian.utilities.Contract;
import com.sonova.difian.utilities.ContractExecutors;

import java.io.IOException;
import java.util.UUID;

public final class RelayIdActivity extends Activity
{
    private static final String TAG = RelayIdActivity.class.getSimpleName();

    private static final class ModelFragment extends Fragment
    {
        @SuppressWarnings("InnerClassFieldHidesOuterClassField")
        private static final String TAG = ModelFragment.class.getSimpleName();
        private BluetoothDevice _device;
        private BluetoothSocket _socket;
        private final BackgroundOperation _operation = new BackgroundOperation(ContractExecutors.newSingleThreadExecutor());

        @Override
        public void onActivityCreated(Bundle savedInstanceState)
        {
            super.onActivityCreated(savedInstanceState);
            Log.v(TAG, "onActivityCreated");
            setRetainInstance(true);
            if (_device == null)
            {
                _device = getActivity().getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Contract.check(_device != null);
                if (_device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    getActivity().finish();
                }
                else
                {
                    try
                    {
                        _socket = _device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                        Log.i(TAG, "socket created");
                        _operation.start(new BackgroundTask<String>()
                        {
                            private Exception _error;

                            @Override
                            protected String doInBackground() throws Exception
                            {
                                Log.i(TAG, "Background operation started");
                                String result = null;
                                BluetoothSocket s = _socket;
                                if (s != null)
                                {
                                    try
                                    {
                                        Log.i(TAG, "Connecting socket");
                                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                                        s.connect();
                                        boolean success = RelayIdHelper.tryReadPrefix(s);
                                        if (success)
                                        {
                                            result = RelayIdHelper.getId(s);
                                        }
                                    }
                                    catch (IOException e)
                                    {
                                        _error = e;
                                    }
                                    finally
                                    {
                                        try
                                        {
                                            Log.i(TAG, "Closing stream");
                                            s.close();
                                        }
                                        catch (IOException e)
                                        {
                                            Log.e(TAG, "Error during stream close", e);
                                        }
                                    }
                                }
                                return result;
                            }

                            @SuppressWarnings("RefusedBequest")
                            @Override
                            protected void onCompleted(String result)
                            {
                                Log.i(TAG, "Background operation completed");
                                if ((_error == null) && (result != null))
                                {
                                    Intent intent = new Intent();
                                    intent.putExtra(BluetoothDevice.EXTRA_DEVICE, _device);
                                    intent.putExtra(BluetoothDevice.EXTRA_NAME, result);
                                    getActivity().setResult(RESULT_OK, intent);
                                }
                                else if (_error != null)
                                {
                                    Log.e(TAG, "Error during ID retrieval", _error);
                                }
                                else
                                {
                                    Log.e(TAG, "Protocol incorrect");
                                }
                                getActivity().finish();
                            }
                        });
                    }
                    catch (IOException e)
                    {
                        getActivity().finish();
                    }
                }
            }
        }

        @Override
        public void onDetach()
        {
            Log.v(TAG, "onDetach");
            if (getActivity().isFinishing())
            {
                if (_socket != null)
                {
                    try
                    {
                        _socket.close();
                    }
                    catch (IOException e)
                    {
                        // Do nothing
                    }
                    Log.i(TAG, "socket closed");
                }
                _device = null;
            }
            super.onDetach();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.com_sonova_difian_ui_setup_fittingdeviceidactivity);
        if (getFragmentManager().findFragmentByTag(ModelFragment.TAG) == null)
        {
            getFragmentManager().beginTransaction().add(new ModelFragment(), ModelFragment.TAG).commit();
        }
    }
}
