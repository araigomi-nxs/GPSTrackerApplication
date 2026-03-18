package com.example.gpstrackerapplication;

import android.os.Bundle;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gpstrackerapplication.database.DatabaseHelper;
import com.example.gpstrackerapplication.models.Waypoint;

import java.util.List;

public class SavedLocationList extends AppCompatActivity {
    ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_saved_location_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Pull waypoints from database
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        List<Waypoint> savedWaypoints = databaseHelper.getAll();

        listView = findViewById(R.id.lv_wp);

        WaypointAdapter adapter = new WaypointAdapter(this, savedWaypoints);
        listView.setAdapter(adapter);

    }
}
