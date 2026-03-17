package com.nuramin.sunsetcoralcalculator.ai.cloud;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Google Gemini API (free tier). Uses generateContent REST API.
 * Get key at https://aistudio.google.com/apikey
 */
public final class GeminiService {

    private static final String TAG = "GeminiService";
    private static final String MODEL = "gemini-1.5-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    public interface Callback {
        void onSuccess(@NonNull String content);
        void onError(@NonNull String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void chat(@NonNull String userInput, @Nullable String apiKey, @NonNull Callback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("API key not configured");
            return;
        }
        executor.execute(() -> {
            try {
                String response = doPost(userInput, apiKey.trim());
                if (response != null) {
                    String content = parseContent(response);
                    if (content != null) {
                        callback.onSuccess(content);
                    } else {
                        callback.onError("Could not parse response");
                    }
                } else {
                    callback.onError("Request failed");
                }
            } catch (Exception e) {
                Log.e(TAG, "chat", e);
                callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
            }
        });
    }

    private String doPost(String userInput, String apiKey) throws Exception {
        HttpURLConnection conn = null;
        try {
            String urlStr = BASE_URL + "?key=" + apiKey.replace(" ", "");
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            String body = buildBody(userInput);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                String err = readStream(conn.getErrorStream());
                Log.e(TAG, "HTTP " + code + " " + err);
                String msg = code == 400 ? "Bad request. Check your input."
                        : code == 403 ? "Invalid API key. Get a free key at aistudio.google.com/apikey"
                        : code == 429 ? "Too many requests. Try again in a minute."
                        : "API error: " + code + (err != null && !err.isEmpty() ? " — " + extractShortError(err) : "");
                throw new RuntimeException(msg);
            }
            return readStream(conn.getInputStream());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String buildBody(String userInput) throws Exception {
        JSONObject part = new JSONObject();
        part.put("text", userInput);
        JSONArray parts = new JSONArray();
        parts.put(part);
        JSONObject content = new JSONObject();
        content.put("parts", parts);
        JSONArray contents = new JSONArray();
        contents.put(content);
        JSONObject root = new JSONObject();
        root.put("contents", contents);
        root.put("generationConfig", new JSONObject()
                .put("maxOutputTokens", 1024)
                .put("temperature", 0.7f));
        return root.toString();
    }

    @Nullable
    private static String parseContent(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return null;
            JSONObject first = candidates.getJSONObject(0);
            JSONObject content = first.optJSONObject("content");
            if (content == null) return null;
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null || parts.length() == 0) return null;
            String text = parts.getJSONObject(0).optString("text", "").trim();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            Log.e(TAG, "parseContent", e);
            return null;
        }
    }

    private static String extractShortError(String err) {
        if (err == null || err.isEmpty()) return "";
        try {
            JSONObject root = new JSONObject(err.trim());
            String message = root.optString("message", "").trim();
            if (!message.isEmpty()) return message;
        } catch (Exception ignored) { }
        return err.length() > 120 ? err.substring(0, 117).trim() + "…" : err.trim();
    }

    private static String readStream(java.io.InputStream is) throws Exception {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
