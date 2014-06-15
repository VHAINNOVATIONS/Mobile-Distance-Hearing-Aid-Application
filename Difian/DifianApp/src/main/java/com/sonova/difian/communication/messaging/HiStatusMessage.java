// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.messaging;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;

public final class HiStatusMessage extends Message
{
    private final HiConnectionStatus[] _status = new HiConnectionStatus[2];
    private final HiMuteStatus[] _muteStatus = new HiMuteStatus[2];

    HiStatusMessage(XmlPullParser parser) throws MessageReaderException, XmlPullParserException, IOException
    {
        for (int i = 0; i < 2; i++)
        {
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, XmlConstants.HI);

            String sideString = parser.getAttributeValue(null, XmlConstants.SIDE);
            HiSide side;
            if (XmlConstants.LEFT.equals(sideString))
            {
                side = HiSide.LEFT;
            }
            else if (XmlConstants.RIGHT.equals(sideString))
            {
                side = HiSide.RIGHT;
            }
            else
            {
                throw new MessageReaderException(String.format("Invalid side in \"hi_status\" <message>: \"%s\".", sideString));
            }

            String statusString = parser.getAttributeValue(null, XmlConstants.STATUS);
            HiConnectionStatus status;
            if (XmlConstants.CONNECTED.equals(statusString))
            {
                status = HiConnectionStatus.CONNECTED;
            }
            else if (XmlConstants.DISCONNECTED.equals(statusString))
            {
                status = HiConnectionStatus.DISCONNECTED;
            }
            else if (XmlConstants.NOT_CONNECTED.equals(statusString))
            {
                status = HiConnectionStatus.NOT_CONNECTED;
            }
            else if (XmlConstants.UNDEFINED.equals(statusString))
            {
                status = HiConnectionStatus.UNDEFINED;
            }
            else
            {
                throw new MessageReaderException("Invalid status in \"hi_status\" <message>.");
            }

            String muteStatusString = parser.getAttributeValue(null, XmlConstants.MUTE_STATUS);
            HiMuteStatus muteStatus;
            if (XmlConstants.MUTED.equals(muteStatusString))
            {
                muteStatus = HiMuteStatus.MUTED;
            }
            else if (XmlConstants.UNMUTED.equals(muteStatusString))
            {
                muteStatus = HiMuteStatus.UNMUTED;
            }
            else if (XmlConstants.UNDEFINED.equals(muteStatusString))
            {
                muteStatus = HiMuteStatus.UNDEFINED;
            }
            else
            {
                throw new MessageReaderException("Invalid mute_status in \"hi_status\" <message>.");
            }

            setStatus(side, status);
            setMuteStatus(side, muteStatus);

            parser.nextTag();
            parser.require(XmlPullParser.END_TAG, null, "hi");
        }
    }

    @Override
    public MessageType getType()
    {
        return MessageType.HI_STATUS;
    }

    public HiConnectionStatus getStatus(HiSide side)
    {
        HiConnectionStatus result = _status[side.ordinal()];
        return (result != null) ? result : HiConnectionStatus.UNDEFINED;
    }

    private void setStatus(HiSide side, HiConnectionStatus status)
    {
        _status[side.ordinal()] = status;
    }

    public HiMuteStatus getMuteStatus(HiSide side)
    {
        HiMuteStatus result = _muteStatus[side.ordinal()];
        return (result != null) ? result : HiMuteStatus.UNDEFINED;
    }

    private void setMuteStatus(HiSide side, HiMuteStatus muteStatus)
    {
        _muteStatus[side.ordinal()] = muteStatus;
    }
}
