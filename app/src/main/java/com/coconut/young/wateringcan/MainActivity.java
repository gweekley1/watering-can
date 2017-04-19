package com.coconut.young.wateringcan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/*
 * TODO: Improve look of app, specifically alignment and placement of fields on different devices
 * TODO:  as well as making prettier list entries
 * TODO: add "Watered today" toggle to list entries
 * TODO: add alarm schedule to set above boolean and deliver notifications/change icon
 * TODO: Update README.md
 *
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

    private static List<PlantSchedule> scheduleList;
    private static PlantScheduleAdapter adapter;

    private SharedPreferences sharedPref;
    // these strings are used while saving the list of PlantSchedules as a string
    private static final String SCHEDULE_SEPARATOR = "%NEWSCHEDULE%";
    private static final String PART_SEPARATOR = "%PART%";
    private static final String PERSISTENT_SCHEDULES = "savedSchedules";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        scheduleList = loadScheduleList();

        // The "Add a new PlantSchedule" button, opens the EditActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_plant);
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

        ListView listView = (ListView) findViewById(android.R.id.list);

        adapter = new PlantScheduleAdapter(MainActivity.this,
                android.R.layout.simple_list_item_1,
                scheduleList);
        assert listView != null;
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int posit, long l) {
                PlantSchedule item = adapter.getItem(posit);
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
        Intent alarmIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // calculate epoch time of the next 6:30 am in the device's timezone
        Calendar c = Calendar.getInstance();
        long timeZoneDif = c.getTimeZone().getOffset(c.getTimeInMillis());
        long when = c.getTimeInMillis()
                - (c.getTimeInMillis() % PlantSchedule.ONE_DAY_IN_MILLISECONDS)
                - timeZoneDif + (long) (1000*60*60*6.5);
        if (when < c.getTimeInMillis()) {
            when += PlantSchedule.ONE_DAY_IN_MILLISECONDS;
        }
        Log.i(TAG, "Scheduling alarm in " + (when - c.getTimeInMillis()) + " ms");
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, when, AlarmManager.INTERVAL_DAY, alarmPendingIntent);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveScheduleList();
    }

    // this method is called when returning from the EditActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                int update = data.getIntExtra("update", -1);

                if (data.getBooleanExtra("delete", false)) {
                    Log.i(TAG, "Deleting " + update);
                    scheduleList.remove(update);
                    adapter.notifyDataSetChanged();
                    saveScheduleList();
                    return;
                }

                String name = data.getStringExtra("name");
                Date nextDate = null;
                try {
                    nextDate = PlantSchedule.DATE_FORMAT.parse(data.getStringExtra("date"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
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
                saveScheduleList();
            }
        }
    }



    // converts scheduleList to a string and saves it in persistent storage
    private void saveScheduleList() {

        String listAsString = "";

        for (PlantSchedule sched : scheduleList) {
            listAsString += sched.getName() + PART_SEPARATOR
                    + PlantSchedule.DATE_FORMAT.format(sched.getRefDate()) + PART_SEPARATOR
                    + sched.getWaterInterval() + PART_SEPARATOR
                    + sched.getWaterToday() + SCHEDULE_SEPARATOR;
        }

        Log.i(TAG, "Saving " + listAsString);

        sharedPref.edit().putString(PERSISTENT_SCHEDULES, listAsString).apply();
    }

    // load the string from persistent storage and convert it to a list
    private List<PlantSchedule> loadScheduleList() {

        List<PlantSchedule> list = new ArrayList<>();
        String unformattedList = sharedPref.getString(PERSISTENT_SCHEDULES, "");

        Log.i(TAG, "Loaded " + unformattedList);

        if (!unformattedList.equals("")) {
            for (String date : unformattedList.split(SCHEDULE_SEPARATOR)) {
                String[] parts= date.split(PART_SEPARATOR);
                try {
                    list.add(new PlantSchedule(parts[0], PlantSchedule.DATE_FORMAT.parse(parts[1]), Integer.valueOf(parts[2]), Boolean.valueOf(parts[3])));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;
    }

    // this BroadcastReceiver updates every PlantSchedule's waterToday boolean
    public static class AlarmReceiver extends BroadcastReceiver {

        public AlarmReceiver() { }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "In AlarmReceiver");
            for (PlantSchedule sched : scheduleList) {
                sched.setWaterToday(sched.shouldWaterToday());
            }
            adapter.notifyDataSetChanged();
        }
    }

}
