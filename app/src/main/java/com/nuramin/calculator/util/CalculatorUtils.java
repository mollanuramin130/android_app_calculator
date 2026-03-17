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
        String stripped = AmountFormatter.stripGrouping(sb);
        stripped = sanitizeSingleDecimalPerNumber(stripped);
        return sanitizeConsecutiveOperators(stripped);
    }

    /**
     * Prevent invalid operator sequences when user edits in the middle of the expression.
     * - Collapse consecutive operators (e.g. +-, ×÷, xxx) to the last one: "5+−×" → "5×".
     * - Remove leading binary operators (allow only leading minus for negative numbers).
     */
    public static String sanitizeConsecutiveOperators(CharSequence s) {
        if (s == null || s.length() == 0) return "";
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        // Strip leading +, ×, ÷, ^ (allow leading − or -)
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '+' || c == '×' || c == '÷' || c == '^') i++;
            else break;
        }
        while (i < s.length()) {
            char c = s.charAt(i);
            if (isBinaryOperator(c)) {
                // Collapse consecutive operators to the last one (e.g. +−× → ×, ××× → ×)
                int j = i + 1;
                while (j < s.length() && isBinaryOperator(s.charAt(j))) j++;
                char lastOp = s.charAt(j - 1);
                // Allow operator only after an operand, or leading minus for negative numbers
                if (out.length() > 0 || lastOp == '−' || lastOp == '-') {
                    out.append(lastOp == '-' ? '−' : lastOp);
                }
                i = j;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static boolean isBinaryOperator(char c) {
        return c == '+' || c == '−' || c == '-' || c == '×' || c == '÷' || c == '^';
    }

    /**
     * Ensure at most one decimal point per number. "2.06.9" → "2.06"; "3.14.15" → "3.14".
     * Stops at operators, parentheses, or non-digit/non-dot so each operand has at most one dot.
     */
    public static String sanitizeSingleDecimalPerNumber(CharSequence s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                StringBuilder num = new StringBuilder();
                boolean seenDot = false;
                while (i < s.length()) {
                    char ch = s.charAt(i);
                    if (Character.isDigit(ch)) {
                        num.append(ch);
                        i++;
                    } else if (ch == '.' && !seenDot) {
                        num.append(ch);
                        seenDot = true;
                        i++;
                    } else if (ch == '.' && seenDot) {
                        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
                        break;
                    } else break;
                }
                out.append(num);
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private CalculatorUtils() {}
}
