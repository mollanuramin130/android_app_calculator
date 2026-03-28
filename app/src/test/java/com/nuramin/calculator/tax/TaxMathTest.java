package com.nuramin.calculator.tax;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class TaxMathTest {

    @Test
    public void exclusive_tenPercent() {
        BigDecimal[] p = TaxMath.computeExclusive(new BigDecimal("100.00"), new BigDecimal("10"));
        assertEquals(new BigDecimal("10.00"), p[0]);
        assertEquals(new BigDecimal("110.00"), p[1]);
    }

    @Test
    public void inclusive_extractsBase() {
        // 110 inclusive at 10% => base 100, tax 10
        BigDecimal[] p = TaxMath.computeInclusive(new BigDecimal("110.00"), new BigDecimal("10"));
        assertEquals(new BigDecimal("10.00"), p[0]);
        assertEquals(new BigDecimal("100.00"), p[1]);
    }

    @Test
    public void exclusive_zeroRate() {
        BigDecimal[] p = TaxMath.computeExclusive(new BigDecimal("50"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), p[0]);
        assertEquals(new BigDecimal("50.00"), p[1]);
    }

    @Test
    public void inclusive_zeroRate() {
        BigDecimal[] p = TaxMath.computeInclusive(new BigDecimal("99.99"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), p[0]);
        assertEquals(new BigDecimal("99.99"), p[1]);
    }
}
