package com.coconut.young.wateringcan.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.coconut.young.wateringcan.settings.DebugActivity;
import com.coconut.young.wateringcan.NotificationJobService;
import com.coconut.young.wateringcan.PlantSchedule;
import com.coconut.young.wateringcan.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.coconut.young.wateringcan.MainActivity.TAG;

public class Utilities {

    public static final String NOTIFICATION_CHANNEL_ID = "WateringCanChannel";

    private static final String PERSISTENT_SCHEDULES = "savedSchedules";
    public static final SimpleDateFormat FULL_DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.getDefault());

    private static final int HOUR_IN_MILLIS = 1000 * 60 * 60;
    private static final long FIFTEEN_MINUTES_IN_MILLIS = 1000 * 60 * 15;

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
     * Schedule a job for the next (PREF_TIME + n*PREF_FREQ) that will repeat approximately every PREF_FREQ hours
     *
     * @param context The context to use to build Components and get Services
     * @param sharedPref the SharedPreferences to store the alarm execution debug info
     */
    public static void scheduleNextJob(Context context, SharedPreferences sharedPref) {

        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        String[] nextJobTimeStrings = defaultSharedPref.getString(
                context.getResources().getString(R.string.pref_time_key), "6:30").split(":");
        int jobFrequency = Integer.parseInt(defaultSharedPref.getString(
                context.getResources().getString(R.string.pref_freq_key), "12"));

        // calculate the time of the next (PREF_TIME + n*PREF_FREQ) in the device's timezone
        Calendar c = Calendar.getInstance();
        Date currentTime = c.getTime();
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(nextJobTimeStrings[0]));
        c.set(Calendar.MINUTE, Integer.parseInt(nextJobTimeStrings[1]));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        while (currentTime.after(c.getTime())) {
            c.setTimeInMillis(c.getTimeInMillis() + jobFrequency * HOUR_IN_MILLIS);
        }
        long when = c.getTimeInMillis();

        sharedPref.edit().putString(DebugActivity.DEBUG_NEXT, FULL_DATE_FORMAT.format(c.getTime())).apply();

        long millisBeforeNextJob = when - currentTime.getTime();

        long timeToAlarm = millisBeforeNextJob / 1000;
        Log.i(TAG, String.format("Scheduled alarm in %s h %s m %s s  (= %s s)", timeToAlarm / 3600,
                (timeToAlarm % 3600) / 60,
                (timeToAlarm % 60),
                timeToAlarm
        ));

        ComponentName jobComponent = new ComponentName(context, NotificationJobService.class);

        // Create JobInfo specifying that the job needs to run at a specific time, then every PREF_FREQ hours
        // The job is persistent and can be idle or not, charging or not, and on any network (or lack thereof)
        JobInfo jobInfo = new JobInfo.Builder(1, jobComponent)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setMinimumLatency(millisBeforeNextJob)
                .setBackoffCriteria(FIFTEEN_MINUTES_IN_MILLIS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setOverrideDeadline(millisBeforeNextJob + jobFrequency * HOUR_IN_MILLIS)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setPersisted(true)
                .build();

        // Schedule the job
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        assert jobScheduler != null;
        jobScheduler.schedule(jobInfo);
    }

    /**
     * Create the Notification channel if Android Version is O or newer
     * This is necessary for notifications to work
     *
     * @param context The context used to get String resources and the NotificationManager
     */
    public static void createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.channel_description));

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

}
