package com.nuramin.calculator.service;

import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.ExprParser;

/**
 * Service for evaluating mathematical expressions.
 */
public final class ExpressionService {

    public static Double evaluate(String expression) {
        if (expression == null) return null;
        String s = expression.trim().replaceAll("\\s+", "");
        if (s.isEmpty()) return null;
        try {
            return new ExprParser(s).parse();
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatResult(double value) {
        return CalculatorUtils.formatNumber(value);
    }

    private ExpressionService() {}
}
