package com.coconut.young.wateringcan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * The Activity used to view debug information on the app's alarms
 */
public class DebugActivity extends Activity {

    public static final String TAG = "DebugWateringCan";

    protected static final String DEBUG_NEXT = "nextAlarm";
    protected static final String DEBUG_LAST = "lastAlarm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        final Intent intent = getIntent();

        // Fill the fields with passed data
        final TextView nextAlarm = (TextView) findViewById(R.id.debug_next_text);
        String nextAlarmText = getString(R.string.debug_next, intent.getStringExtra(DEBUG_NEXT));
        nextAlarm.setText(nextAlarmText);

        final TextView lastAlarm = (TextView) findViewById(R.id.debug_last_text);
        String lastAlarmText = getString(R.string.debug_last, intent.getStringExtra(DEBUG_LAST));
        lastAlarm.setText(lastAlarmText);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setText(TextView field, String text) {
        if (text == null)
            text = "";
        field.setText(text);
    }

}
