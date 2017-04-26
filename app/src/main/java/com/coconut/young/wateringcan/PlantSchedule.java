package com.coconut.young.wateringcan;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *  Holds the information needed to determine when a plant needs to be watered
 */
public class PlantSchedule {

    private static final String TAG = MainActivity.TAG + "." + PlantSchedule.class.getSimpleName();

    private String name;
    private Date refDate = new Date();
    private int waterInterval;
    private boolean waterToday = false;

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");
    public static final int ONE_DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;

    /*
     * Constructor to build a plant schedule, evaluates and sets waterToday
     *
     * @param name the plant's display name
     * @param refDate the date used to calculate when to water
     * @param waterInterval how often to water the plant, in days
     */
    public PlantSchedule(String name, Date refDate, int waterInterval) {
        this.name = name;
        // Ensure that the reference date is before the current date
        long currentTime = Calendar.getInstance().getTimeInMillis();
        while (currentTime < refDate.getTime()) {
            refDate.setTime(refDate.getTime() - waterInterval * ONE_DAY_IN_MILLISECONDS);
        }
        // Set the clock of the reference date to 6 am, which the app considers the start of the day
        this.refDate.setTime(refDate.getTime() - (refDate.getTime() % ONE_DAY_IN_MILLISECONDS) + 1000 * 60 * 60 * 6);
        this.waterInterval = waterInterval;

        this.waterToday = shouldWaterToday();
    }

    /*
     * Constructor to build a plant schedule from a JSONObject
     * used to load stored PlantSchedules from persistent storage
     *
     * @param json must hold the keys: `name`,`interval`,`water`,`date`
     */
    public PlantSchedule(JSONObject json) {
        try {
            this.name = json.getString("name");
            this.waterInterval = json.getInt("interval");
            this.waterToday = json.getBoolean("water");

            Date tempRef = DATE_FORMAT.parse(json.getString("date"));
            this.refDate.setTime(tempRef.getTime() - (tempRef.getTime() % ONE_DAY_IN_MILLISECONDS) + 1000 * 60 * 60 * 6);
        } catch (JSONException e) {
            Log.e(TAG, "Exception building PlantSchedule from JSON");
        } catch (ParseException e) {
            Log.e(TAG, "Exception parsing PlantSchedule.refDate from JSON");
        }
    }

    // the string is formatted to display to the user on the MainActivity
    public String toString() {
        String instruction = "Water " + name + " ";
        if (shouldWaterToday()) {
            instruction += "today";
        } else if (getDaysToWater() == 1) {
            instruction += "in " + getDaysToWater() + " day";
        } else {
            instruction += "in " + getDaysToWater() + " days";
        }
        if (waterInterval == 1) {
            instruction += ", and every 1 day";
        } else {
            instruction += ", and every " + waterInterval + " days";
        }

        return instruction;
    }

    public boolean shouldWaterToday() {
        int days = getDaysToWater();
        return days >= 0 && days % waterInterval == 0;
    }

    private int getDaysToWater() {
        long timeDiff = Calendar.getInstance().getTimeInMillis() - refDate.getTime();
        int days = (int) (timeDiff / ONE_DAY_IN_MILLISECONDS);
        return waterInterval - (days % waterInterval);
    }

    /*
     * Setters and Getters
     */
    public void setName(String newName) {
        name = newName;
    }

    public String getName() {
        return name;
    }

    public void setRefDate(Date newDate) {
        refDate = newDate;
    }

    public Date getRefDate() {
        return refDate;
    }

    public String getFormattedNextDate() {
        return DATE_FORMAT.format(refDate);
    }

    public void setWaterInterval(int newInterval) {
        waterInterval = newInterval;
    }

    public int getWaterInterval() {
        return waterInterval;
    }

    public void setWaterToday(boolean waterToday) {
        this.waterToday = waterToday;
    }

    public boolean getWaterToday() {
        return waterToday;
    }
}
