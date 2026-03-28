package com.nuramin.calculator.interest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class InterestCalculatorTest {

    @Test
    public void yearsForInterest_months() {
        assertEquals(1.0, InterestCalculator.yearsForInterest(12, true), 1e-9);
        assertEquals(0.5, InterestCalculator.yearsForInterest(6, true), 1e-9);
    }

    @Test
    public void yearsForInterest_years() {
        assertEquals(5.0, InterestCalculator.yearsForInterest(5, false), 1e-9);
    }

    @Test
    public void simpleInterest_known() {
        // 10000 at 5% for 2 years => 1000
        assertEquals(1000.0, InterestCalculator.simpleInterest(10000, 5, 2), 1e-6);
        assertEquals(11000.0, InterestCalculator.simpleTotalAmount(10000, 5, 2), 1e-6);
    }

    @Test
    public void compoundInterest_known() {
        // 1000 at 10% for 1 year => 100
        assertEquals(100.0, InterestCalculator.compoundInterest(1000, 10, 1), 1e-6);
        assertEquals(1100.0, InterestCalculator.compoundTotalAmount(1000, 10, 1), 1e-6);
    }

    @Test
    public void compoundInterest_nonNegativePrincipal() {
        double i = InterestCalculator.compoundInterest(0, 8, 5);
        assertEquals(0.0, i, 1e-9);
    }

    @Test
    public void simpleInterest_zeroRate() {
        assertEquals(0.0, InterestCalculator.simpleInterest(5000, 0, 10), 1e-9);
        assertTrue(InterestCalculator.simpleTotalAmount(5000, 0, 10) > 0);
    }
}
