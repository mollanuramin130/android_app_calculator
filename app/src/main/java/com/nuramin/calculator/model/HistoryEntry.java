package com.nuramin.calculator.model;

/**
 * Model for a single calculation history entry (expression + result, optional timestamp).
 */
public class HistoryEntry {
    private final String expression;
    private final String result;
    /** Timestamp in millis for "Old History"; null for current session. */
    private final Long timestamp;

    public HistoryEntry(String expression, String result) {
        this(expression, result, null);
    }

    public HistoryEntry(String expression, String result, Long timestamp) {
        this.expression = expression;
        this.result = result;
        this.timestamp = timestamp;
    }

    public String getExpression() {
        return expression;
    }

    public String getResult() {
        return result;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
