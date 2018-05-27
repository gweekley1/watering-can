package com.coconut.young.wateringcan;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.coconut.young.wateringcan.settings.DebugActivity;
import com.coconut.young.wateringcan.utils.Utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.coconut.young.wateringcan.MainActivity.SHARED_PREFERENCES_NAME;
import static com.coconut.young.wateringcan.PlantSchedule.ONE_DAY_IN_MILLIS;

/**
 * This JobService loads the PlantSchedules from SharedPreferences, determines how many plants
 * need to be watered, and displays a notification if that number is > 0
 * This job is scheduled for every PREF_TIME and every PREF_FREQ afterwards
 * (Defaults are 6:30 AM and 12 hours, respectively)
 */
public class NotificationJobService extends JobService {

    private static final String TAG = "WateringCan/Service";

    @Override
    public boolean onStartJob(JobParameters params) {

        Log.i(TAG, "In NotificationJobService");
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        Date lastJob;
        try {
             lastJob = SimpleDateFormat.getDateInstance().parse(
                    sharedPref.getString(DebugActivity.DEBUG_LAST, ""));
        } catch (ParseException e) {
            Log.w(TAG, "No recorded last alarm");
            lastJob = new Date();
            lastJob.setTime(lastJob.getTime() - ONE_DAY_IN_MILLIS);
        }

        // Store the current time that the Job is running
        sharedPref.edit().putString(DebugActivity.DEBUG_LAST, new Date().toString()).apply();

        List<PlantSchedule> scheduleList = Utilities.loadScheduleList(sharedPref);

        int numPlants = 0;
        for (PlantSchedule sched : scheduleList) {

            sched.updateReferenceDate();

            boolean alreadySet = sched.getWaterToday();

            // if this is the day's first alarm and the plant should be watered today,
            // but waterToday is false, we assume that it has been watered and do not count it
            if (!(isToday(lastJob) && !alreadySet) && sched.shouldWaterToday()) {
                sched.setWaterToday(true);
                ++numPlants;
            }
        }

        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showNotification = defaultSharedPref.getBoolean(
                this.getResources().getString(R.string.pref_notify_key), true);

        if (showNotification && numPlants > 0) {
            displayNotification(numPlants, this);
            Utilities.saveScheduleList(sharedPref, scheduleList);

            if (MainActivity.adapter != null) {
                MainActivity.adapter.notifyDataSetChanged();
            }
        }

        // Periodic jobs can't be scheduled for a specific time so this Job must reschedule itself
        Utilities.scheduleNextJob(this, sharedPref);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    /*
     * Displays a notification informing the user that plants need to be watered
     *
     * @param numPlants the number of plants that need to be watered
     */
    private static void displayNotification(int numPlants, Context context) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;

        String notificationString = numPlants + (numPlants == 1 ? " plant needs" : " plants need") + " to be watered";

        NotificationCompat.Builder builder;

        // Only use the NotificationCompat.Builder ctor with NotificationChannels if
        //  Android Version >= O and it is necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(context, Utilities.NOTIFICATION_CHANNEL_ID)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.wateringcan_notification2)
                    .setContentTitle("Watering Can")
                    .setContentText(notificationString);
        } else {
            builder = new NotificationCompat.Builder(context)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.wateringcan_notification2)
                    .setContentTitle("Watering Can")
                    .setContentText(notificationString);
        }


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
     * Determines whether or not a given Date is today
     *
     * @param date The Date to check
     * @return whether or not the Date is today
     */
    private static boolean isToday(Date date){
        Calendar today = Calendar.getInstance();
        Calendar specifiedDate  = Calendar.getInstance();
        specifiedDate.setTime(date);

        return today.get(Calendar.DAY_OF_MONTH) == specifiedDate.get(Calendar.DAY_OF_MONTH)
                &&  today.get(Calendar.MONTH) == specifiedDate.get(Calendar.MONTH)
                &&  today.get(Calendar.YEAR) == specifiedDate.get(Calendar.YEAR);
    }


}
