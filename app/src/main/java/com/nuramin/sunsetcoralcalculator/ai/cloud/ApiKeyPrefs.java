package com.nuramin.sunsetcoralcalculator.ai.cloud;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stores and retrieves DeepSeek API key in SharedPreferences.
 * Used as runtime fallback when BuildConfig key is empty (e.g. after adding key without rebuild).
 */
public final class ApiKeyPrefs {

    private static final String PREFS_NAME = "ai_cloud_prefs";
    private static final String KEY_API_KEY = "deepseek_api_key";

    @Nullable
    public static String get(@NonNull Context context) {
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String value = prefs.getString(KEY_API_KEY, null);
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void set(@NonNull Context context, @Nullable String apiKey) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_API_KEY, apiKey != null ? apiKey.trim() : "").apply();
    }
}
