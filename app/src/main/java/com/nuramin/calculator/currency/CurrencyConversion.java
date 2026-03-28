package com.nuramin.calculator.currency;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Cross-rate math: each currency has a "rate to USD" (how many USD one unit equals).
 * Pure JVM; covered by unit tests.
 */
public final class CurrencyConversion {

    private CurrencyConversion() {}

    /**
     * @param amount            amount in source currency
     * @param fromRateToUsd     USD value of one unit of source currency
     * @param toRateToUsd       USD value of one unit of target currency
     * @return converted amount, or NaN if inputs are non-finite; 0 if target rate is non-positive
     */
    public static double convertViaUsd(double amount, double fromRateToUsd, double toRateToUsd) {
        if (!Double.isFinite(amount) || !Double.isFinite(fromRateToUsd)) {
            return Double.NaN;
        }
        if (!Double.isFinite(toRateToUsd) || toRateToUsd <= 0) {
            return 0;
        }
        double usdValue = amount * fromRateToUsd;
        return usdValue / toRateToUsd;
    }

    /** How many {@code to} units per one {@code from} unit (for display). */
    public static double unitsOfTargetPerOneSource(double fromRateToUsd, double toRateToUsd) {
        if (!Double.isFinite(fromRateToUsd) || !Double.isFinite(toRateToUsd) || toRateToUsd <= 0) {
            return Double.NaN;
        }
        return fromRateToUsd / toRateToUsd;
    }

    public static boolean isBlankAmount(@Nullable String raw) {
        if (raw == null) return true;
        return raw.replace(",", "").trim().isEmpty();
    }

    public static boolean isValidAmount(@Nullable String raw) {
        if (isBlankAmount(raw)) return false;
        try {
            double v = Double.parseDouble(raw.replace(",", "").trim());
            return Double.isFinite(v);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Parses a non-blank valid amount string; caller must validate first. */
    public static double parseAmount(@NonNull String raw) {
        return Double.parseDouble(raw.replace(",", "").trim());
    }
}
