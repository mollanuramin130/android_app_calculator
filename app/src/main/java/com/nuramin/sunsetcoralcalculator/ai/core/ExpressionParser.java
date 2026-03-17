package com.nuramin.sunsetcoralcalculator.ai.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts numbers, percentage, and duration from natural language for AI calculator.
 * Regex-based, offline.
 */
public final class ExpressionParser {

    private static final Pattern NUMBER = Pattern.compile("(?:^|[^\\d.])(\\d+(?:\\.\\d+)?)(?:\\s*(?:lakh|lac|l|cr|crore|k|thousand))?");
    private static final Pattern PERCENT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
    private static final Pattern YEARS = Pattern.compile("(\\d+)\\s*(?:years?|yrs?|y)\\b");
    private static final Pattern MONTHS = Pattern.compile("(\\d+)\\s*(?:months?|mon|mos?)\\b");

    @NonNull
    public static List<Double> extractNumbers(@NonNull String input) {
        List<Double> out = new ArrayList<>();
        String normalized = input.replace(",", "").trim();
        Matcher m = NUMBER.matcher(normalized);
        while (m.find()) {
            try {
                String g = m.group(1);
                if (g != null) out.add(Double.parseDouble(g));
            } catch (NumberFormatException ignored) { }
        }
        if (out.isEmpty()) {
            Matcher numOnly = Pattern.compile("\\d+(?:\\.\\d+)?").matcher(normalized);
            while (numOnly.find()) {
                try {
                    out.add(Double.parseDouble(numOnly.group()));
                } catch (NumberFormatException ignored) { }
            }
        }
        return out;
    }

    @Nullable
    public static Double extractPercentage(@NonNull String input) {
        Matcher m = PERCENT.matcher(input);
        return m.find() ? parseDouble(m.group(1)) : null;
    }

    public static int extractYears(@NonNull String input) {
        Matcher m = YEARS.matcher(input.toLowerCase(Locale.US));
        return m.find() ? parseInt(m.group(1), 0) : 0;
    }

    public static int extractMonths(@NonNull String input) {
        Matcher m = MONTHS.matcher(input.toLowerCase(Locale.US));
        if (m.find()) return parseInt(m.group(1), 0);
        int years = extractYears(input);
        return years > 0 ? years * 12 : 0;
    }

    /** Normalize "5l", "5 lakh" to 500000 etc. */
    public static double parseAmount(@NonNull String raw) {
        String s = raw.trim().toLowerCase(Locale.US).replace(",", "");
        double mult = 1;
        if (s.endsWith("l") || s.endsWith("lac") || s.endsWith("lakh")) {
            mult = 100_000;
            s = s.replaceAll("(lac|lakh|l)$", "").trim();
        } else if (s.endsWith("cr") || s.endsWith("crore")) {
            mult = 10_000_000;
            s = s.replaceAll("(crore|cr)$", "").trim();
        } else if (s.endsWith("k") || s.endsWith("thousand")) {
            mult = 1_000;
            s = s.replaceAll("(thousand|k)$", "").trim();
        }
        Double d = parseDouble(s);
        return (d != null ? d : 0) * mult;
    }

    @Nullable
    private static Double parseDouble(@NonNull String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseInt(@NonNull String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
