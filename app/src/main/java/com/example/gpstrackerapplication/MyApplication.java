package com.example.gpstrackerapplication;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.location.Location;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    public static final String CHANNEL_ID = "location_service_channel";
    private static MyApplication singleton;
    private List<Location> myLocations;

    public List<Location> getMyLocations() {
        return myLocations;
    }

    public void setMyLocations(List<Location> myLocations) {
        this.myLocations = myLocations;
    }

    public static MyApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        myLocations = new ArrayList<>();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
