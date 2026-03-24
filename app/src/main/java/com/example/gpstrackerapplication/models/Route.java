package com.example.gpstrackerapplication.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Route {
    private int id;
    private String name;
    private long timestamp;

    public Route(int id, String name, long timestamp) {
        this.id = id;
        this.name = name;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
