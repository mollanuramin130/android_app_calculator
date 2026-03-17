package com.nuramin.sunsetcoralcalculator.ai.cloud;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;

/**
 * Limits DeepSeek API usage to avoid exceeding free tier.
 * Max 5 API calls per user per day. Count resets at midnight (local time).
 */
public final class APILimiter {

    private static final String PREFS_NAME = "ai_cloud_api_limiter";
    private static final String KEY_DATE = "api_limit_date";
    private static final String KEY_COUNT = "api_limit_count";
    private static final int MAX_CALLS_PER_DAY = 5;

    private final SharedPreferences prefs;

    public APILimiter(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns true if the user can make another API call today.
     * Resets count if the stored date is not today.
     */
    public boolean canCallAPI() {
        String today = getTodayKey();
        String storedDate = prefs.getString(KEY_DATE, "");
        if (!today.equals(storedDate)) {
            prefs.edit().putString(KEY_DATE, today).putInt(KEY_COUNT, 0).apply();
            return true;
        }
        int count = prefs.getInt(KEY_COUNT, 0);
        return count < MAX_CALLS_PER_DAY;
    }

    /**
     * Increments the API call count for today. Call after a successful API request.
     */
    public void increaseCount() {
        String today = getTodayKey();
        String storedDate = prefs.getString(KEY_DATE, "");
        int count = prefs.getInt(KEY_COUNT, 0);
        if (!today.equals(storedDate)) {
            count = 0;
        }
        count++;
        prefs.edit().putString(KEY_DATE, today).putInt(KEY_COUNT, count).apply();
    }

    /** Returns remaining calls for today (0 if none). */
    public int getRemainingCalls() {
        if (!canCallAPI()) return 0;
        String today = getTodayKey();
        String storedDate = prefs.getString(KEY_DATE, "");
        if (!today.equals(storedDate)) return MAX_CALLS_PER_DAY;
        int count = prefs.getInt(KEY_COUNT, 0);
        return Math.max(0, MAX_CALLS_PER_DAY - count);
    }

    private static String getTodayKey() {
        Calendar c = Calendar.getInstance(Locale.US);
        return String.format(Locale.US, "%d-%d-%d", c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }
}
