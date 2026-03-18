package com.example.gpstrackerapplication.models;

import android.location.Location;

public class Waypoint {
    private int id;
    private Location location;

    public Waypoint(int id, Location location) {
        this.id = id;
        this.location = location;
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
}
