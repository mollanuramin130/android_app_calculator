package com.nuramin.calculator.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central amount/number formatter for the app. Use everywhere (Basic, Scientific, EMI, Interest,
 * Currency, etc.) so large values are shown with grouping (e.g. 9,999,999,999,999) and users can
 * read them easily. Very large or very small numbers use compact scientific notation.
 */
public final class AmountFormatter {

    private static final double LARGE_THRESHOLD = 1e12;   // use scientific above this
    private static final double SMALL_THRESHOLD = 1e-4;     // use scientific below this (positive)

    private static final DecimalFormat WITH_GROUPING;
    private static final DecimalFormat SCIENTIFIC;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        WITH_GROUPING = new DecimalFormat("#,###.##", symbols);
        WITH_GROUPING.setGroupingUsed(true);
        WITH_GROUPING.setGroupingSize(3);
        SCIENTIFIC = new DecimalFormat("0.####E0", symbols);
    }

    private AmountFormatter() {}

    /**
     * Format a number for display: grouping (commas) for readability; scientific for very large/small.
     */
    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        double abs = Math.abs(value);
        if (abs >= LARGE_THRESHOLD || (abs > 0 && abs < SMALL_THRESHOLD)) {
            return SCIENTIFIC.format(value);
        }
        if (value == (long) value) {
            return WITH_GROUPING.format((long) value);
        }
        return WITH_GROUPING.format(value);
    }

    /** Format with grouping only (no scientific). Use for expression display so user always sees e.g. 9,999,999,999,999. */
    public static String formatWithGroupingOnly(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        if (value == (long) value) {
            return WITH_GROUPING.format((long) value);
        }
        return WITH_GROUPING.format(value);
    }

    /**
     * Format a long for display (always with grouping, no scientific).
     */
    public static String format(long value) {
        return WITH_GROUPING.format(value);
    }

    /**
     * Remove grouping characters (commas, spaces) from input so it can be parsed or stored as raw.
     */
    public static String stripGrouping(CharSequence s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ',' && c != '\u00A0' && c != ' ') sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Parse a string that may contain grouping (e.g. "1,000.50") to a double.
     */
    public static double parse(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Double.parseDouble(stripGrouping(s.toString()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Matches a number: digits, or .digits, or digits.optionalDecimals */
    private static final Pattern NUMBER_IN_EXPRESSION = Pattern.compile("\\.\\d+|\\d+(?:\\.\\d+)?");

    /**
     * Format an expression for display: each number in the string is formatted with grouping.
     * Example: "9999999999999+1" → "9,999,999,999,999+1"
     */
    public static String formatExpressionForDisplay(String rawExpression) {
        if (rawExpression == null || rawExpression.isEmpty()) return rawExpression;
        String stripped = stripGrouping(rawExpression);
        Matcher m = NUMBER_IN_EXPRESSION.matcher(stripped);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String numStr = m.group();
            try {
                double n = Double.parseDouble(numStr);
                m.appendReplacement(out, Matcher.quoteReplacement(formatWithGroupingOnly(n)));
            } catch (NumberFormatException ignored) {
                // keep as-is
            }
        }
        m.appendTail(out);
        return out.toString();
    }

    /**
     * Map a cursor position in the raw expression to the position in the formatted (display) string.
     * Used so selection stays correct when showing formatted numbers (e.g. commas).
     */
    public static int rawIndexToFormattedIndex(String formatted, String raw, int rawIndex) {
        if (formatted == null || raw == null || rawIndex <= 0) return 0;
        if (rawIndex >= raw.length()) return formatted.length();
        int rawCount = 0;
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (c == ',' || c == '\u00A0' || c == ' ') continue;
            if (rawCount == rawIndex) return i;
            rawCount++;
        }
        return formatted.length();
    }
}
