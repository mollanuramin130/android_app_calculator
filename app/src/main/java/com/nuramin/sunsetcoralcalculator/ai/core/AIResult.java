package com.nuramin.sunsetcoralcalculator.ai.core;

import androidx.annotation.NonNull;

/**
 * Result of AI calculation. Used by AIEngine and displayed in AISmartActivity.
 */
public final class AIResult {

    public enum Type {
        EMI,
        AGE,
        DISCOUNT,
        GST,
        UNKNOWN
    }

    private final Type type;
    private final String title;
    private final String resultText;
    private final String explanation;
    private final boolean success;

    public AIResult(@NonNull Type type, @NonNull String title, @NonNull String resultText,
                    @NonNull String explanation, boolean success) {
        this.type = type;
        this.title = title;
        this.resultText = resultText;
        this.explanation = explanation;
        this.success = success;
    }

    public static AIResult unknown(String suggestion) {
        return new AIResult(Type.UNKNOWN, "Try again", "",
                suggestion != null ? suggestion : "Try: 5000 loan at 8% for 5 years", false);
    }

    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getResultText() { return resultText; }
    public String getExplanation() { return explanation; }
    public boolean isSuccess() { return success; }
}
