package com.nuramin.calculator.favorites;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.nuramin.sunsetcoralcalculator.R;

/**
 * Remaining time formatting, urgency tiers, and priority visuals for favourites rows.
 */
public final class FavoriteUrgencyHelper {

    public enum Urgency {
        SAFE,
        WARNING,
        CRITICAL
    }

    private FavoriteUrgencyHelper() {}

    public static boolean blocksAutoDelete(FavoriteRowMeta m) {
        return m != null && m.shouldBlockAutoDelete();
    }

    public static Urgency urgencyForExpiry(long expireAtMs, long nowMs) {
        long rem = expireAtMs - nowMs;
        if (rem <= 0) return Urgency.CRITICAL;
        if (rem <= 60 * 60 * 1000L) return Urgency.CRITICAL;
        if (rem <= 24 * 60 * 60 * 1000L) return Urgency.WARNING;
        return Urgency.SAFE;
    }

    public static String formatRemaining(Context context, long expireAtMs, long nowMs) {
        long rem = expireAtMs - nowMs;
        Resources res = context.getResources();
        if (rem <= 0) {
            return context.getString(R.string.favorites_expiring_now);
        }
        long minutes = rem / (60 * 1000L);
        long hours = rem / (60 * 60 * 1000L);
        long days = rem / (24 * 60 * 60 * 1000L);
        if (days >= 1) {
            return res.getQuantityString(R.plurals.favorites_days_left, (int) Math.min(days, Integer.MAX_VALUE), days);
        }
        if (hours >= 1) {
            return res.getQuantityString(R.plurals.favorites_hours_left, (int) Math.min(hours, Integer.MAX_VALUE), hours);
        }
        int m = (int) Math.max(1, minutes);
        return res.getQuantityString(R.plurals.favorites_minutes_left, m, m);
    }

    @ColorInt
    public static int priorityStripColor(Context context, int priorityLevel, int customWeight) {
        switch (priorityLevel) {
            case FavoriteRowMeta.PRIORITY_LOW:
                return ContextCompat.getColor(context, R.color.fav_priority_low);
            case FavoriteRowMeta.PRIORITY_MEDIUM:
                return ContextCompat.getColor(context, R.color.fav_priority_medium);
            case FavoriteRowMeta.PRIORITY_HIGH:
                return ContextCompat.getColor(context, R.color.fav_priority_high);
            case FavoriteRowMeta.PRIORITY_CUSTOM:
                return ContextCompat.getColor(context, R.color.fav_priority_custom);
            default:
                return ContextCompat.getColor(context, R.color.fav_priority_medium);
        }
    }

    public static String priorityBadgeText(Context context, int priorityLevel, int customWeight) {
        switch (priorityLevel) {
            case FavoriteRowMeta.PRIORITY_LOW:
                return context.getString(R.string.favorites_badge_low);
            case FavoriteRowMeta.PRIORITY_MEDIUM:
                return context.getString(R.string.favorites_badge_medium);
            case FavoriteRowMeta.PRIORITY_HIGH:
                return context.getString(R.string.favorites_badge_high);
            case FavoriteRowMeta.PRIORITY_CUSTOM:
                return context.getString(R.string.favorites_badge_custom, customWeight);
            default:
                return "";
        }
    }

    @ColorInt
    public static int urgencyTextColor(Context context, Urgency u) {
        switch (u) {
            case SAFE:
                return ContextCompat.getColor(context, R.color.fav_urgency_safe_text);
            case WARNING:
                return ContextCompat.getColor(context, R.color.fav_urgency_warning_text);
            case CRITICAL:
                return ContextCompat.getColor(context, R.color.fav_urgency_critical_text);
            default:
                return ContextCompat.getColor(context, R.color.history_timestamp_text);
        }
    }

    /**
     * Subtle overlay on top of row background when auto-delete is active.
     */
    @ColorInt
    public static int urgencyRowOverlay(Context context, Urgency u) {
        switch (u) {
            case SAFE:
                return Color.TRANSPARENT;
            case WARNING:
                return ContextCompat.getColor(context, R.color.fav_urgency_warning_overlay);
            case CRITICAL:
                return ContextCompat.getColor(context, R.color.fav_urgency_critical_overlay);
            default:
                return Color.TRANSPARENT;
        }
    }

    /** Suggested auto-delete duration from screen label (heuristic). */
    public static long suggestedAutoDeleteDurationMs(String screenTitle) {
        String s = screenTitle == null ? "" : screenTitle.toLowerCase();
        if (s.contains("game") || s.contains("puzzle") || s.contains("grid")) {
            return 3L * 24L * 60L * 60L * 1000L;
        }
        return 7L * 24L * 60L * 60L * 1000L;
    }

    /** Human-readable duration for toasts (e.g. "7 days"). */
    public static String formatSuggestedDurationLabel(Context context, long durationMs) {
        Resources res = context.getResources();
        long days = durationMs / (24L * 60L * 60L * 1000L);
        if (days >= 1) {
            int d = (int) Math.min(days, Integer.MAX_VALUE);
            return res.getQuantityString(R.plurals.favorites_days_duration, d, d);
        }
        long hours = durationMs / (60L * 60L * 1000L);
        if (hours >= 1) {
            int h = (int) Math.min(hours, Integer.MAX_VALUE);
            return res.getQuantityString(R.plurals.favorites_hours_duration, h, h);
        }
        int m = (int) Math.max(1, durationMs / (60L * 1000L));
        return res.getQuantityString(R.plurals.favorites_minutes_duration, m, m);
    }
}
