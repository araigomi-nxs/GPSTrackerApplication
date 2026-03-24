package com.example.gpstrackerapplication;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.gpstrackerapplication.database.DatabaseHelper;
import com.example.gpstrackerapplication.integration.DiscordWebhook;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

public class LocationService extends Service {

    private final IBinder binder = new LocalBinder();
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    
    private DiscordWebhook discordWebhook;
    private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1483553823980257330/YNA4dBeRu_Q3ILbj2-cT6ep1eq6Wi9EdRo8g7uCt2POnamQ4oRtt7Be8Q27h75ZLtNES";
    private boolean discordEnabled = false;
    private int currentPriority = Priority.PRIORITY_BALANCED_POWER_ACCURACY;

    private DatabaseHelper databaseHelper;
    private long currentRouteId = -1;
    private long lastRouteWaypointTime = 0;
    private static final long FIVE_MINUTES = 5 * 60 * 1000;

    public class LocalBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        discordWebhook = new DiscordWebhook(DISCORD_WEBHOOK_URL);
        databaseHelper = new DatabaseHelper(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    broadcastLocation(location);
                    if (discordEnabled) {
                        sendToDiscord(location);
                    }
                    
                    if (currentRouteId != -1) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastRouteWaypointTime >= FIVE_MINUTES) {
                            databaseHelper.addWaypointToRoute(currentRouteId, location);
                            lastRouteWaypointTime = currentTime;
                        }
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            discordEnabled = intent.getBooleanExtra("discord_enabled", discordEnabled);
            int newPriority = intent.getIntExtra("priority", currentPriority);
            
            if (intent.hasExtra("start_route")) {
                String routeName = intent.getStringExtra("start_route");
                currentRouteId = databaseHelper.startRoute(routeName);
                lastRouteWaypointTime = 0; // Record immediately
            } else if (intent.hasExtra("stop_route")) {
                currentRouteId = -1;
            }

            if (locationRequest == null || newPriority != currentPriority) {
                currentPriority = newPriority;
                stopLocationUpdates();
                locationRequest = new LocationRequest.Builder(currentPriority, 5000L)
                        .setMinUpdateIntervalMillis(2000L)
                        .build();
                startLocationUpdates();
            }
        }

        startForeground(1, getNotification());
        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    private void broadcastLocation(Location location) {
        Intent intent = new Intent("LocationUpdate");
        intent.setPackage(getPackageName());
        intent.putExtra("location", location);
        intent.putExtra("is_recording", currentRouteId != -1);
        sendBroadcast(intent);
    }

    private void sendToDiscord(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            String address = (addresses != null && !addresses.isEmpty()) ? addresses.get(0).getAddressLine(0) : "N/A";
            discordWebhook.sendLocation(location.getLatitude(), location.getLongitude(), address);
        } catch (Exception e) {
            discordWebhook.sendLocation(location.getLatitude(), location.getLongitude(), "N/A");
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String contentText = currentRouteId != -1 ? "Recording route..." : "Tracking location in background";

        return new NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
                .setContentTitle("GPS Tracker")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.gpslogo)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
}
