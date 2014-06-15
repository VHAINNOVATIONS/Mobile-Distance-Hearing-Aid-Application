// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.sonova.difian.R;
import com.sonova.difian.utilities.Contract;

public final class FittingInfoHelpers
{
    public static final String FITTING_DEVICE_ADDRESS_KEY = "com.sonova.difian.preferences.fittingdeviceaddress";
    public static final String FITTING_DEVICE_ID_KEY = "com.sonova.difian.preferences.fittingdeviceid";
    public static final String RELAY_REGION_KEY = "pref_relay_region";

    private FittingInfoHelpers()
    {
    }

    public static void updateFittingDeviceIdAndRegion(Activity activity)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        String id = preferences.getString(FITTING_DEVICE_ID_KEY, activity.getString(R.string.current_fitting_device));
        String[] regionKeys = activity.getResources().getStringArray(R.array.pref_relay_region_values);
        String[] regionEntries = activity.getResources().getStringArray(R.array.pref_relay_region_entries);
        String regionKey = preferences.getString(RELAY_REGION_KEY, null);
        Contract.check(regionKey != null);
        int i = 0;
        boolean found = false;
        while (!found && (i < regionKeys.length))
        {
            found = regionKey.equals(regionKeys[i]);
            if (!found)
            {
                i++;
            }
        }
        Contract.check(found);
        activity.getActionBar().setTitle("Distance Support " + id);
        activity.getActionBar().setSubtitle(regionEntries[i]);
    }
}
