package com.example.gpstrackerapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_FINE_LOCATION = 99;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wp;
    Button bt_wp, bt_showp, bt_showmap;
    SwitchMaterial sw_locationupdates, sw_gps;

    private static final int DEFAULT_UPDATE_INTERVAL = 5;
    private static final int FASTEST_UPDATE_INTERVAL = 2;


    Location currentLocation;
    List<Location> savedLocations;

    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;

    private DiscordWebhook discordWebhook;
    private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1483553823980257330/YNA4dBeRu_Q3ILbj2-cT6ep1eq6Wi9EdRo8g7uCt2POnamQ4oRtt7Be8Q27h75ZLtNES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);
        sw_gps = findViewById(R.id.sw_gps);

        tv_wp = findViewById(R.id.tv_wp);
        bt_wp = findViewById(R.id.bt_wp);
        bt_showp = findViewById(R.id.bt_showp);
        bt_showmap = findViewById(R.id.bt_showmap);

        discordWebhook = new DiscordWebhook(DISCORD_WEBHOOK_URL);

        locationRequest = buildLocationRequest(Priority.PRIORITY_BALANCED_POWER_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateUComponents(location);
                    
                    discordWebhook.sendLocation(
                            location.getLatitude(),
                            location.getLongitude(),
                            tv_address.getText().toString()
                    );
                }
            }
        };

        bt_wp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyApplication myApplication = (MyApplication) getApplicationContext();
                savedLocations = myApplication.getMyLocations();
                if (currentLocation != null) {
                    savedLocations.add(currentLocation);
                    updateUComponents(currentLocation);
                } else {
                    Toast.makeText(MainActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        bt_showp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SavedLocationList.class);
                startActivity(intent);
            }
        });

        bt_showmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    locationRequest = buildLocationRequest(Priority.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS");
                } else {
                    locationRequest = buildLocationRequest(Priority.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + Wifi");
                }
                
                if (sw_locationupdates.isChecked()) {
                    stopLocationUpdates();
                    startLocationUpdates();
                }
            }
        });

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_locationupdates.isChecked()) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });

        updateGPS();
    }

    private void stopLocationUpdates() {
        tv_updates.setText("Off");
        tv_lat.setText("NTL");
        tv_lon.setText("NTL");
        tv_altitude.setText("NTL");
        tv_accuracy.setText("NTL");
        tv_speed.setText("NTL");
        tv_address.setText("NTL");
        tv_sensor.setText("NTL");

        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    private void startLocationUpdates() {
        tv_updates.setText("On");
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest, locationCallback, getMainLooper()
            );
        }
        updateGPS();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    updateGPS();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        updateUComponents(location);
                        currentLocation = location;
                    } else {
                        tv_lat.setText("Location not available");
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_FINE_LOCATION
            );
        }
    }

    private LocationRequest buildLocationRequest(int priority) {
        return new LocationRequest.Builder(priority, 1000L * DEFAULT_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(1000L * FASTEST_UPDATE_INTERVAL)
                .build();
    }

    private void updateUComponents(Location location) {
        currentLocation = location;
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));

        if (location.hasAltitude()) {
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        } else {
            tv_altitude.setText("N/A");
        }
        if (location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getSpeed()));
        } else {
            tv_speed.setText("N/A");
        }

        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                tv_address.setText(addresses.get(0).getAddressLine(0));
            }
        } catch (Exception e) {
            tv_address.setText("N/A");
        }

        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocations();
        tv_wp.setText(String.valueOf(savedLocations.size()));
    }
}
