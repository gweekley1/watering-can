package com.coconut.young.wateringcan;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.coconut.young.wateringcan.utils.Utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.coconut.young.wateringcan.MainActivity.SHARED_PREFERENCES_NAME;

/**
 * This BroadcastReceiver updates every PlantSchedule's waterToday boolean
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = "WateringCan/Alarm";

    public AlarmReceiver() { }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "In AlarmReceiver");

        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
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
            MainActivity.adapter.notifyDataSetChanged();
            displayNotification(numPlants, context);
            Utilities.saveScheduleList(sharedPref, scheduleList);
        }
    }

    /*
     * Displays a notification informing the user that plants need to be watered
     *
     * @param numPlants the number of plants that need to be watered
     */
    public static void displayNotification(int numPlants, Context context) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;

        String notificationString = numPlants + (numPlants == 1 ? " plant needs" : " plants need") + " to be watered";

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.wateringcan_notification)
                        .setContentTitle("Watering Can")
                        .setContentText(notificationString);

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
