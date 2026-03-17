package com.nuramin.calculator.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for ExprParser: expression evaluation, edge cases, invalid input.
 */
@RunWith(JUnit4.class)
public class ExprParserTest {

    private static double parse(String s) {
        return new ExprParser(s).parse();
    }

    private static double parse(String s, boolean degrees) {
        return new ExprParser(s, degrees).parse();
    }

    private static void assertThrows(Runnable r) {
        try {
            r.run();
            throw new AssertionError("Expected exception");
        } catch (RuntimeException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void basicArithmetic() {
        assertEquals(5.0, parse("2+3"), 1e-9);
        assertEquals(-1.0, parse("2-3"), 1e-9);
        assertEquals(6.0, parse("2*3"), 1e-9);
        assertEquals(2.5, parse("5/2"), 1e-9);
        assertEquals(8.0, parse("2^3"), 1e-9);
    }

    @Test
    public void orderOfOperations() {
        assertEquals(14.0, parse("2+3*4"), 1e-9);
        assertEquals(20.0, parse("(2+3)*4"), 1e-9);
        assertEquals(10.0, parse("2*3+4"), 1e-9);
    }

    @Test
    public void divisionByZero_throws() {
        assertThrows(() -> parse("1/0"));
        assertThrows(() -> parse("5/(2-2)"));
    }

    @Test
    public void factorial() {
        assertEquals(1.0, parse("0!"), 1e-9);
        assertEquals(1.0, parse("1!"), 1e-9);
        assertEquals(24.0, parse("4!"), 1e-9);
        assertEquals(120.0, parse("5!"), 1e-9);
    }

    @Test
    public void factorialNegative_throws() {
        assertThrows(() -> parse("-1!"));
    }

    @Test
    public void factorialNonInteger_throws() {
        assertThrows(() -> parse("2.5!"));
    }

    @Test
    public void sqrt() {
        assertEquals(3.0, parse("sqrt(9)"), 1e-9);
        assertEquals(2.0, parse("sqrt(4)"), 1e-9);
    }

    @Test
    public void sqrtNegative_throws() {
        assertThrows(() -> parse("sqrt(-1)"));
    }

    @Test
    public void sinCosTan_degrees() {
        assertEquals(0.5, parse("sin(30)", true), 1e-6);
        assertEquals(1.0, parse("cos(0)", true), 1e-9);
        assertTrue(Math.abs(parse("tan(45)", true) - 1.0) < 1e-6);
    }

    @Test
    public void lnAndLog_positive() {
        assertEquals(1.0, parse("ln(e)"), 1e-9);
        assertTrue(Math.abs(parse("log(100)") - 2.0) < 1e-9);
    }

    @Test
    public void lnNonPositive_throws() {
        assertThrows(() -> parse("ln(0)"));
        assertThrows(() -> parse("ln(-1)"));
        assertThrows(() -> parse("log(0)"));
    }

    @Test
    public void constants() {
        assertTrue(Math.abs(parse("π") - Math.PI) < 1e-9);
        assertTrue(Math.abs(parse("e") - Math.E) < 1e-9);
        assertTrue(Math.abs(parse("pi") - Math.PI) < 1e-9);
    }

    @Test
    public void decimalNumbers() {
        assertEquals(3.14, parse("3.14"), 1e-9);
        assertEquals(0.5, parse("0.5"), 1e-9);
        assertEquals(100.0, parse("1e2"), 1e-9);
    }

    @Test
    public void nullOrEmpty_throwsOrZero() {
        assertThrows(() -> new ExprParser(null).parse());
        assertThrows(() -> parse(""));
    }

    @Test
    public void invalidCharacter_throws() {
        assertThrows(() -> parse("2@3"));
        assertThrows(() -> parse("2+"));
    }

    @Test
    public void unclosedParenthesis_throws() {
        assertThrows(() -> parse("(2+3"));
        assertThrows(() -> parse("2+(3*4"));
    }

    @Test
    public void powerRightAssociative() {
        assertEquals(512.0, parse("2^3^2"), 1e-9); // 2^(3^2) = 2^9 = 512
    }
}
