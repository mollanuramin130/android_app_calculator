package com.nuramin.sunsetcoralcalculator.ai.calculator;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Lightweight EMI calculation. Does not call existing app calculator classes.
 */
public final class EMIUtil {

    /**
     * @param principalRs Principal in rupees
     * @param ratePercent Annual interest rate (e.g. 8 for 8%)
     * @param months Tenure in months
     * @return EMI in rupees, or null if invalid
     */
    public static Double calculateEMI(double principalRs, double ratePercent, int months) {
        if (principalRs <= 0 || months <= 0) return null;
        double r = ratePercent / 12 / 100;
        if (r <= 0) return principalRs / months;
        double emi = principalRs * r * Math.pow(1 + r, months) / (Math.pow(1 + r, months) - 1);
        return Math.round(emi * 100) / 100.0;
    }

    public static double totalPayment(double emi, int months) {
        return Math.round(emi * months * 100) / 100.0;
    }

    public static double totalInterest(double totalPayment, double principal) {
        return Math.round((totalPayment - principal) * 100) / 100.0;
    }

    @NonNull
    public static String formatCurrency(double value) {
        if (value >= 1_00_00_000) {
            return String.format(Locale.US, "₹%.2f Cr", value / 1_00_00_000);
        }
        if (value >= 1_00_000) {
            return String.format(Locale.US, "₹%.2f L", value / 1_00_000);
        }
        if (value >= 1_000) {
            return String.format(Locale.US, "₹%.2f K", value / 1_000);
        }
        return String.format(Locale.US, "₹%.2f", value);
    }

    @NonNull
    public static String formatMonthly(double emi) {
        return String.format(Locale.US, "₹%,.0f/month", emi);
    }
}
