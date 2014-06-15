// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.graphics.Bitmap;
import com.sonova.difian.communication.fittingconnection.FittingConnectionError;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.messaging.HiMuteStatus;

public interface FittingServiceConnectionFragmentCallback
{
    @SuppressWarnings("UnusedParameters")
    void onFittingServiceStateUpdated(FittingConnectionState state, FittingConnectionError error, String id, String audiologistName, Bitmap audiologistPicture, HiMuteStatus muteStatusLeft, HiMuteStatus muteStatusRight);
}
