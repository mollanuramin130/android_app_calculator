package com.nuramin.calculator.dto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class CalculationResultTest {

    @Test
    public void getters() {
        CalculationResult r = new CalculationResult(42.5, "42.5");
        assertEquals(42.5, r.getValue(), 1e-9);
        assertEquals("42.5", r.getFormattedResult());
    }

    @Test
    public void zeroAndFormatted() {
        CalculationResult r = new CalculationResult(0, "0");
        assertEquals(0.0, r.getValue(), 1e-9);
        assertEquals("0", r.getFormattedResult());
    }
}
