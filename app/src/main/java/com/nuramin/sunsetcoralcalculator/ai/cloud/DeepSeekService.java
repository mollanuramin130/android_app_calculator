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
 * Handles DeepSeek API calls via HttpURLConnection.
 * POST to chat/completions; extracts choices[0].message.content.
 * Timeout and error handling; no crash on failure.
 */
public final class DeepSeekService {

    private static final String TAG = "DeepSeekService";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    public interface Callback {
        void onSuccess(@NonNull String content);
        void onError(@NonNull String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Sends user input to DeepSeek and returns the assistant reply via callback.
     * Runs on background thread; callbacks may be invoked on that thread (call runOnUiThread if updating UI).
     *
     * @param userInput the user message
     * @param apiKey    Bearer token (from BuildConfig or secure storage). If null/empty, onError is called.
     * @param callback  onSuccess with extracted content, or onError with message
     */
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
            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
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
                String msg = code == 401 ? "Invalid API key. Check key in menu > API key."
                        : code == 429 ? "Too many requests. Try again later."
                        : code == 402 || (err != null && err.contains("Insufficient Balance"))
                        ? "Your DeepSeek account has insufficient balance. Add credits at platform.deepseek.com"
                        : "API error: " + code + (err != null && !err.isEmpty() ? " — " + extractShortError(err) : "");
                throw new RuntimeException(msg);
            }
            return readStream(conn.getInputStream());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String buildBody(String userInput) throws Exception {
        JSONObject msg = new JSONObject();
        msg.put("role", "user");
        msg.put("content", userInput);
        JSONArray messages = new JSONArray();
        messages.put(msg);
        JSONObject root = new JSONObject();
        root.put("model", "deepseek-chat");
        root.put("messages", messages);
        return root.toString();
    }

    @Nullable
    private static String parseContent(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) return null;
            JSONObject first = choices.getJSONObject(0);
            JSONObject message = first.optJSONObject("message");
            if (message == null) return null;
            String content = message.optString("content", "").trim();
            return content.isEmpty() ? null : content;
        } catch (Exception e) {
            Log.e(TAG, "parseContent", e);
            return null;
        }
    }

    /** Extract a short user-facing message from API error JSON (e.g. "Insufficient Balance"). */
    private static String extractShortError(String err) {
        if (err == null || err.isEmpty()) return "";
        try {
            JSONObject root = new JSONObject(err.trim());
            JSONObject error = root.optJSONObject("error");
            if (error != null) {
                String message = error.optString("message", "").trim();
                if (!message.isEmpty()) return message;
            }
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
