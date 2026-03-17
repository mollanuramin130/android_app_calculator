package com.nuramin.sunsetcoralcalculator.ai.core;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Classifies user input into calculator intents (EMI, AGE, DISCOUNT, GST).
 * Keyword-based, 100% offline.
 */
public final class IntentClassifier {

    private static final Pattern EMI_PATTERN = Pattern.compile(
            "\\b(loan|emi|emı|principal|interest\\s*rate|tenure|monthly\\s*payment)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern AGE_PATTERN = Pattern.compile(
            "\\b(age|dob|birth|birthday|years?\\s*old|date\\s*of\\s*birth)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DISCOUNT_PATTERN = Pattern.compile(
            "\\b(discount|percent\\s*off|off\\s*price|sale|marked\\s*price)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GST_PATTERN = Pattern.compile(
            "\\b(gst|tax|cgst|sgst|igst)\\b",
            Pattern.CASE_INSENSITIVE);

    @NonNull
    public static AIResult.Type classify(@NonNull String input) {
        if (input == null || input.trim().isEmpty()) {
            return AIResult.Type.UNKNOWN;
        }
        String normalized = input.trim().toLowerCase(Locale.US);

        if (EMI_PATTERN.matcher(normalized).find()) return AIResult.Type.EMI;
        if (AGE_PATTERN.matcher(normalized).find()) return AIResult.Type.AGE;
        if (GST_PATTERN.matcher(normalized).find()) return AIResult.Type.GST;
        if (DISCOUNT_PATTERN.matcher(normalized).find()) return AIResult.Type.DISCOUNT;

        return AIResult.Type.UNKNOWN;
    }
}
