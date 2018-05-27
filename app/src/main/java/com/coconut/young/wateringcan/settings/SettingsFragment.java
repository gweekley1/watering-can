package com.coconut.young.wateringcan.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.coconut.young.wateringcan.R;

/**
 *  PreferenceFragment boilerplate for the Settings page
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

}
