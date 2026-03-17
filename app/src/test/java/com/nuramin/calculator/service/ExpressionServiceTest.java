package com.nuramin.calculator.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class ExpressionServiceTest {

    @Test
    public void evaluate_valid() {
        assertEquals(5.0, ExpressionService.evaluate("2+3"), 1e-9);
        assertEquals(6.0, ExpressionService.evaluate("2*3"), 1e-9);
        assertEquals(10.0, ExpressionService.evaluate("  2 + 8  "), 1e-9);
    }

    @Test
    public void evaluate_null() {
        assertNull(ExpressionService.evaluate(null));
    }

    @Test
    public void evaluate_empty() {
        assertNull(ExpressionService.evaluate(""));
        assertNull(ExpressionService.evaluate("   "));
    }

    @Test
    public void evaluate_invalid() {
        assertNull(ExpressionService.evaluate("2+"));
        assertNull(ExpressionService.evaluate("abc"));
    }

    @Test
    public void formatResult() {
        assertEquals("0", ExpressionService.formatResult(0));
        assertEquals("1,234", ExpressionService.formatResult(1234));
    }
}
