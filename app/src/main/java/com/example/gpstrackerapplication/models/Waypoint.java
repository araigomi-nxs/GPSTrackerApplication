package com.example.gpstrackerapplication.models;

import android.location.Location;

public class Waypoint {
    private int id;
    private Location location;
    private long time;

    public Waypoint(int id, Location location, long time) {
        this.id = id;
        this.location = location;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
