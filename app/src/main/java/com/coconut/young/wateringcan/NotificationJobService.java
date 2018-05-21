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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.coconut.young.wateringcan.utils.Utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.coconut.young.wateringcan.MainActivity.SHARED_PREFERENCES_NAME;

/**
 * This JobService loads the PlantSchedules from SharedPreferences, determines how many plants
 * need to be watered, and displays a notification if that number is > 0
 * This job is scheduled for every 6:30 AM and PM
 */
public class NotificationJobService extends JobService {

    private static final String TAG = "WateringCan/Service";

    @Override
    public boolean onStartJob(JobParameters params) {

        Log.i(TAG, "In NotificationJobService");
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);


        // Store the current time that the Job is running
        sharedPref.edit().putString(DebugActivity.DEBUG_LAST, new Date().toString()).apply();

        List<PlantSchedule> scheduleList = Utilities.loadScheduleList(sharedPref);

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
}
