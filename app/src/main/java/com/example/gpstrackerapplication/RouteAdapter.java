package com.example.gpstrackerapplication;

import android.content.Context;
import android.content.Intent;
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
import com.example.gpstrackerapplication.models.Route;

import java.util.List;

public class RouteAdapter extends ArrayAdapter<Route> {

    private DatabaseHelper databaseHelper;
    private OnRouteDeleteListener deleteListener;

    public interface OnRouteDeleteListener {
        void onRouteDeleted();
    }

    public RouteAdapter(@NonNull Context context, @NonNull List<Route> objects, OnRouteDeleteListener listener) {
        super(context, 0, objects);
        databaseHelper = new DatabaseHelper(context);
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_route, parent, false);
        }

        Route route = getItem(position);

        TextView tvName = convertView.findViewById(R.id.tv_route_name);
        TextView tvDate = convertView.findViewById(R.id.tv_route_date);
        ImageButton btnDelete = convertView.findViewById(R.id.btn_delete_route);

        if (route != null) {
            tvName.setText(route.getName());
            tvDate.setText(route.getFormattedDate());

            // Handle the click on the entire item here for better reliability
            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), RouteMapActivity.class);
                intent.putExtra("route_id", route.getId());
                intent.putExtra("route_name", route.getName());
                getContext().startActivity(intent);
            });

            btnDelete.setOnClickListener(v -> {
                if (databaseHelper.deleteRoute(route.getId())) {
                    remove(route);
                    notifyDataSetChanged();
                    Toast.makeText(getContext(), "Route deleted", Toast.LENGTH_SHORT).show();
                    if (deleteListener != null) deleteListener.onRouteDeleted();
                }
            });
        }

        return convertView;
    }
}
