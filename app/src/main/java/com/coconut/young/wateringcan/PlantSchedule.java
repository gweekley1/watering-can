package com.coconut.young.wateringcan;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 *  Holds the information needed to determine when a plant needs to be watered
 */
public class PlantSchedule {

    private String name;
    private Date nextDate;
    private int waterInterval;

    public static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance();

    public PlantSchedule(String name, Date nextDate, int waterInterval) {
        this.name = name;
        this.nextDate = nextDate;
        this.nextDate.setHours(6);
        this.nextDate.setMinutes(0);
        this.nextDate.setSeconds(0);
        this.waterInterval = waterInterval;
    }

    public String toString() {
        return "Water " + name + " "
                + (shouldWaterToday() ? "today" : "in " + getDaysToWater() + " days")
                +  ", and every "
                + waterInterval + " days";
    }

    public boolean shouldWaterToday() {
        int days = getDaysToWater();
        return days >= 0 && days % waterInterval == 0;
    }

    private int getDaysToWater() {
        long timeDiff = Calendar.getInstance().getTimeInMillis() - nextDate.getTime();
        int days = (int) Math.floor(TimeUnit.DAYS.convert(timeDiff, TimeUnit.MILLISECONDS));
        return days;
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

    public void setNextDate(Date newDate) {
        nextDate = newDate;
    }

    public Date getNextDate() {
        return nextDate;
    }

    public String getFormattedNextDate() {
        return DATE_FORMAT.format(nextDate);
    }

    public void setWaterInterval(int newInterval) {
        waterInterval = newInterval;
    }

    public int getWaterInterval() {
        return waterInterval;
    }
}
