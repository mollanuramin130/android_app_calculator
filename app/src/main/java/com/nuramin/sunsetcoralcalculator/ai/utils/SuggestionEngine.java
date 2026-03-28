package com.nuramin.sunsetcoralcalculator.ai.utils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Suggests actions based on current input (e.g. "5000" → "Calculate EMI?", "Add GST?").
 */
public final class SuggestionEngine {

    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");

    @NonNull
    public static List<String> getSuggestions(@NonNull String input) {
        List<String> out = new ArrayList<>();
        String t = input.trim();
        if (t.isEmpty()) {
            out.add("5000 loan at 8% for 5 years");
            out.add("Age from 15 March 1990");
            out.add("1000 with 18% tax");
            out.add("500 with 20% discount");
            return out;
        }
        if (HAS_DIGIT.matcher(t).find()) {
            out.add("Calculate EMI?");
            out.add("Add tax?");
            if (t.length() <= 4) {
                out.add("Discount %?");
            }
        }
        if (out.isEmpty()) {
            out.add("Try: 5 lakh loan at 8% for 5 years");
        }
        return out;
    }
}
