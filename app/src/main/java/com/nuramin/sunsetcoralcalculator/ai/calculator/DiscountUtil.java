package com.nuramin.sunsetcoralcalculator.ai.calculator;

import androidx.annotation.NonNull;

import com.nuramin.calculator.util.CurrencyFormatter;

/**
 * Lightweight discount / GST calculation. Does not call existing app calculator classes.
 */
public final class DiscountUtil {

    /** Final price after discount: original * (1 - percent/100) */
    public static double priceAfterDiscount(double original, double percentOff) {
        return Math.round(original * (1 - percentOff / 100) * 100) / 100.0;
    }

    /** Amount discounted */
    public static double discountAmount(double original, double percentOff) {
        return Math.round(original * (percentOff / 100) * 100) / 100.0;
    }

    /** GST amount: price * (gstPercent/100) */
    public static double gstAmount(double price, double gstPercent) {
        return Math.round(price * (gstPercent / 100) * 100) / 100.0;
    }

    /** Price including GST */
    public static double priceWithGst(double basePrice, double gstPercent) {
        return Math.round(basePrice * (1 + gstPercent / 100) * 100) / 100.0;
    }

    @NonNull
    public static String formatCurrency(double value) {
        return CurrencyFormatter.formatAmount(value);
    }
}
