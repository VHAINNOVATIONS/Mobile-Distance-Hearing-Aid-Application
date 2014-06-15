// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.test;

class ConnectionTestResult {
    static final int STATE_UNDEFINED = 0;
    static final int STATE_SUCCESS = 1;
    static final int STATE_ERROR = 2;
    int state;
    int latency;
    Exception exception;
}
