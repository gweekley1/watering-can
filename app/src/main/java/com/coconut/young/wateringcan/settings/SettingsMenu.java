package com.coconut.young.wateringcan.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import com.coconut.young.wateringcan.R;

import static com.coconut.young.wateringcan.MainActivity.SHARED_PREFERENCES_NAME;

/**
 *  The Settings popup menu on the MainActivity. Either has "Settings" or "Settings" and "Debug"
 */
public class SettingsMenu extends PopupMenu {

    public SettingsMenu(Context context, View anchor) {
        super(context, anchor);

        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.setOnMenuItemClickListener(new SettingsMenuOnClickListener(context, sharedPref));
    }


    private class SettingsMenuOnClickListener implements OnMenuItemClickListener {

        private final Context context;
        private final SharedPreferences sharedPref;

        /*Package-Private*/ SettingsMenuOnClickListener(Context context, SharedPreferences sharedPref) {
            this.context = context;
            this.sharedPref = sharedPref;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {

            switch (item.getItemId()) {
                case R.id.action_settings:
                    openSettingsActivity();
                    break;
                case R.id.debug:
                    openDebugActivity();
                    break;
            }

            return true;
        }


        private void openSettingsActivity() {
            Intent intent = new Intent(context, SettingsActivity.class);
            context.startActivity(intent);
        }


        private void openDebugActivity() {
            Intent intent = new Intent(context, DebugActivity.class);
            intent.putExtra(DebugActivity.DEBUG_NEXT, sharedPref.getString(DebugActivity.DEBUG_NEXT, "N/A"));
            intent.putExtra(DebugActivity.DEBUG_LAST, sharedPref.getString(DebugActivity.DEBUG_LAST, "N/A"));

            context.startActivity(intent);
        }
    }
}
