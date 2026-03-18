package com.example.gpstrackerapplication.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import androidx.annotation.Nullable;

import com.example.gpstrackerapplication.models.Waypoint;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String LOCATION_TABLE = "LOCATION_TABLE";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_LATITUDE = "LATITUDE";
    public static final String COLUMN_LONGITUDE = "LONGITUDE";
    public static final String COLUMN_ACCURACY = "ACCURACY";

    public DatabaseHelper(@Nullable Context context) {
        super(context, "locations.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableStatement = "CREATE TABLE " + LOCATION_TABLE + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_LATITUDE + " REAL, " + COLUMN_LONGITUDE + " REAL, " + COLUMN_ACCURACY + " REAL)";
        db.execSQL(createTableStatement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public boolean addOne(Location location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_LATITUDE, location.getLatitude());
        cv.put(COLUMN_LONGITUDE, location.getLongitude());
        cv.put(COLUMN_ACCURACY, location.getAccuracy());

        long insert = db.insert(LOCATION_TABLE, null, cv);
        return insert != -1;
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

                Location newLocation = new Location("database");
                newLocation.setLatitude(lat);
                newLocation.setLongitude(lon);
                newLocation.setAccuracy(acc);

                Waypoint waypoint = new Waypoint(id, newLocation);
                returnList.add(waypoint);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }
}
