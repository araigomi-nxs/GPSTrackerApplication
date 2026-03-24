package com.example.gpstrackerapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gpstrackerapplication.database.DatabaseHelper;
import com.example.gpstrackerapplication.models.Route;
import com.google.android.gms.location.Priority;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RouteListActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 101;

    private ListView lvRoutes;
    private DatabaseHelper databaseHelper;
    private RouteAdapter adapter;
    private Button btRecordRoute;
    private boolean isRecording = false;

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("LocationUpdate".equals(intent.getAction())) {
                isRecording = intent.getBooleanExtra("is_recording", false);
                updateRecordButtonUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list);

        lvRoutes = findViewById(R.id.lv_routes);
        btRecordRoute = findViewById(R.id.bt_record_route);
        databaseHelper = new DatabaseHelper(this);

        loadRoutes();

        btRecordRoute.setOnClickListener(v -> {
            if (!isRecording) {
                showRouteNameDialog();
            } else {
                stopRouteRecording();
            }
        });
    }

    private void loadRoutes() {
        List<Route> routes = databaseHelper.getAllRoutes();
        adapter = new RouteAdapter(this, routes, this::loadRoutes);
        lvRoutes.setAdapter(adapter);
    }

    private void showRouteNameDialog() {
        EditText etName = new EditText(this);
        etName.setHint("Route Name");
        new AlertDialog.Builder(this)
                .setTitle("Record Route")
                .setMessage("Enter a name for this route:")
                .setView(etName)
                .setPositiveButton("Start", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) name = "Route " + new SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()).format(new Date());
                    startRouteRecording(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startRouteRecording(String name) {
        checkPermissionsAndStartService(name);
    }

    private void stopRouteRecording() {
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra("stop_route", true);
        startForegroundService(intent);
        isRecording = false;
        updateRecordButtonUI();
        loadRoutes(); // Refresh list to show new route (though it might be empty/short)
        Toast.makeText(this, "Route recording stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateRecordButtonUI() {
        if (isRecording) {
            btRecordRoute.setText("Stop Recording Route");
            btRecordRoute.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else {
            btRecordRoute.setText("Start Recording Route");
            btRecordRoute.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }
    }

    private void checkPermissionsAndStartService(String routeName) {
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
            sendStartCommand(routeName);
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void sendStartCommand(String routeName) {
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra("start_route", routeName);
        intent.putExtra("priority", Priority.PRIORITY_HIGH_ACCURACY);
        startForegroundService(intent);
        isRecording = true;
        updateRecordButtonUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("LocationUpdate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(serviceReceiver, filter);
        }
        loadRoutes();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(serviceReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showRouteNameDialog();
            } else {
                Toast.makeText(this, "Permission required for tracking", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
