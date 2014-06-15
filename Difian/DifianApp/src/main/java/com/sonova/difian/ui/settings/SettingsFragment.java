// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.sonova.difian.R;
import com.sonova.difian.utilities.Contract;

final class SettingsFragment extends PreferenceFragment
{
    private static final String PREF_RELAY_REGION = "pref_relay_region";

    private SharedPreferences.OnSharedPreferenceChangeListener _listener = new SharedPreferences.OnSharedPreferenceChangeListener()
    {
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            if (PREF_RELAY_REGION.equals(key)) {
                setRelayRegionPreferenceSummary(sharedPreferences);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        setRelayRegionPreferenceSummary(sharedPreferences);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(_listener);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(_listener);
    }

    private void setRelayRegionPreferenceSummary (SharedPreferences sharedPreferences) {
        String[] regionKeys = getResources().getStringArray(R.array.pref_relay_region_values);
        String[] regionEntries = getResources().getStringArray(R.array.pref_relay_region_entries);
        String regionKey = sharedPreferences.getString(PREF_RELAY_REGION, null);
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
        findPreference(PREF_RELAY_REGION).setSummary(regionEntries[i]);
    }
}
