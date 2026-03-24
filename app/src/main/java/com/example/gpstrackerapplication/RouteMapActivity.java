package com.example.gpstrackerapplication;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.example.gpstrackerapplication.database.DatabaseHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RouteMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private int routeId;
    private String routeName;
    private DatabaseHelper databaseHelper;
    private final List<Polyline> polylines = new ArrayList<>();
    private final Map<String, Float> segmentRoadDistanceMeters = new HashMap<>();
    private List<DatabaseHelper.RouteWaypoint> routeWaypoints = new ArrayList<>();
    private int drawGeneration = 0;
    private boolean hasShownRoadRoutingError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);

        routeId = getIntent().getIntExtra("route_id", -1);
        routeName = getIntent().getStringExtra("route_name");
        databaseHelper = new DatabaseHelper(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);

        drawRoute(true);
    }

    private void drawRoute(boolean zoom) {
        drawGeneration++;
        int generation = drawGeneration;
        mMap.clear();
        for (Polyline p : polylines) p.remove();
        polylines.clear();
        segmentRoadDistanceMeters.clear();
        hasShownRoadRoutingError = false;
        
        routeWaypoints = databaseHelper.getRouteWaypoints(routeId);

        if (routeWaypoints.isEmpty()) {
            Toast.makeText(this, "No location data for this route. Long-press to add waypoints.", Toast.LENGTH_LONG).show();
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < routeWaypoints.size(); i++) {
            DatabaseHelper.RouteWaypoint rwp = routeWaypoints.get(i);
            LatLng latLng = new LatLng(rwp.location.getLatitude(), rwp.location.getLongitude());
            builder.include(latLng);

            MarkerOptions markerOptions = new MarkerOptions().position(latLng);
            String title = (rwp.name != null && !rwp.name.isEmpty()) ? rwp.name : "Waypoint " + (i + 1);
            
            if (i == 0) {
                markerOptions.title("Start: " + title)
                             .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            } else if (i == routeWaypoints.size() - 1) {
                markerOptions.title("End: " + title)
                             .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            } else {
                markerOptions.title(title)
                             .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            }
            
            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                marker.setTag(rwp);
            }

            // Draw line to previous point
            if (i > 0) {
                DatabaseHelper.RouteWaypoint prevWaypoint = routeWaypoints.get(i - 1);
                LatLng prevLatLng = new LatLng(prevWaypoint.location.getLatitude(), prevWaypoint.location.getLongitude());
                fetchRoadDirections(prevLatLng, latLng, prevWaypoint.id, rwp.id, generation);
            }
        }

        if (zoom) {
            mMap.setOnMapLoadedCallback(() -> {
                try {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                } catch (Exception ignored) {
                    if (!routeWaypoints.isEmpty()) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(routeWaypoints.get(0).location.getLatitude(), routeWaypoints.get(0).location.getLongitude()), 15f));
                    }
                }
            });
        }
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        new AlertDialog.Builder(this)
                .setTitle("Add Waypoint")
                .setMessage("Do you want to add a waypoint at this location?")
                .setPositiveButton("Add", (dialog, which) -> {
                    Location l = new Location("manual");
                    l.setLatitude(latLng.latitude);
                    l.setLongitude(latLng.longitude);
                    l.setTime(System.currentTimeMillis());
                    databaseHelper.addWaypointToRoute(routeId, l);
                    drawRoute(false);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof DatabaseHelper.RouteWaypoint) {
            showEditWaypointDialog((DatabaseHelper.RouteWaypoint) tag);
            return true;
        }
        return false;
    }

    private void showEditWaypointDialog(DatabaseHelper.RouteWaypoint rwp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_waypoint, null);
        
        EditText etName = view.findViewById(R.id.et_waypoint_name);
        TextView tvDistancePrev = view.findViewById(R.id.tv_distance_prev);
        TextView tvDistanceNext = view.findViewById(R.id.tv_distance_next);
        
        etName.setText(rwp.name != null ? rwp.name : "");
        
        // Find distances
        int index = -1;
        for(int i=0; i<routeWaypoints.size(); i++) {
            if(routeWaypoints.get(i).id == rwp.id) {
                index = i;
                break;
            }
        }
        
        if (index > 0) {
            DatabaseHelper.RouteWaypoint prev = routeWaypoints.get(index - 1);
            Float roadDistance = segmentRoadDistanceMeters.get(getSegmentKey(prev.id, rwp.id));
            if (roadDistance != null) {
                tvDistancePrev.setText("From previous: " + formatDistance(roadDistance));
            } else {
                float directDistance = rwp.location.distanceTo(prev.location);
                tvDistancePrev.setText("From previous: " + formatDistance(directDistance) + " (direct)");
            }
        } else {
            tvDistancePrev.setText("Start of route");
        }
        
        if (index != -1 && index < routeWaypoints.size() - 1) {
            DatabaseHelper.RouteWaypoint next = routeWaypoints.get(index + 1);
            Float roadDistance = segmentRoadDistanceMeters.get(getSegmentKey(rwp.id, next.id));
            if (roadDistance != null) {
                tvDistanceNext.setText("To next: " + formatDistance(roadDistance));
            } else {
                float directDistance = rwp.location.distanceTo(next.location);
                tvDistanceNext.setText("To next: " + formatDistance(directDistance) + " (direct)");
            }
        } else {
            tvDistanceNext.setText("End of route");
        }

        builder.setView(view)
                .setTitle("Edit Waypoint")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    databaseHelper.updateRouteWaypointName(rwp.id, newName);
                    drawRoute(false);
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    databaseHelper.deleteRouteWaypoint(rwp.id);
                    drawRoute(false);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private String getSegmentKey(int startWaypointId, int endWaypointId) {
        return startWaypointId + "->" + endWaypointId;
    }

    private String formatDistance(float meters) {
        if (meters >= 1000f) {
            return String.format(Locale.getDefault(), "%.2f km", meters / 1000f);
        }
        return String.format(Locale.getDefault(), "%.0f m", meters);
    }

    private String getMapsApiKey() {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                String key = appInfo.metaData.getString("com.google.android.geo.API_KEY", "");
                if (!TextUtils.isEmpty(key)) {
                    return key;
                }
            }
        } catch (Exception ignored) {
            // Use empty key and fail gracefully.
        }
        return "";
    }

    private void fetchRoadDirections(LatLng origin, LatLng dest, int startWaypointId, int endWaypointId, int generation) {
        new GetDirectionsTask(origin, dest, startWaypointId, endWaypointId, generation).execute();
    }

    private class GetDirectionsTask extends AsyncTask<Void, Void, DirectionsResult> {
        private final LatLng origin;
        private final LatLng dest;
        private final int startWaypointId;
        private final int endWaypointId;
        private final int generation;

        GetDirectionsTask(LatLng origin, LatLng dest, int startWaypointId, int endWaypointId, int generation) {
            this.origin = origin;
            this.dest = dest;
            this.startWaypointId = startWaypointId;
            this.endWaypointId = endWaypointId;
            this.generation = generation;
        }

        @Override
        protected DirectionsResult doInBackground(Void... voids) {
            try {
                String apiKey = getMapsApiKey();
                if (TextUtils.isEmpty(apiKey)) {
                    return null;
                }

                String urlStr = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                        origin.latitude + "," + origin.longitude +
                        "&destination=" + dest.latitude + "," + dest.longitude +
                        "&mode=driving" +
                        "&alternatives=false" +
                        "&key=" + apiKey;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                
                JSONObject json = new JSONObject(sb.toString());
                String status = json.optString("status", "");
                if (!"OK".equals(status)) {
                    return null;
                }

                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    String encodedPoints = route.getJSONObject("overview_polyline").getString("points");
                    List<LatLng> decodedPoints = decodePoly(encodedPoints);

                    float meters = 0f;
                    JSONArray legs = route.optJSONArray("legs");
                    if (legs != null && legs.length() > 0) {
                        JSONObject firstLeg = legs.getJSONObject(0);
                        JSONObject distance = firstLeg.optJSONObject("distance");
                        if (distance != null) {
                            meters = (float) distance.optDouble("value", 0d);
                        }
                    }
                    return new DirectionsResult(decodedPoints, meters);
                }
            } catch (Exception e) {
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(DirectionsResult result) {
            if (generation != drawGeneration || mMap == null) {
                return;
            }

            if (result != null && result.points != null && !result.points.isEmpty()) {
                PolylineOptions options = new PolylineOptions()
                        .width(10)
                        .color(Color.BLUE)
                        .geodesic(false)
                        .addAll(result.points);
                polylines.add(mMap.addPolyline(options));

                if (result.distanceMeters > 0f) {
                    segmentRoadDistanceMeters.put(getSegmentKey(startWaypointId, endWaypointId), result.distanceMeters);
                }
            } else if (!hasShownRoadRoutingError) {
                hasShownRoadRoutingError = true;
                Toast.makeText(RouteMapActivity.this, "Unable to load road path for one or more segments.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static class DirectionsResult {
        final List<LatLng> points;
        final float distanceMeters;

        DirectionsResult(List<LatLng> points, float distanceMeters) {
            this.points = points;
            this.distanceMeters = distanceMeters;
        }
    }

    // Helper to decode overview_polyline
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}
