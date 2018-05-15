package com.coconut.young.wateringcan;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * This custom Adapter is used to build list entries representing a PlantSchedule
 * Each view contains an icon indicating plantSchedule.waterToday and plantSchedule.toString()
 */
public class PlantScheduleAdapter extends ArrayAdapter<PlantSchedule> {
    /*Package-Private*/ PlantScheduleAdapter(Context context, List<PlantSchedule> list) {
        super(context, android.R.layout.simple_list_item_1, list);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater inflater;
            inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.schedule_list_item, new LinearLayout(getContext()));
        }

        final PlantSchedule sched = getItem(position);
        assert sched != null;

        // set the icon
        final ImageView imageView = view.findViewById(R.id.schedule_icon);
        setIcon(sched, imageView);
        // the user can toggle the boolean to indicate that they've watered the plant
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sched.setWaterToday(!sched.getWaterToday());
                setIcon(sched, imageView);
            }
        });

        // set the text
        TextView textView = view.findViewById(R.id.schedule_text);
        textView.setText(sched.toString());

        return view;
    }

    // replaces the image of an ImageView with an icon showing if the plant should be watered
    private void setIcon(PlantSchedule sched, ImageView imageView) {
        if (sched.getWaterToday()) {
            imageView.setImageResource(R.drawable.wateringcan_active2);
        } else {
            imageView.setImageResource(R.drawable.wateringcan2);
        }
    }

}
