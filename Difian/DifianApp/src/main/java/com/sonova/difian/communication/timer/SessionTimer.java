// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.timer;

import java.util.Date;

public final class SessionTimer
{
    private final Object _lockObject = new Object();
    private long _secondOffset;
    private Date _partStartTime;

    public void reset()
    {
        synchronized (_lockObject)
        {
            _secondOffset = 0;
            _partStartTime = null;
        }
    }

    public void start()
    {
        synchronized (_lockObject)
        {
            if (_partStartTime == null)
            {
                _partStartTime = new Date();
            }
        }
    }

    public void pause()
    {
        synchronized (_lockObject)
        {
            if (_partStartTime != null)
            {
                Date now = new Date();
                long seconds = (now.getTime() - _partStartTime.getTime()) / 1000;

                _secondOffset += seconds;
                _partStartTime = null;
            }
        }
    }

    public long getSessionSeconds()
    {
        synchronized (_lockObject)
        {
            long result = _secondOffset;
            if (_partStartTime != null)
            {
                Date now = new Date();
                result += (now.getTime() - _partStartTime.getTime()) / 1000;
            }
            return result;
        }
    }
}
