package com.example.gpstrackerapplication.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import androidx.annotation.Nullable;

import com.example.gpstrackerapplication.models.Route;
import com.example.gpstrackerapplication.models.Waypoint;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String LOCATION_TABLE = "LOCATION_TABLE";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_LATITUDE = "LATITUDE";
    public static final String COLUMN_LONGITUDE = "LONGITUDE";
    public static final String COLUMN_ACCURACY = "ACCURACY";
    public static final String COLUMN_TIME = "TIME";

    // Route Tables
    public static final String ROUTE_TABLE = "ROUTE_TABLE";
    public static final String COLUMN_ROUTE_NAME = "NAME";
    public static final String COLUMN_ROUTE_TIMESTAMP = "TIMESTAMP";

    public static final String ROUTE_WAYPOINT_TABLE = "ROUTE_WAYPOINT_TABLE";
    public static final String COLUMN_ROUTE_ID = "ROUTE_ID";
    public static final String COLUMN_WAYPOINT_NAME = "WAYPOINT_NAME";

    public DatabaseHelper(@Nullable Context context) {
        super(context, "locations.db", null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableStatement = "CREATE TABLE " + LOCATION_TABLE + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL, " +
                COLUMN_ACCURACY + " REAL, " +
                COLUMN_TIME + " INTEGER)";
        db.execSQL(createTableStatement);

        String createRouteTable = "CREATE TABLE " + ROUTE_TABLE + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ROUTE_NAME + " TEXT, " +
                COLUMN_ROUTE_TIMESTAMP + " INTEGER)";
        db.execSQL(createRouteTable);

        String createRouteWaypointTable = "CREATE TABLE " + ROUTE_WAYPOINT_TABLE + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ROUTE_ID + " INTEGER, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL, " +
                COLUMN_TIME + " INTEGER, " +
                COLUMN_WAYPOINT_NAME + " TEXT)";
        db.execSQL(createRouteWaypointTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + LOCATION_TABLE + " ADD COLUMN " + COLUMN_TIME + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            String createRouteTable = "CREATE TABLE " + ROUTE_TABLE + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ROUTE_NAME + " TEXT, " +
                    COLUMN_ROUTE_TIMESTAMP + " INTEGER)";
            db.execSQL(createRouteTable);

            String createRouteWaypointTable = "CREATE TABLE " + ROUTE_WAYPOINT_TABLE + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ROUTE_ID + " INTEGER, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_LONGITUDE + " REAL, " +
                    COLUMN_TIME + " INTEGER)";
            db.execSQL(createRouteWaypointTable);
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + ROUTE_WAYPOINT_TABLE + " ADD COLUMN " + COLUMN_WAYPOINT_NAME + " TEXT");
        }
    }

    public boolean addOne(Location location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_LATITUDE, location.getLatitude());
        cv.put(COLUMN_LONGITUDE, location.getLongitude());
        cv.put(COLUMN_ACCURACY, location.getAccuracy());
        cv.put(COLUMN_TIME, location.getTime());

        long insert = db.insert(LOCATION_TABLE, null, cv);
        return insert != -1;
    }

    public long startRoute(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ROUTE_NAME, name);
        cv.put(COLUMN_ROUTE_TIMESTAMP, System.currentTimeMillis());
        return db.insert(ROUTE_TABLE, null, cv);
    }

    public void addWaypointToRoute(long routeId, Location location) {
        addWaypointToRoute(routeId, location, null);
    }

    public void addWaypointToRoute(long routeId, Location location, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ROUTE_ID, routeId);
        cv.put(COLUMN_LATITUDE, location.getLatitude());
        cv.put(COLUMN_LONGITUDE, location.getLongitude());
        cv.put(COLUMN_TIME, location.getTime());
        cv.put(COLUMN_WAYPOINT_NAME, name);
        db.insert(ROUTE_WAYPOINT_TABLE, null, cv);
    }

    public List<Route> getAllRoutes() {
        List<Route> routes = new ArrayList<>();
        String query = "SELECT * FROM " + ROUTE_TABLE + " ORDER BY " + COLUMN_ROUTE_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                long timestamp = cursor.getLong(2);
                routes.add(new Route(id, name, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return routes;
    }

    public List<RouteWaypoint> getRouteWaypoints(int routeId) {
        List<RouteWaypoint> waypoints = new ArrayList<>();
        String query = "SELECT * FROM " + ROUTE_WAYPOINT_TABLE + " WHERE " + COLUMN_ROUTE_ID + " = " + routeId + " ORDER BY " + COLUMN_TIME + " ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                double lat = cursor.getDouble(2);
                double lon = cursor.getDouble(3);
                long time = cursor.getLong(4);
                String name = cursor.getString(5);
                
                Location l = new Location("database");
                l.setLatitude(lat);
                l.setLongitude(lon);
                l.setTime(time);
                
                waypoints.add(new RouteWaypoint(id, l, name));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return waypoints;
    }

    public boolean deleteRouteWaypoint(int waypointId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int delete = db.delete(ROUTE_WAYPOINT_TABLE, COLUMN_ID + " = " + waypointId, null);
        return delete > 0;
    }

    public boolean updateRouteWaypointName(int waypointId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_WAYPOINT_NAME, newName);
        int update = db.update(ROUTE_WAYPOINT_TABLE, cv, COLUMN_ID + " = " + waypointId, null);
        return update > 0;
    }

    public boolean deleteRoute(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(ROUTE_WAYPOINT_TABLE, COLUMN_ROUTE_ID + " = " + id, null);
        int delete = db.delete(ROUTE_TABLE, COLUMN_ID + " = " + id, null);
        return delete > 0;
    }

    public boolean deleteOneById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int delete = db.delete(LOCATION_TABLE, COLUMN_ID + " = " + id, null);
        db.close();
        return delete > 0;
    }

    public long getCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, LOCATION_TABLE);
        db.close();
        return count;
    }

    public List<Waypoint> getAll() {
        List<Waypoint> returnList = new ArrayList<>();
        String queryString = "SELECT * FROM " + LOCATION_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(queryString, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                double lat = cursor.getDouble(1);
                double lon = cursor.getDouble(2);
                float acc = cursor.getFloat(3);
                long time = cursor.getLong(4);

                Location newLocation = new Location("database");
                newLocation.setLatitude(lat);
                newLocation.setLongitude(lon);
                newLocation.setAccuracy(acc);
                newLocation.setTime(time);

                Waypoint waypoint = new Waypoint(id, newLocation, time);
                returnList.add(waypoint);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }
    
    // Helper class for waypoints inside a route
    public static class RouteWaypoint {
        public int id;
        public Location location;
        public String name;

        public RouteWaypoint(int id, Location location, String name) {
            this.id = id;
            this.location = location;
            this.name = name;
        }
    }
}
