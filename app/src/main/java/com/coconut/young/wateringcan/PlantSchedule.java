package com.coconut.young.wateringcan;

import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by grant on 4/17/17.
 */
public class PlantSchedule {

    private String name;
    private Date nextDate;
    private int waterInterval;

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");

    public PlantSchedule(String name, Date nextDate, int waterInterval) {
        this.name = name;
        this.nextDate = nextDate;
        this.nextDate.setHours(6);
        this.nextDate.setMinutes(0);
        this.nextDate.setSeconds(0);
        this.waterInterval = waterInterval;
    }

    public String toString() {
        return "Water " + name + (shouldWaterToday() ? " today! " : " on ")
                + DATE_FORMAT.format(nextDate)
                + ", every " + waterInterval + " days";
    }

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

    public boolean shouldWaterToday() {
        long timeDiff = Calendar.getInstance().getTimeInMillis() - nextDate.getTime();
        int days = (int) Math.floor(TimeUnit.DAYS.convert(timeDiff, TimeUnit.MILLISECONDS));
        return days >= 0 && days % waterInterval == 0;
    }

}
