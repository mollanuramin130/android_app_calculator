package com.nuramin.sunsetcoralcalculator.ai.core;

import androidx.annotation.NonNull;

import com.nuramin.sunsetcoralcalculator.ai.calculator.CalculationRouter;

/**
 * AI flow: Input → Detect Intent → Parse → Calculate → Return Result.
 * 100% offline, no API calls. Target &lt;100ms response.
 */
public final class AIEngine {

    private final CalculationRouter router = new CalculationRouter();

    @NonNull
    public AIResult process(@NonNull String input) {
        if (input == null || input.trim().isEmpty()) {
            return AIResult.unknown("Try: 5000 loan at 8% for 5 years");
        }
        String trimmed = input.trim();
        AIResult.Type intent = IntentClassifier.classify(trimmed);

        if (intent == AIResult.Type.UNKNOWN) {
            return AIResult.unknown(getSuggestionForUnknown(trimmed));
        }

        return router.calculate(intent, trimmed);
    }

    private String getSuggestionForUnknown(String input) {
        if (input.matches(".*\\d+.*")) {
            return "Try: Calculate EMI? or Add GST?";
        }
        return "Try: 5000 loan at 8% for 5 years";
    }
}
