package com.nuramin.calculator.currency;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class CurrencyConversionTest {

    @Test
    public void convertViaUsd_symmetric() {
        // USD 1 = 1 USD, EUR ~ 1.08 USD per EUR in sample — use abstract rates
        double from = 1.0;
        double to = 2.0;
        assertEquals(50.0, CurrencyConversion.convertViaUsd(100, from, to), 1e-9);
    }

    @Test
    public void unitsOfTargetPerOneSource() {
        assertEquals(2.0, CurrencyConversion.unitsOfTargetPerOneSource(1.0, 0.5), 1e-9);
    }

    @Test
    public void convertViaUsd_invalidAmount_nan() {
        assertTrue(Double.isNaN(CurrencyConversion.convertViaUsd(Double.NaN, 1, 1)));
        assertTrue(Double.isNaN(CurrencyConversion.convertViaUsd(Double.POSITIVE_INFINITY, 1, 1)));
    }

    @Test
    public void convertViaUsd_zeroTargetRate() {
        assertEquals(0.0, CurrencyConversion.convertViaUsd(100, 1.0, 0), 0);
    }

    @Test
    public void isBlankAmount() {
        assertTrue(CurrencyConversion.isBlankAmount(null));
        assertTrue(CurrencyConversion.isBlankAmount(""));
        assertTrue(CurrencyConversion.isBlankAmount("  "));
        assertTrue(CurrencyConversion.isBlankAmount(" , "));
    }

    @Test
    public void isValidAmount() {
        assertTrue(CurrencyConversion.isValidAmount("1"));
        assertTrue(CurrencyConversion.isValidAmount("1,234.5"));
        assertFalse(CurrencyConversion.isValidAmount("abc"));
        assertFalse(CurrencyConversion.isValidAmount(""));
    }

    @Test
    public void parseAmount() {
        assertEquals(100.5, CurrencyConversion.parseAmount("100.5"), 1e-9);
        assertEquals(1234.0, CurrencyConversion.parseAmount("1,234"), 1e-9);
    }
}
