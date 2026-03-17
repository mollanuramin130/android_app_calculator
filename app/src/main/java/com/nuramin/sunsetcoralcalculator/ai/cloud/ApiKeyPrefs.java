package com.nuramin.sunsetcoralcalculator.ai.cloud;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stores and retrieves cloud AI API keys. Gemini (free) is primary; DeepSeek is optional fallback.
 */
public final class ApiKeyPrefs {

    private static final String PREFS_NAME = "ai_cloud_prefs";
    private static final String KEY_GEMINI = "gemini_api_key";
    private static final String KEY_DEEPSEEK = "deepseek_api_key";

    /** Gemini key (free at aistudio.google.com/apikey). Used by menu "API key" dialog. */
    @Nullable
    public static String getGemini(@NonNull Context context) {
        return get(context, KEY_GEMINI);
    }

    public static void setGemini(@NonNull Context context, @Nullable String apiKey) {
        set(context, KEY_GEMINI, apiKey);
    }

    @Nullable
    public static String getDeepSeek(@NonNull Context context) {
        return get(context, KEY_DEEPSEEK);
    }

    public static void setDeepSeek(@NonNull Context context, @Nullable String apiKey) {
        set(context, KEY_DEEPSEEK, apiKey);
    }

    /** Returns Gemini key (backward compat: in-app dialog stores Gemini). */
    @Nullable
    public static String get(@NonNull Context context) {
        return getGemini(context);
    }

    public static void set(@NonNull Context context, @Nullable String apiKey) {
        setGemini(context, apiKey);
    }

    @Nullable
    private static String get(@NonNull Context context, String key) {
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String value = prefs.getString(key, null);
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void set(@NonNull Context context, String key, @Nullable String apiKey) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, apiKey != null ? apiKey.trim() : "").apply();
    }
}
