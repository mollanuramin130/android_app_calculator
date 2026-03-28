package com.nuramin.calculator.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for CalculatorUtils: expression filtering, sanitize decimals.
 */
@RunWith(JUnit4.class)
public class CalculatorUtilsTest {

    @Test
    public void filterExpressionChars_allowsValid() {
        assertEquals("123+456", CalculatorUtils.filterExpressionChars("123+456"));
        // * and / are normalized to × and ÷; ASCII hyphen is normalized to minus sign U+2212
        assertEquals("12−34×56÷78", CalculatorUtils.filterExpressionChars("12-34*56/78"));
        assertEquals("1.5+2.06", CalculatorUtils.filterExpressionChars("1.5+2.06"));
        assertEquals("sin(30)", CalculatorUtils.filterExpressionChars("sin(30)"));
        assertEquals("", CalculatorUtils.filterExpressionChars(null));
    }

    @Test
    public void filterExpressionChars_stripsInvalid() {
        // Non-expression chars (@# etc.) are stripped; letters and ! (factorial) allowed
        assertEquals("123", CalculatorUtils.filterExpressionChars("1@2#3"));
        assertEquals("12+34!", CalculatorUtils.filterExpressionChars("12+34!@#")); // ! kept for factorial
    }

    @Test
    public void filterExpressionChars_stripsSpaces() {
        assertEquals("1+2", CalculatorUtils.filterExpressionChars("1 + 2"));
    }

    @Test
    public void sanitizeSingleDecimalPerNumber() {
        assertEquals("2.06", CalculatorUtils.sanitizeSingleDecimalPerNumber("2.06.9"));
        assertEquals("3.14", CalculatorUtils.sanitizeSingleDecimalPerNumber("3.14.15"));
        assertEquals("1.5+2.3", CalculatorUtils.sanitizeSingleDecimalPerNumber("1.5+2.3"));
        assertEquals("", CalculatorUtils.sanitizeSingleDecimalPerNumber(null));
    }

    @Test
    public void formatNumber() {
        assertEquals("0", CalculatorUtils.formatNumber(0));
        assertEquals("1,234", CalculatorUtils.formatNumber(1234));
    }
}
