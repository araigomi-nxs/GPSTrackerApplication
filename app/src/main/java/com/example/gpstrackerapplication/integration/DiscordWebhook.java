package com.example.gpstrackerapplication.integration;

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
        String message = "📍 **New Location Update**\n" +
                "**Address:** " + address + "\n" +
                "**Coordinates:** " + lat + ", " + lon + "\n" +
                "**Google Maps:** https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
        sendMessage(message);
    }

    public void sendMessage(String message) {
        new SendWebhookTask().execute(message);
    }

    private class SendWebhookTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                String message = params[0];

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

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
