// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.test.mock;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.sonova.difian.communication.FittingBinder;
import com.sonova.difian.communication.fittingconnection.test.mock.MockFittingConnectionManagerFactory;

public final class MockFittingService extends Service
{
    private FittingBinder _binder;

    @Override
    public void onCreate()
    {
        super.onCreate();
        _binder = new FittingBinder(this, new MockFittingConnectionManagerFactory());
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return _binder;
    }
}
