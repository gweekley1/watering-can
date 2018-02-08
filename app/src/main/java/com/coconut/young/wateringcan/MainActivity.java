package com.coconut.young.wateringcan;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private static List<PlantSchedule> scheduleList;
    private static PlantScheduleAdapter adapter;

    private static SharedPreferences sharedPref;
    private static final String PERSISTENT_SCHEDULES = "savedSchedules";

    private static NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        scheduleList = loadScheduleList();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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
       scheduleNextAlarm(getApplicationContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        saveScheduleList();
        scheduleNextAlarm(getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleNextAlarm(getApplicationContext());
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
    private static void saveScheduleList() {

        JSONArray savedArray = new JSONArray();

        for (PlantSchedule sched : scheduleList) {
            JSONObject schedJson = new JSONObject();

            try {
                schedJson.put("name", sched.getName());
                schedJson.put("date", PlantSchedule.DATE_FORMAT.format(sched.getRefDate()));
                schedJson.put("interval", sched.getWaterInterval());
                schedJson.put("water", sched.getWaterToday());
                savedArray.put(schedJson);
            } catch (JSONException e) {
                Log.e(TAG, "Exception while saving PlantSchedule");
            }
        }

        Log.i(TAG, "Saving " + savedArray.toString());

        sharedPref.edit().putString(PERSISTENT_SCHEDULES, savedArray.toString()).apply();
    }

    // load the string from persistent storage and convert it to a list
    private static List<PlantSchedule> loadScheduleList() {

        List<PlantSchedule> list = new ArrayList<>();
        String unformattedList = sharedPref.getString(PERSISTENT_SCHEDULES, "");

        Log.i(TAG, "Loaded " + unformattedList);

        try {
            JSONArray savedArray = new JSONArray(unformattedList);
            for (int i = 0; i < savedArray.length(); ++i) {
                list.add(new PlantSchedule(savedArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Exception loading PlantSchedule");
        }

        return list;
    }

    // this BroadcastReceiver updates every PlantSchedule's waterToday boolean
    public static class AlarmReceiver extends BroadcastReceiver {

        public AlarmReceiver() { }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "In AlarmReceiver");

            if (scheduleList == null) {
                scheduleList = loadScheduleList();
            }

            SimpleDateFormat timeParser = new SimpleDateFormat("HH:mm", Locale.US);
            String noonTime = "12:00";
            String currentTime= timeParser.format(new Date());

            int numPlants = 0;
            for (PlantSchedule sched : scheduleList) {

                sched.updateReferenceDate();

                boolean alreadySet = sched.getWaterToday();

                // if this is the 6:30 PM alarm and the plant should be watered today,
                // but waterToday is false, we assume that it has been watered and do not count it
                if (!(currentTime.compareTo(noonTime) > 0 && !alreadySet) && sched.shouldWaterToday()) {
                    sched.setWaterToday(true);
                    ++numPlants;
                }
            }

            if (numPlants > 0) {
                adapter.notifyDataSetChanged();
                displayNotification(numPlants, context);
                saveScheduleList();
            }
            scheduleNextAlarm(context);
        }
    }

    /*
     * Displays a notification informing the user that plants need to be watered
     *
     * @param numPlants the number of plants that need to be watered
     */
    public static void displayNotification(int numPlants, Context context) {

        String notificationString = numPlants + (numPlants == 1 ? " plant needs" : " plants need") + " to be watered";

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.wateringcan_notification)
                        .setContentTitle("Watering Can")
                        .setContentText(notificationString)
                        .setColor(context.getResources().getColor(R.color.colorPrimaryDark));

        Intent intent = new Intent(context, MainActivity.class);

        // build a fake stack so the back button will work properly
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        notificationManager.notify(0, builder.build());
        Log.i(TAG, "Displayed notification");
    }

    /**
     * Schedule an alarm for the next 6:30 (am or pm) that will repeat approximately every 12 hours
     *
     * @param context
     */
    private static void scheduleNextAlarm(Context context) {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // calculate epoch time of the next 6:30 in the device's timezone
        Calendar c = Calendar.getInstance();
        long timeZoneDif = c.getTimeZone().getOffset(c.getTimeInMillis());
        long when = c.getTimeInMillis()
                - (c.getTimeInMillis() % PlantSchedule.HALF_DAY_IN_MILLISECONDS)
                - timeZoneDif + (long) (1000*60*60*6.5);
        if (when < c.getTimeInMillis()) {
            when += PlantSchedule.HALF_DAY_IN_MILLISECONDS;
        }

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, when, AlarmManager.INTERVAL_HALF_DAY, alarmPendingIntent);

        long timeToAlarm = (when - c.getTimeInMillis()) / 1000;
        Log.i(TAG, String.format("Scheduled alarm in %s h %s m %s s", timeToAlarm / 3600,
                (timeToAlarm % 3600) / 60,
                (timeToAlarm % 60)
        ));
    }

}
