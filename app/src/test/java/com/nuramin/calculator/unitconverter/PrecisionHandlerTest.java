package com.nuramin.calculator.unitconverter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class PrecisionHandlerTest {

    @Test
    public void apply_roundsToScale() {
        PrecisionHandler h = new PrecisionHandler(4, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("3.1416"), h.apply(new BigDecimal("3.14159265")));
    }

    @Test
    public void apply_nullReturnsZero() {
        PrecisionHandler h = new PrecisionHandler();
        assertEquals(BigDecimal.ZERO, h.apply(null));
    }

    @Test
    public void apply_stripsTrailingZeros() {
        PrecisionHandler h = new PrecisionHandler(6, RoundingMode.HALF_UP);
        BigDecimal out = h.apply(new BigDecimal("2.500000"));
        assertEquals("2.5", out.toPlainString());
    }
}
