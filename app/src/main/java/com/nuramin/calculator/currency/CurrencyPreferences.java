package com.nuramin.calculator.currency;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Persists last selected from/to currency codes.
 */
public final class CurrencyPreferences {

    private static final String PREFS = "currency_converter_prefs";
    private static final String KEY_FROM = "from_code";
    private static final String KEY_TO = "to_code";

    private CurrencyPreferences() {}

    public static void saveSelection(@NonNull Context context, @NonNull String fromCode, @NonNull String toCode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_FROM, fromCode)
                .putString(KEY_TO, toCode)
                .apply();
    }

    @NonNull
    public static String getFromCode(@NonNull Context context, @NonNull String defaultCode) {
        String s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_FROM, defaultCode);
        return s != null ? s : defaultCode;
    }

    @NonNull
    public static String getToCode(@NonNull Context context, @NonNull String defaultCode) {
        String s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TO, defaultCode);
        return s != null ? s : defaultCode;
    }
}
