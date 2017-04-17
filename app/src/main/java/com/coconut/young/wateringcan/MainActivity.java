package com.coconut.young.wateringcan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "WateringCan";

    private List<PlantSchedule> scheduleList;
    private ArrayAdapter<PlantSchedule> adapter;

    private SharedPreferences sharedPref;

    private static final String SCHEDULE_SEPARATOR = "%NEWSCHEDULE%";
    private static final String PART_SEPARATOR = "%PART%";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getPreferences(Context.MODE_PRIVATE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_plant);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, EditActivity.class);
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
                myIntent.putExtra("update", posit);
                myIntent.putExtra("name", item.getName());
                myIntent.putExtra("date", PlantSchedule.DATE_FORMAT.format(item.getNextDate()));
                myIntent.putExtra("interval", item.getWaterInterval());
                startActivityForResult(myIntent, 1);
            }
        });
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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

    private void saveScheduleList() {

        String listAsString = "";

        for (PlantSchedule sched : scheduleList) {
            listAsString += sched.getName() + PART_SEPARATOR
                    + PlantSchedule.DATE_FORMAT.format(sched.getNextDate()) + PART_SEPARATOR
                    + sched.getWaterInterval() + SCHEDULE_SEPARATOR;
        }

        Log.i(TAG, "Saving " + listAsString);

        sharedPref.edit().putString("savedSchedules", listAsString).apply();
    }

    private List<PlantSchedule> loadScheduleList() {

        List<PlantSchedule> list = new ArrayList<>();

        String unformattedList = sharedPref.getString("savedSchedules", "");

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
