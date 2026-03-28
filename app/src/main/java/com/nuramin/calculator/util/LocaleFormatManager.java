package com.nuramin.calculator.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.nuramin.sunsetcoralcalculator.R;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

/**
 * Locale and currency helper for globally-ready formatting.
 */
public final class LocaleFormatManager {

    private static final String PREFS = "global_locale_prefs";
    private static final String KEY_CURRENCY_OVERRIDE = "currency_override";
    private static final String[] POPULAR_CODES = {
            "USD", "EUR", "GBP", "JPY", "CNY", "INR", "BDT", "AUD", "CAD", "SAR", "AED"
    };

    private LocaleFormatManager() {}

    @NonNull
    public static Locale getLocale() {
        return Locale.getDefault();
    }

    @NonNull
    public static Currency getCurrency(@Nullable Context context) {
        if (context != null) {
            String code = getCurrencyOverrideCode(context);
            if (code != null && !code.trim().isEmpty()) {
                try {
                    return Currency.getInstance(code);
                } catch (IllegalArgumentException ignored) {
                    // Fall through to locale default.
                }
            }
        }
        try {
            return Currency.getInstance(getLocale());
        } catch (Exception ignored) {
            return Currency.getInstance("USD");
        }
    }

    @Nullable
    public static String getCurrencyOverrideCode(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENCY_OVERRIDE, null);
    }

    public static void setCurrencyOverrideCode(@NonNull Context context, @Nullable String code) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENCY_OVERRIDE, code).apply();
    }

    @NonNull
    public static String formatCurrency(@Nullable Context context, double value) {
        return CurrencyFormatter.formatAmount(value);
    }

    @NonNull
    public static String formatNumber(double value) {
        return NumberFormat.getNumberInstance(getLocale()).format(value);
    }

    public static double parseLocalizedNumber(@Nullable CharSequence text, double def) {
        if (text == null) return def;
        String raw = text.toString().trim();
        if (raw.isEmpty()) return def;
        NumberFormat nf = NumberFormat.getNumberInstance(getLocale());
        try {
            Number n = nf.parse(raw);
            return n != null ? n.doubleValue() : def;
        } catch (ParseException e) {
            try {
                return Double.parseDouble(raw.replace(",", "").trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
    }

    public static int parseLocalizedInt(@Nullable CharSequence text, int def) {
        return (int) Math.round(parseLocalizedNumber(text, def));
    }

    public interface OnCurrencyChangedListener {
        void onCurrencyChanged();
    }

    public static void showCurrencyPickerDialog(@NonNull Context context, @Nullable OnCurrencyChangedListener listener) {
        String[] labels = new String[POPULAR_CODES.length + 1];
        labels[0] = context.getString(R.string.currency_picker_device_default);
        for (int i = 0; i < POPULAR_CODES.length; i++) {
            Currency c = Currency.getInstance(POPULAR_CODES[i]);
            labels[i + 1] = POPULAR_CODES[i] + " — " + c.getDisplayName(getLocale());
        }
        String current = getCurrencyOverrideCode(context);
        int checked = 0;
        if (current != null) {
            for (int i = 0; i < POPULAR_CODES.length; i++) {
                if (POPULAR_CODES[i].equalsIgnoreCase(current)) {
                    checked = i + 1;
                    break;
                }
            }
        }

        final int[] selected = { checked };
        new AlertDialog.Builder(context)
                .setTitle(R.string.currency_picker_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> selected[0] = which)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (selected[0] == 0) {
                        setCurrencyOverrideCode(context, null);
                    } else {
                        setCurrencyOverrideCode(context, POPULAR_CODES[selected[0] - 1]);
                    }
                    if (listener != null) listener.onCurrencyChanged();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
