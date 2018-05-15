package com.coconut.young.wateringcan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.coconut.young.wateringcan.utils.Utilities;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/*
 * Watering Can is a lightweight app for tracking when you should water your plants. Each
 * watering schedule is a list entry on the main activity. Each entry tells you which plant it
 * refers to, when it next needs to be watered, and how often it needs to be watered. When adding
 * a schedule, you enter the plant's/schedule's name, the reference date by which the schedule
 * schedule is calculated (in MM/DD/YY format), and the how often to water the plant (in days)
 *
 * @author G Weekley
 */

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "WateringCan";
    /*Package-Private*/ static final String SHARED_PREFERENCES_NAME = "WateringCanPreferences";

    private static List<PlantSchedule> scheduleList;
    public static PlantScheduleAdapter adapter;

    private static SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        scheduleList = Utilities.loadScheduleList(sharedPref);

        // The "Add a new PlantSchedule" button, opens the EditActivity
        FloatingActionButton fab = findViewById(R.id.add_plant);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, EditActivity.class);
                // pass it the current date by default
                myIntent.putExtra("date", PlantSchedule.DATE_FORMAT.format(Calendar.getInstance().getTime()));
                startActivityForResult(myIntent, 1);
            }
        });

        // The "Debug Menu" button, opens the DebugActivity.
        // This button is invisible until the user names a plant "DEBUG"
        ImageButton debugButton = findViewById(R.id.debug);
        assert debugButton != null;
        debugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, DebugActivity.class);
                myIntent.putExtra(DebugActivity.DEBUG_NEXT, sharedPref.getString(DebugActivity.DEBUG_NEXT, "N/A"));
                myIntent.putExtra(DebugActivity.DEBUG_LAST, sharedPref.getString(DebugActivity.DEBUG_LAST, "N/A"));
                startActivity(myIntent);
            }
        });
        debugButton.setVisibility(ImageView.INVISIBLE);

        ListView listView = findViewById(android.R.id.list);

        adapter = new PlantScheduleAdapter(MainActivity.this,
                scheduleList);
        assert listView != null;
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int posit, long l) {
                PlantSchedule item = adapter.getItem(posit);
                assert item != null;

                Intent myIntent = new Intent(MainActivity.this, EditActivity.class);
                // to edit an item, pass it's info to the EditActivity
                myIntent.putExtra("update", posit);
                myIntent.putExtra("name", item.getName());
                myIntent.putExtra("date", PlantSchedule.DATE_FORMAT.format(item.getRefDate()));
                myIntent.putExtra("interval", item.getWaterInterval());
                startActivityForResult(myIntent, 1);
            }
        });
        adapter.notifyDataSetChanged();

        // set up the alarm intent to update every schedule's icon
       Utilities.scheduleNextJob(MainActivity.this,sharedPref);
    }

    @Override
    public void onPause() {
        super.onPause();
        Utilities.saveScheduleList(sharedPref, scheduleList);
        Utilities.scheduleNextJob(MainActivity.this, sharedPref);
    }

    @Override
    public void onResume() {
        super.onResume();
        Utilities.scheduleNextJob(MainActivity.this, sharedPref);
    }

    // this method is called when returning from the EditActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {

                String name = data.getStringExtra("name");
                if (DebugActivity.DEBUG.equals(name)) {
                    ImageButton debugButton = findViewById(R.id.debug);
                    assert debugButton != null;
                    debugButton.setVisibility(ImageButton.VISIBLE);
                    return;
                }

                int update = data.getIntExtra("update", -1);

                if (data.getBooleanExtra("delete", false)) {
                    Log.i(TAG, "Deleting " + update);
                    scheduleList.remove(update);
                    adapter.notifyDataSetChanged();
                    Utilities.saveScheduleList(sharedPref, scheduleList);
                    return;
                }


                Date nextDate = null;
                try {
                    nextDate = PlantSchedule.DATE_FORMAT.parse(data.getStringExtra("date"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                assert nextDate != null;

                int waterInterval = data.getIntExtra("interval", -1);

                Log.i(TAG, "Got result " + update + " " + name + " " + PlantSchedule.DATE_FORMAT.format(nextDate) + " " + waterInterval);
                PlantSchedule newSchedule = new PlantSchedule(name, nextDate, waterInterval);

                // is this PlantSchedule a new one or replacing an old one?
                if (update != -1) {
                    scheduleList.add(update, newSchedule);
                    scheduleList.remove(update + 1);
                } else {
                    scheduleList.add(newSchedule);
                }

                adapter.notifyDataSetChanged();
                Utilities.saveScheduleList(sharedPref, scheduleList);
            }
        }
    }

}
