package com.example.gpstrackerapplication;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

public class WaypointAdapter extends ArrayAdapter<Location> {

    public WaypointAdapter(@NonNull Context context, @NonNull List<Location> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_waypoint, parent, false);
        }

        Location location = getItem(position);

        TextView tvIndex = convertView.findViewById(R.id.tv_index);
        TextView tvCoords = convertView.findViewById(R.id.tv_wp_coords);

        if (location != null) {
            tvIndex.setText(String.valueOf(position + 1));
            String coords = String.format(Locale.getDefault(), "%.6f° N, %.6f° E", 
                    location.getLatitude(), location.getLongitude());
            tvCoords.setText(coords);
        }

        return convertView;
    }
}
