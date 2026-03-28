package com.nuramin.sunsetcoralcalculator.ai.calculator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class DiscountUtilTest {

    @Test
    public void priceAfterDiscount() {
        assertEquals(80.0, DiscountUtil.priceAfterDiscount(100, 20), 0.001);
        assertEquals(100.0, DiscountUtil.priceAfterDiscount(100, 0), 0.001);
    }

    @Test
    public void discountAmount() {
        assertEquals(20.0, DiscountUtil.discountAmount(100, 20), 0.001);
    }

    @Test
    public void gstAmount() {
        assertEquals(18.0, DiscountUtil.gstAmount(100, 18), 0.001);
    }

    @Test
    public void priceWithGst() {
        assertEquals(118.0, DiscountUtil.priceWithGst(100, 18), 0.001);
    }
}
