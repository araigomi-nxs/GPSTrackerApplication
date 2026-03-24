package com.example.gpstrackerapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gpstrackerapplication.database.DatabaseHelper;
import com.example.gpstrackerapplication.integration.DiscordWebhook;
import com.google.android.gms.location.Priority;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wp, tv_discord_status, tv_time;
    Button bt_wp, bt_showp, bt_showmap, bt_close_app, bt_route_list;
    SwitchMaterial sw_locationupdates, sw_gps, sw_discord;

    Location currentLocation;
    private DiscordWebhook discordWebhook;
    private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1483553823980257330/YNA4dBeRu_Q3ILbj2-cT6ep1eq6Wi9EdRo8g7uCt2POnamQ4oRtt7Be8Q27h75ZLtNES";
    private DatabaseHelper databaseHelper;

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("LocationUpdate".equals(intent.getAction())) {
                Location location = intent.getParcelableExtra("location");
                if (location != null) {
                    updateUComponents(location);
                }
            }
        }
    };

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
        tv_time = findViewById(R.id.tv_time);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);
        sw_gps = findViewById(R.id.sw_gps);
        sw_discord = findViewById(R.id.sw_discord);
        tv_discord_status = findViewById(R.id.tv_discord_status);
        tv_wp = findViewById(R.id.tv_wp);
        bt_wp = findViewById(R.id.bt_wp);
        bt_showp = findViewById(R.id.bt_showp);
        bt_showmap = findViewById(R.id.bt_showmap);
        bt_close_app = findViewById(R.id.bt_close_app);
        bt_route_list = findViewById(R.id.bt_route_list);

        discordWebhook = new DiscordWebhook(DISCORD_WEBHOOK_URL);
        databaseHelper = new DatabaseHelper(MainActivity.this);

        bt_wp.setOnClickListener(v -> {
            if (currentLocation != null) {
                if (databaseHelper.addOne(currentLocation)) {
                    Toast.makeText(MainActivity.this, "Location saved", Toast.LENGTH_SHORT).show();
                    tv_wp.setText(String.valueOf(databaseHelper.getCount()));
                }
            } else {
                Toast.makeText(MainActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        });

        bt_showp.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SavedLocationList.class)));
        bt_showmap.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MapsActivity.class)));
        bt_route_list.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RouteListActivity.class)));

        bt_close_app.setOnClickListener(v -> {
            stopLocationService();
            finishAffinity();
        });

        sw_gps.setOnClickListener(v -> {
            int priority = sw_gps.isChecked() ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
            tv_sensor.setText(sw_gps.isChecked() ? "Using GPS" : "Using Towers + Wifi");
            
            Intent intent = new Intent(this, LocationService.class);
            intent.putExtra("priority", priority);
            if (sw_locationupdates.isChecked()) {
                startForegroundService(intent);
            }
        });

        sw_locationupdates.setOnClickListener(v -> {
            if (sw_locationupdates.isChecked()) {
                checkPermissionsAndStartService();
            } else {
                stopLocationService();
            }
        });

        sw_discord.setOnClickListener(v -> {
            boolean isChecked = sw_discord.isChecked();
            tv_discord_status.setText(isChecked ? "On" : "Off");
            
            Intent intent = new Intent(this, LocationService.class);
            intent.putExtra("discord_enabled", isChecked);
            if (sw_locationupdates.isChecked()) {
                startForegroundService(intent);
            }

            if (isChecked) {
                discordWebhook.sendMessage("🟢 **Discord Integration Enabled**");
            } else {
                discordWebhook.sendMessage("🔴 **Discord Integration Disabled**");
            }
        });

        tv_wp.setText(String.valueOf(databaseHelper.getCount()));
    }

    private void checkPermissionsAndStartService() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startLocationService();
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void startLocationService() {
        tv_updates.setText("On");
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra("discord_enabled", sw_discord.isChecked());
        intent.putExtra("priority", sw_gps.isChecked() ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY);
        startForegroundService(intent);
    }

    private void stopLocationService() {
        tv_updates.setText("Off");
        stopService(new Intent(this, LocationService.class));
        resetUI();
    }

    private void resetUI() {
        tv_lat.setText("NTL");
        tv_lon.setText("NTL");
        tv_altitude.setText("NTL");
        tv_accuracy.setText("NTL");
        tv_speed.setText("NTL");
        tv_address.setText("NTL");
        tv_time.setText("—:—");
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("LocationUpdate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(locationReceiver, filter);
        }
        tv_wp.setText(String.valueOf(databaseHelper.getCount()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                sw_locationupdates.setChecked(false);
                Toast.makeText(this, "Permission required for tracking", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUComponents(Location location) {
        currentLocation = location;
        tv_lat.setText(String.format(Locale.getDefault(), "%.6f", location.getLatitude()));
        tv_lon.setText(String.format(Locale.getDefault(), "%.6f", location.getLongitude()));
        tv_altitude.setText(location.hasAltitude() ? String.format(Locale.getDefault(), "%.2f", location.getAltitude()) : "N/A");
        tv_speed.setText(location.hasSpeed() ? String.format(Locale.getDefault(), "%.2f", location.getSpeed()) : "N/A");
        tv_accuracy.setText(String.format(Locale.getDefault(), "%.2f", location.getAccuracy()));

        // Geocoding in background
        new Thread(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String addr = addresses.get(0).getAddressLine(0);
                    runOnUiThread(() -> tv_address.setText(addr));
                }
            } catch (Exception ignored) {}
        }).start();

        tv_time.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(location.getTime())));
    }
}
