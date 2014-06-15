// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.timer.test;

import com.sonova.difian.communication.timer.SessionTimer;
import junit.framework.TestCase;

public final class SessionTimerTest extends TestCase
{
    private SessionTimer _sessionTimer;

    @SuppressWarnings("ProhibitedExceptionDeclared")
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        _sessionTimer = new SessionTimer();
    }

    public void initializationTest()
    {
        assertEquals("Timer is initialized to 0.", 0, _sessionTimer.getSessionSeconds());
    }

    public void testReset()
    {

        _sessionTimer.start();
        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }
        _sessionTimer.reset();

        assertEquals("Timer is reset to 0 after calling reset().", 0, _sessionTimer.getSessionSeconds());

        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }

        assertEquals("Timer does not continue after resetting.", 0, _sessionTimer.getSessionSeconds());
    }

    public void testStart()
    {
        long startTime = _sessionTimer.getSessionSeconds();
        _sessionTimer.start();
        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }
        long middleTime = _sessionTimer.getSessionSeconds();
        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }
        long endTime = _sessionTimer.getSessionSeconds();

        assertTrue("Timer counted up as time elapsed.", middleTime > startTime);
        assertTrue("Timer counted up further as more time elapsed.", endTime > middleTime);
    }

    public void testPause()
    {
        long startTime = _sessionTimer.getSessionSeconds();
        _sessionTimer.start();
        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }
        _sessionTimer.pause();
        long pauseTime = _sessionTimer.getSessionSeconds();
        try
        {
            Thread.sleep(2500);
        }
        catch (InterruptedException ignored)
        {
        }
        long endTime = _sessionTimer.getSessionSeconds();

        assertEquals("Timer did not proceed when it was paused.", pauseTime, endTime);
        assertTrue("Timer did go up before pausing it.", pauseTime > startTime);
    }
}
