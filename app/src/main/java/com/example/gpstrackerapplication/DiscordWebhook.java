package com.example.gpstrackerapplication;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final String webhookUrl;


    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void sendLocation(double lat, double lon, String address) {
        new SendWebhookTask().execute(lat, lon, address);
    }

    private class SendWebhookTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            try {
                double lat = (double) params[0];
                double lon = (double) params[1];
                String address = (String) params[2];

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String message = "📍 **New Location Update**\n" +
                        "**Address:** " + address + "\n" +
                        "**Coordinates:** " + lat + ", " + lon + "\n" +
                        "**Google Maps:** https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;

                JSONObject json = new JSONObject();
                json.put("content", message);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d("DiscordWebhook", "Response Code: " + responseCode);

            } catch (Exception e) {
                Log.e("DiscordWebhook", "Error sending webhook", e);
            }
            return null;
        }
    }
}
