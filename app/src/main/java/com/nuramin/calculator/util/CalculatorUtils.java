package com.nuramin.calculator.util;

/**
 * Shared number formatting and expression filtering for calculator screens.
 * Number formatting is delegated to {@link AmountFormatter} for consistent display app-wide.
 */
public final class CalculatorUtils {

    /** Format a number for display (grouping, scientific when needed). Use everywhere: Basic, Scientific, EMI, Interest, etc. */
    public static String formatNumber(double value) {
        return AmountFormatter.format(value);
    }

    /**
     * Keep only characters valid for calculator expression (digits, ., +, −, ×, ÷, (, ), %, ^, !,
     * letters for sin/cos/tan/sqrt/ln/log/asin/acos/atan, π and e). Commas are not allowed so pasted "1,000" becomes "1000".
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
        return AmountFormatter.stripGrouping(sb);
    }

    private CalculatorUtils() {}
}
