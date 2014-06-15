// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.sonova.difian.communication.fittingconnection.DefaultFittingConnectionManagerFactory;

public final class FittingService extends Service
{
    private FittingBinder _binder;

    @Override
    public void onCreate()
    {
        super.onCreate();
        _binder = new FittingBinder(this, new DefaultFittingConnectionManagerFactory());
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return _binder;
    }
}
