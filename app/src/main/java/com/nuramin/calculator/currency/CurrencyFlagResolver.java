package com.nuramin.calculator.currency;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nuramin.sunsetcoralcalculator.R;

import java.util.Locale;

/**
 * Resolves flag drawables ({@code R.drawable.flag_xx}) or falls back to a placeholder.
 * Add PNG/vector drawables named {@code flag_us}, {@code flag_gb}, etc. under {@code res/drawable/}.
 */
public final class CurrencyFlagResolver {

    private CurrencyFlagResolver() {}

    @DrawableRes
    public static int getFlagDrawableResId(@NonNull Context context, @Nullable String iso3166Alpha2) {
        if (iso3166Alpha2 == null || iso3166Alpha2.length() != 2) {
            return R.drawable.flag_placeholder;
        }
        String key = "flag_" + iso3166Alpha2.toLowerCase(Locale.US);
        int id = context.getResources().getIdentifier(key, "drawable", context.getPackageName());
        if (id != 0) return id;
        return R.drawable.flag_placeholder;
    }

    /** Prefer packaged currency icons when present (EUR etc.), else {@code flag_xx} drawable. */
    @DrawableRes
    public static int getFlagForCurrency(@NonNull Context context, @NonNull CurrencyItem item) {
        switch (item.getCode()) {
            case "EUR":
                return R.drawable.ic_flag_eur;
            case "USD":
                return R.drawable.ic_flag_us;
            case "GBP":
                return R.drawable.ic_flag_gb;
            case "INR":
                return R.drawable.ic_flag_in;
            case "BDT":
                return R.drawable.ic_flag_bd;
            default:
                return getFlagDrawableResId(context, item.getFlagCountryCode());
        }
    }

    /** Regional indicator emoji (e.g. US → 🇺🇸). Empty if invalid. */
    @NonNull
    public static String emojiFlag(@Nullable String iso3166Alpha2) {
        if (iso3166Alpha2 == null || iso3166Alpha2.length() != 2) return "";
        String cc = iso3166Alpha2.toUpperCase(Locale.US);
        char a = cc.charAt(0);
        char b = cc.charAt(1);
        if (a < 'A' || a > 'Z' || b < 'A' || b > 'Z') return "";
        int cp1 = 0x1F1E6 + (a - 'A');
        int cp2 = 0x1F1E6 + (b - 'A');
        return new String(new int[]{cp1, cp2}, 0, 2);
    }
}
