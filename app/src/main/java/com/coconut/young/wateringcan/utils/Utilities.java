package com.coconut.young.wateringcan.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.coconut.young.wateringcan.AlarmReceiver;
import com.coconut.young.wateringcan.DebugActivity;
import com.coconut.young.wateringcan.PlantSchedule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.coconut.young.wateringcan.MainActivity.TAG;

public class Utilities {

    private static final String PERSISTENT_SCHEDULES = "savedSchedules";

    /**
     * Converts scheduleList to a string and saves it in persistent storage
     *
     * @param sharedPref the SharedPreferences to store the ScheduleList in at PERSISTENT_SCHEDULES
     * @param scheduleList the List<PlantSchedule> to store in persistent storage
     */
    public static void saveScheduleList(SharedPreferences sharedPref, List<PlantSchedule> scheduleList) {

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

    /**
     * Load the string from persistent storage and convert it to a list
     *
     * @param sharedPref the SharedPreferences to get the ScheduleList from at PERSISTENT_SCHEDULES
     */
    public static List<PlantSchedule> loadScheduleList(SharedPreferences sharedPref) {

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


    /**
     * Schedule an alarm for the next 6:30 (am or pm) that will repeat approximately every 12 hours
     *
     * @param context The context to use to build Intents and get Services
     * @param sharedPref the SharedPreferences to store the alarm execution debug info
     */
    public static void scheduleNextAlarm(Context context, SharedPreferences sharedPref) {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, 1, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        assert alarmManager != null;

        // calculate the time of the next 6:30 in the device's timezone
        Calendar c = Calendar.getInstance();
        Date currentTime = c.getTime();
        c.set(Calendar.HOUR_OF_DAY,6);
        c.set(Calendar.MINUTE,30);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);

        while (currentTime.after(c.getTime())) {
            c.setTimeInMillis(c.getTimeInMillis() + PlantSchedule.HALF_DAY_IN_MILLISECONDS);
        }
        long when = c.getTimeInMillis();

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, when, PlantSchedule.HALF_DAY_IN_MILLISECONDS, alarmPendingIntent);

        Date nextAlarm = new Date();
        nextAlarm.setTime(when);
        sharedPref.edit().putString(DebugActivity.DEBUG_NEXT, nextAlarm.toString()).apply();

        long timeToAlarm = (when - currentTime.getTime()) / 1000;
        Log.i(TAG, String.format("Scheduled alarm in %s h %s m %s s", timeToAlarm / 3600,
                (timeToAlarm % 3600) / 60,
                (timeToAlarm % 60)
        ));
    }

}
