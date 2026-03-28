package com.nuramin.sunsetcoralcalculator.ai.calculator;

import androidx.annotation.NonNull;

import com.nuramin.calculator.util.CurrencyFormatter;

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
        if (r < 0) return null;
        if (r == 0) {
            double flat = principalRs / months;
            return Double.isFinite(flat) ? Math.round(flat * 100) / 100.0 : null;
        }
        double pow = Math.pow(1 + r, months);
        if (!Double.isFinite(pow) || pow <= 1) return null;
        double emi = principalRs * r * pow / (pow - 1);
        if (!Double.isFinite(emi)) return null;
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
        return CurrencyFormatter.formatAmount(value);
    }

    @NonNull
    public static String formatMonthly(double emi) {
        return formatCurrency(emi) + "/month";
    }
}
