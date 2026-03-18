package com.example.gpstrackerapplication;

import androidx.fragment.app.FragmentActivity;

import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.example.gpstrackerapplication.models.Waypoint;

import com.example.gpstrackerapplication.database.DatabaseHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.gpstrackerapplication.databinding.ActivityMapsBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private DatabaseHelper databaseHelper;

    private FloatingActionButton fab_back;
    private TextView tv_wp_count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);
        fab_back = findViewById(R.id.fab_back);
        tv_wp_count = findViewById(R.id.tv_wp_count_map);


        tv_wp_count.setText(String.valueOf(databaseHelper.getCount()));

        if (fab_back != null) {
            fab_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Apply Dark Mode Map Style
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style_dark));

            if (!success) {
                Log.e("MapsActivity", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapsActivity", "Can't find style. Error: ", e);
        }

        // Pull waypoints from database
        List<Waypoint> savedWaypoints = databaseHelper.getAll();
        LatLng lastLocationPlaced = null;

        if (tv_wp_count != null) {
            tv_wp_count.setText(String.valueOf(savedWaypoints.size()));
        }

        for (Waypoint waypoint : savedWaypoints) {
            Location location = waypoint.getLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Saved Waypoint")
                    .snippet(location.getLatitude() + ", " + location.getLongitude()));
            lastLocationPlaced = latLng;
        }

        if (lastLocationPlaced != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocationPlaced, 12.0f));
        }
    }
}
