package com.nuramin.calculator.discount;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class DiscountCalculatorTest {

    @Test
    public void percentOnly_25off100() {
        DiscountCalculator.Result r = DiscountCalculator.compute(100, 25, 0);
        assertEquals(25.0, r.mainDiscountAmount, 0.001);
        assertEquals(0.0, r.extraDiscountAmount, 0.001);
        assertEquals(25.0, r.totalDiscountAmount, 0.001);
        assertEquals(75.0, r.finalPrice, 0.001);
        assertEquals(75.0, r.priceAfterMainDiscount, 0.001);
        assertEquals(25.0, r.mainPercent, 0.001);
    }

    @Test
    public void withExtra_subtractsFromAfterMain() {
        DiscountCalculator.Result r = DiscountCalculator.compute(100, 25, 5);
        assertEquals(25.0, r.mainDiscountAmount, 0.001);
        assertEquals(5.0, r.extraDiscountAmount, 0.001);
        assertEquals(30.0, r.totalDiscountAmount, 0.001);
        assertEquals(70.0, r.finalPrice, 0.001);
        assertEquals(75.0, r.priceAfterMainDiscount, 0.001);
    }

    @Test
    public void mainDiscount_cappedAtOriginal() {
        DiscountCalculator.Result r = DiscountCalculator.compute(100, 150, 0);
        assertEquals(100.0, r.mainDiscountAmount, 0.001);
        assertEquals(0.0, r.finalPrice, 0.001);
    }

    @Test
    public void zeroPercent_noMainDiscount() {
        DiscountCalculator.Result r = DiscountCalculator.compute(50, 0, 0);
        assertEquals(0.0, r.mainDiscountAmount, 0.001);
        assertEquals(50.0, r.finalPrice, 0.001);
    }
}
