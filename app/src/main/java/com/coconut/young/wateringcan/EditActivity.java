package com.coconut.young.wateringcan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * The Activity used to add or edit a PlantSchedule
 */
public class EditActivity extends Activity {

    public static final String TAG = "EditWateringCan";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        final Intent intent = getIntent();

        // Fill the fields with passed data
        final EditText nameInput = (EditText) findViewById(R.id.nameInput);
        setText(nameInput, intent.getStringExtra("name"));

        final EditText dateInput = (EditText) findViewById(R.id.dateInput);
        setText(dateInput, intent.getStringExtra("date"));

        final EditText intervalInput = (EditText) findViewById(R.id.intervalInput);
        int passedInterval = intent.getIntExtra("interval", -1);
        setText(intervalInput,  passedInterval == -1 ? "" : Integer.toString(passedInterval));

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String name = nameInput.getText().toString();
                String nextDate = dateInput.getText().toString();
                int waterInterval = Integer.valueOf(intervalInput.getText().toString());

                Intent returnIntent = new Intent();
                int update = intent.getIntExtra("update", -1);
                if (update != -1) {
                    returnIntent.putExtra("update", update);
                }

                returnIntent.putExtra("name", name);
                returnIntent.putExtra("date", nextDate);
                returnIntent.putExtra("interval", waterInterval);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
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

    private void setText(EditText field, String text) {
        if (text == null)
            text = "";
        field.setText(text);
    }
}
