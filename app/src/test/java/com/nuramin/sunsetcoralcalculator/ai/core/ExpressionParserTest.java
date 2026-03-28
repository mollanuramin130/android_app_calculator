package com.nuramin.sunsetcoralcalculator.ai.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class ExpressionParserTest {

    @Test
    public void extractNumbers_basic() {
        List<Double> n = ExpressionParser.extractNumbers("loan 500000 at 8% for 60 months");
        assertEquals(500000.0, n.get(0), 0.001);
    }

    @Test
    public void extractPercentage() {
        assertEquals(8.0, ExpressionParser.extractPercentage("rate 8% yearly"), 0.001);
        assertNull(ExpressionParser.extractPercentage("no percent here"));
    }

    @Test
    public void extractYears() {
        assertEquals(5, ExpressionParser.extractYears("5 years tenure"));
    }

    @Test
    public void parseAmount_lakh() {
        assertEquals(500_000.0, ExpressionParser.parseAmount("5 lakh"), 0.001);
        assertEquals(10_000_000.0, ExpressionParser.parseAmount("1 crore"), 0.001);
    }

    @Test
    public void extractNumbers_fallbackDigits() {
        List<Double> n = ExpressionParser.extractNumbers("100 200");
        assertEquals(2, n.size());
        assertEquals(100.0, n.get(0), 0.001);
    }
}
