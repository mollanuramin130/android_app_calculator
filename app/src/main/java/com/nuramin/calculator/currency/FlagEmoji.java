package com.nuramin.calculator.currency;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Regional indicator flag emoji from ISO 3166-1 alpha-2 (e.g. US → 🇺🇸).
 * Special cases for non-country codes (metals, supranational).
 */
public final class FlagEmoji {

    private FlagEmoji() {}

    @NonNull
    public static String forCurrency(@NonNull String currencyCode, @Nullable String regionAlpha2) {
        String u = currencyCode.trim().toUpperCase();
        switch (u) {
            case "XAU":
                return "\uD83E\uDD47"; // 🥇
            case "XAG":
                return "\uD83E\uDD48"; // 🥈
            case "XDR":
            case "XTS":
            case "XXX":
                return "\uD83C\uDF10"; // globe
            default:
                break;
        }
        if (regionAlpha2 == null || regionAlpha2.length() != 2) {
            return "\uD83C\uDF0D"; // globe (no specific region)
        }
        String r = regionAlpha2.toUpperCase();
        if ("EU".equals(r)) {
            return "\uD83C\uDDEA\uD83C\uDDFA";
        }
        if ("UN".equals(r)) {
            return "\uD83C\uDF10";
        }
        char a = r.charAt(0);
        char b = r.charAt(1);
        if (a < 'A' || a > 'Z' || b < 'A' || b > 'Z') {
            return "\uD83C\uDF0D";
        }
        int cp1 = 0x1F1E6 + (a - 'A');
        int cp2 = 0x1F1E6 + (b - 'A');
        return new String(Character.toChars(cp1)) + new String(Character.toChars(cp2));
    }
}
