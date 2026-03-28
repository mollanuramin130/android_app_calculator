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

    /** Use scientific notation only above this (e.g. 1e9). */
    private static final double LARGE_THRESHOLD = 1e9;
    /** Use scientific notation only below this (e.g. 1e-6). Between 1e-6 and 1e9 show normal decimal with up to 6 places so 0.008 and 0.002 show correctly. */
    private static final double SMALL_THRESHOLD = 1e-6;

    private static final DecimalFormat WITH_GROUPING;
    /** Normal numbers: up to 6 decimal places so 0.008 and 0.002 display as 0.008, 0.002 not 0.01 or 0. */
    private static final DecimalFormat WITH_GROUPING_6DEC;
    private static final DecimalFormat SCIENTIFIC;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        WITH_GROUPING = new DecimalFormat("#,###.##", symbols);
        WITH_GROUPING.setGroupingUsed(true);
        WITH_GROUPING.setGroupingSize(3);
        WITH_GROUPING_6DEC = new DecimalFormat("#,###.######", symbols);
        WITH_GROUPING_6DEC.setGroupingUsed(true);
        WITH_GROUPING_6DEC.setGroupingSize(3);
        SCIENTIFIC = new DecimalFormat("0.####E0", symbols);
    }

    private AmountFormatter() {}

    /**
     * Format a number for display: show normal decimal (up to 6 places) within range; use exponential only when past limit.
     * So 0.008 and 0.002 show as 0.008, 0.002 in live result; only when value is very small (&lt; 1e-6) or very large (≥ 1e9) use scientific.
     */
    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        if (value == 0) return "0";
        double abs = Math.abs(value);
        if (abs >= LARGE_THRESHOLD || (abs > 0 && abs < SMALL_THRESHOLD)) {
            return SCIENTIFIC.format(value);
        }
        if (value == (long) value && abs <= Long.MAX_VALUE) {
            return WITH_GROUPING.format((long) value);
        }
        return WITH_GROUPING_6DEC.format(value);
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
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        char grouping = symbols.getGroupingSeparator();
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != grouping && c != ',' && c != '\u00A0' && c != ' ') sb.append(c);
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

    /** Matches a number: digits with optional . and more digits, or .digits (preserves trailing dot e.g. "2.") */
    private static final Pattern NUMBER_IN_EXPRESSION = Pattern.compile("\\d+\\.?\\d*|\\.\\d+");

    /**
     * Format an expression for display: add grouping (commas) to integer parts only; preserve decimal
     * part exactly so user can type 2.06, 2.006, 2.0 without digits being lost or rounded.
     * Example: "9999999999999+1" → "9,999,999,999,999+1"; "2.006" stays "2.006".
     */
    public static String formatExpressionForDisplay(String rawExpression) {
        if (rawExpression == null || rawExpression.isEmpty()) return "";
        String stripped = stripGrouping(rawExpression);
        Matcher m = NUMBER_IN_EXPRESSION.matcher(stripped);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String numStr = m.group();
            String formatted = addGroupingToNumberString(numStr);
            m.appendReplacement(out, Matcher.quoteReplacement(formatted));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Add grouping (commas) to integer part only; decimal part is preserved exactly. */
    private static String addGroupingToNumberString(String numStr) {
        if (numStr == null || numStr.isEmpty()) return numStr;
        int dotIdx = numStr.indexOf('.');
        String intPart = dotIdx >= 0 ? numStr.substring(0, dotIdx) : numStr;
        String decPart = dotIdx >= 0 ? numStr.substring(dotIdx) : ""; // includes the "." and all digits after
        if (intPart.isEmpty()) return numStr; // e.g. ".5"
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < intPart.length(); i++) {
            char grouping = new DecimalFormatSymbols(Locale.getDefault()).getGroupingSeparator();
            if (i > 0 && (intPart.length() - i) % 3 == 0) grouped.append(grouping);
            grouped.append(intPart.charAt(i));
        }
        return grouped.append(decPart).toString();
    }

    /**
     * Map a cursor position in the raw expression to the position in the formatted (display) string.
     * rawIndex = number of raw characters before cursor; returns formatted position (cursor after that many chars).
     * E.g. "1,234" with rawIndex 2 -> return 2 (cursor after "12").
     */
    public static int rawIndexToFormattedIndex(String formatted, String raw, int rawIndex) {
        if (formatted == null || raw == null || rawIndex <= 0) return 0;
        if (rawIndex >= raw.length()) return formatted.length();
        int rawCount = 0;
        char grouping = new DecimalFormatSymbols(Locale.getDefault()).getGroupingSeparator();
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (c == grouping || c == 44 || c == '\u00A0' || c == ' ') continue; // 44 = ASCII comma
            rawCount++;
            if (rawCount == rawIndex) return i + 1;
        }
        return formatted.length();
    }

    /**
     * Map a cursor position in the formatted (display) string to the position in the raw expression.
     * Returns the number of non-grouping characters in formatted[0, formattedIndex).
     * E.g. "1,234" with formattedIndex 2 -> return 2 (two raw chars "1","2" before position 2).
     */
    public static int formattedIndexToRawIndex(String formatted, int formattedIndex) {
        if (formatted == null) return 0;
        if (formattedIndex <= 0) return 0;
        int end = formattedIndex > formatted.length() ? formatted.length() : formattedIndex;
        int rawCount = 0;
        char grouping = new DecimalFormatSymbols(Locale.getDefault()).getGroupingSeparator();
        for (int i = 0; i < end; i++) {
            char c = formatted.charAt(i);
            if (c != grouping && c != 44 && c != '\u00A0' && c != ' ') rawCount++; // 44 = ASCII comma
        }
        return rawCount;
    }
}
