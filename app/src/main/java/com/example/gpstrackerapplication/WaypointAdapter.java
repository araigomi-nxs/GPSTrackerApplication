package com.example.gpstrackerapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gpstrackerapplication.database.DatabaseHelper;
import com.example.gpstrackerapplication.models.Waypoint;

import java.util.List;
import java.util.Locale;

public class WaypointAdapter extends ArrayAdapter<Waypoint> {

    private DatabaseHelper databaseHelper;

    public WaypointAdapter(@NonNull Context context, @NonNull List<Waypoint> objects) {
        super(context, 0, objects);
        databaseHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_waypoint, parent, false);
        }

        Waypoint waypoint = getItem(position);

        TextView tvIndex = convertView.findViewById(R.id.tv_index);
        TextView tvCoords = convertView.findViewById(R.id.tv_wp_coords);
        ImageButton btnDelete = convertView.findViewById(R.id.btn_delete_waypoint);

        if (waypoint != null) {
            tvIndex.setText(String.valueOf(position + 1));
            String coords = String.format(Locale.getDefault(), "%.6f° N, %.6f° E", 
                    waypoint.getLocation().getLatitude(), waypoint.getLocation().getLongitude());
            tvCoords.setText(coords);

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean success = databaseHelper.deleteOneById(waypoint.getId());
                    if (success) {
                        remove(waypoint);
                        notifyDataSetChanged();
                        Toast.makeText(getContext(), "Waypoint deleted", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        return convertView;
    }
}
