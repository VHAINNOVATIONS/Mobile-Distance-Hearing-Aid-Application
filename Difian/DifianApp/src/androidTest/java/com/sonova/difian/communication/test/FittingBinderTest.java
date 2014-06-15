// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.test;

import android.content.Intent;
import android.test.ServiceTestCase;
import com.sonova.difian.communication.FittingBinder;
import com.sonova.difian.communication.fittingconnection.FittingConnectionError;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManagerCallback;
import com.sonova.difian.communication.fittingconnection.FittingConnectionManagerState;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.fittingconnection.test.mock.MockFittingConnectionManager;
import com.sonova.difian.communication.test.mock.MockFittingService;

public final class FittingBinderTest extends ServiceTestCase<MockFittingService>
{
    private FittingBinder _binder;

    public FittingBinderTest()
    {
        super(MockFittingService.class);
    }

    @SuppressWarnings("ProhibitedExceptionDeclared")
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        _binder = (FittingBinder)bindService(new Intent(getSystemContext(), MockFittingService.class));
        startService(new Intent(getSystemContext(), MockFittingService.class));
    }

    private void start()
    {
        _binder.setFittingDeviceAddress("DE:AD:BE:EF:42");
        _binder.setRelayConfiguration("mock.yaler.invalid");
        _binder.start();
    }

    public void testInjectionSuccessful()
    {
        start();
        FittingConnectionManagerCallback fittingCallback = MockFittingConnectionManager.getLastListener();
        assertNotNull("Callbacks can be injected into FittingBinder.", fittingCallback);
    }

    public void testTimerStartsWhenSessionStarts()
    {
        start();

        FittingConnectionManagerCallback fittingCallback = MockFittingConnectionManager.getLastListener();

        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }

        long startTime = _binder.getSessionDurationSeconds();
        assertEquals("Timer is at 0 seconds at beginning.", 0, startTime);

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.CONNECTING_SERIAL, FittingConnectionError.NONE, "Test-ID"));

        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }

        long connectingTime = _binder.getSessionDurationSeconds();
        assertEquals("Timer is at 0 seconds during connect.", 0, connectingTime);

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.CONNECTED, FittingConnectionError.NONE, "Test-ID"));

        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }

        long connectedTime = _binder.getSessionDurationSeconds();
        assertTrue("Timer is running while connected.", connectedTime > startTime);
    }

    public void testTimerPausesWhileThankYou()
    {
        start();

        FittingConnectionManagerCallback fittingCallback = MockFittingConnectionManager.getLastListener();

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.CONNECTING_SERIAL, FittingConnectionError.NONE, "Test-ID"));

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.CONNECTED, FittingConnectionError.NONE, "Test-ID"));

        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }

        long connectedTime = _binder.getSessionDurationSeconds();
        assertTrue("Timer is running while connected.", connectedTime > 0);

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.CONNECTING_SERIAL, FittingConnectionError.NONE, "Test-ID"));

        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }

        long pausedTime = _binder.getSessionDurationSeconds();
        assertEquals("Timer is paused while reconnecting.", connectedTime, pausedTime);

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.CONNECTED, FittingConnectionError.NONE, "Test-ID"));

        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }

        long reconnectedTime = _binder.getSessionDurationSeconds();
        assertTrue("Timer is resumed when reconnected.", reconnectedTime > pausedTime);
    }

    public void testTimerResetsOnSessionEnd()
    {
        start();

        FittingConnectionManagerCallback fittingCallback = MockFittingConnectionManager.getLastListener();

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.CONNECTING_SERIAL, FittingConnectionError.NONE, "Test-ID"));

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.CONNECTED, FittingConnectionError.NONE, "Test-ID"));

        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }

        long connectedTime = _binder.getSessionDurationSeconds();
        assertTrue("Timer is running while connected.", connectedTime > 0);

        _binder.stop();

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.STOPPING, FittingConnectionError.NONE, null));

        fittingCallback.fittingConnectionManagerStateChanged(new FittingConnectionManagerState(FittingConnectionState.NOT_CONNECTED, FittingConnectionError.NONE, null));

        long stoppedTime = _binder.getSessionDurationSeconds();
        assertEquals("Timer is reset on session end", 0, stoppedTime);
    }
}
