package com.nuramin.calculator.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.NumberFormat;

/**
 * Money amounts as locale-grouped numbers only — no symbols, no ISO codes ($ ₹ USD).
 * Pair with {@code ic_generic_currency} drawable in UI where money/currency is indicated.
 */
public final class CurrencyFormatter {

    private CurrencyFormatter() {}

    /**
     * Grouped numeric amount, e.g. {@code "10,000.5"} — no trailing currency code or symbol.
     */
    @NonNull
    public static String formatAmount(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(LocaleFormatManager.getLocale());
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(0);
        nf.setGroupingUsed(true);
        return nf.format(amount);
    }

    /**
     * Same as {@link #formatAmount(double)}; currency code is ignored (neutral global UI).
     */
    @NonNull
    public static String formatAmount(double amount, @Nullable String ignoredCurrencyCode) {
        return formatAmount(amount);
    }
}
