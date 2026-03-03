package com.nuramin.calculator.dto;

/**
 * DTO for a calculation result (e.g. from expression evaluation).
 */
public class CalculationResult {
    private final double value;
    private final String formattedResult;

    public CalculationResult(double value, String formattedResult) {
        this.value = value;
        this.formattedResult = formattedResult;
    }

    public double getValue() {
        return value;
    }

    public String getFormattedResult() {
        return formattedResult;
    }
}
