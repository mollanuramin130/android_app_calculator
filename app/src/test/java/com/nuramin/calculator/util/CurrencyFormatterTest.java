package com.nuramin.calculator.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CurrencyFormatterTest {

    @Test
    public void formatAmount_noSymbolOrCode() {
        String s = CurrencyFormatter.formatAmount(10000.5);
        assertFalse(s.contains("USD"));
        assertFalse(s.contains("$"));
        assertFalse(s.contains("₹"));
    }

    @Test
    public void formatAmount_twoArgIgnoresCode() {
        String s = CurrencyFormatter.formatAmount(1, "USD");
        assertFalse(s.contains("USD"));
    }
}
