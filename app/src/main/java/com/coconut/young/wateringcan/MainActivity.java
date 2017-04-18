package com.coconut.young.wateringcan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/*
 * TODO: Add delete button
 * TODO: Add app description here
 * TODO: Improve look of app, specifically alignment and placement of fields on different devices
 * TODO: add "Watered today" toggle to list entries
 * TODO: Update README.md
 */

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "WateringCan";

    private List<PlantSchedule> scheduleList;
    private ArrayAdapter<PlantSchedule> adapter;

    private SharedPreferences sharedPref;
    // these strings are used while saving the list of PlantSchedules as a string
    private static final String SCHEDULE_SEPARATOR = "%NEWSCHEDULE%";
    private static final String PART_SEPARATOR = "%PART%";
    private static final String PERSISTENT_SCHEDULES = "savedSchedules";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getPreferences(Context.MODE_PRIVATE);

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

        scheduleList = loadScheduleList();

        ListView listView = (ListView) findViewById(android.R.id.list);

        adapter = new ArrayAdapter<>(MainActivity.this,
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
                myIntent.putExtra("date", PlantSchedule.DATE_FORMAT.format(item.getNextDate()));
                myIntent.putExtra("interval", item.getWaterInterval());
                startActivityForResult(myIntent, 1);
            }
        });
        adapter.notifyDataSetChanged();

    }

    // this method is called when returning from the EditActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                String name = data.getStringExtra("name");
                Date nextDate = null;
                try {
                    nextDate = PlantSchedule.DATE_FORMAT.parse(data.getStringExtra("date"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                int waterInterval = data.getIntExtra("interval", -1);
                int update = data.getIntExtra("update", -1);

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
    private void saveScheduleList() {

        String listAsString = "";

        for (PlantSchedule sched : scheduleList) {
            listAsString += sched.getName() + PART_SEPARATOR
                    + PlantSchedule.DATE_FORMAT.format(sched.getNextDate()) + PART_SEPARATOR
                    + sched.getWaterInterval() + SCHEDULE_SEPARATOR;
        }

        Log.i(TAG, "Saving " + listAsString);

        sharedPref.edit().putString(PERSISTENT_SCHEDULES, listAsString).apply();
    }

    // load the string from persistent storage and convert it to a list
    private List<PlantSchedule> loadScheduleList() {

        List<PlantSchedule> list = new ArrayList<>();
        String unformattedList = sharedPref.getString(PERSISTENT_SCHEDULES, "");

        Log.i(TAG, "Loaded " + unformattedList);

        if (!unformattedList.equals("")) {
            for (String date : unformattedList.split(SCHEDULE_SEPARATOR)) {
                String[] parts= date.split(PART_SEPARATOR);
                try {
                    list.add(new PlantSchedule(parts[0], PlantSchedule.DATE_FORMAT.parse(parts[1]), Integer.valueOf(parts[2])));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;
    }

}
