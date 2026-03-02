package com.nuramin.calculator.model;

/**
 * Model for a single calculation history entry (expression + result).
 */
public class HistoryEntry {
    private final String expression;
    private final String result;

    public HistoryEntry(String expression, String result) {
        this.expression = expression;
        this.result = result;
    }

    public String getExpression() {
        return expression;
    }

    public String getResult() {
        return result;
    }
}
