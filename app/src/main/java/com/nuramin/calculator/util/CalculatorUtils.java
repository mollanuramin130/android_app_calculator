package com.nuramin.calculator.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Shared number formatting and expression filtering for calculator screens.
 */
public final class CalculatorUtils {

    private static final double LARGE_THRESHOLD = 1e9;
    private static final double SMALL_THRESHOLD = 1e-3;

    private static final DecimalFormat FORMATTER;
    private static final DecimalFormat SCIENTIFIC_FORMATTER;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        FORMATTER = new DecimalFormat("#,###.##", symbols);
        FORMATTER.setGroupingUsed(true);
        SCIENTIFIC_FORMATTER = new DecimalFormat("0.####E0", symbols);
    }

    public static String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "Error";
        }
        double abs = Math.abs(value);
        if (abs >= LARGE_THRESHOLD || (abs > 0 && abs < SMALL_THRESHOLD)) {
            return SCIENTIFIC_FORMATTER.format(value);
        }
        if (value == (long) value) {
            return FORMATTER.format((long) value);
        }
        return FORMATTER.format(value);
    }

    /**
     * Keep only characters valid for calculator expression (digits, ., +, −, ×, ÷, (, ), %, ^, !,
     * letters for sin/cos/tan/sqrt/ln/log/asin/acos/atan, π and e).
     */
    public static String filterExpressionChars(CharSequence s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == '+' || c == '−' || c == '-' || c == '×' || c == '*' || c == '÷' || c == '/' || c == '(' || c == ')' || c == '%' || c == ' ' || c == '^' || c == '!' || Character.isLetter(c) || c == 'π') {
                if (c == '*') sb.append('×');
                else if (c == '/') sb.append('÷');
                else if (c != ' ') sb.append(c);
            }
        }
        return sb.toString();
    }

    private CalculatorUtils() {}
}
