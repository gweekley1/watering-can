package com.coconut.young.wateringcan;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 *  Holds the information needed to determine when a plant needs to be watered
 */
public class PlantSchedule {

    private static final String TAG = MainActivity.TAG + "." + PlantSchedule.class.getSimpleName();

    private String name;
    private Date refDate = new Date();
    private int waterInterval;
    private boolean waterToday = false;

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
    public static final int HALF_DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 12;
    public static final int ONE_DAY_IN_MILLISECONDS = HALF_DAY_IN_MILLISECONDS * 2;

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
        updateReferenceDate();

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
    // it is in format: Water <name> <today|in # day(s)>, and every <# day(s)>
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
        return waterInterval - (getDaysAfterReferenceDate() % waterInterval);
    }

    /**
     * Update the reference date to the most recent watering day
     */
    public void updateReferenceDate() {
        int days = getDaysAfterReferenceDate();
        Calendar c = Calendar.getInstance();
        c.setTime(refDate);
        c.add(Calendar.DATE, (days / waterInterval) * waterInterval);
        Log.d(TAG, String.format("Updating reference date from %s to %s",
                DATE_FORMAT.format(refDate), DATE_FORMAT.format(c.getTime())));
        refDate = c.getTime();
    }

    private int getDaysAfterReferenceDate() {
        long timeDiff = Calendar.getInstance().getTimeInMillis() - refDate.getTime();
        return (int) (timeDiff / ONE_DAY_IN_MILLISECONDS);
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
