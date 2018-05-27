package com.coconut.young.wateringcan.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.coconut.young.wateringcan.MainActivity;
import com.coconut.young.wateringcan.utils.Utilities;

/**
 *  PreferenceActivity boilerplate for the Settings Page
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utilities.scheduleNextJob(this,
                this.getSharedPreferences(MainActivity.SHARED_PREFERENCES_NAME, MODE_PRIVATE));
    }

}
