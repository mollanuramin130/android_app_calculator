package com.nuramin.sunsetcoralcalculator.ai.calculator;

import com.nuramin.sunsetcoralcalculator.ai.core.AIResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class CalculationRouterTest {

    private final CalculationRouter router = new CalculationRouter();

    @Test
    public void calculate_emi_success() {
        AIResult r = router.calculate(AIResult.Type.EMI,
                "EMI for 100000 loan at 12% for 12 months");
        assertEquals(AIResult.Type.EMI, r.getType());
        assertTrue(r.isSuccess());
        assertTrue(r.getResultText().length() > 0);
    }

    @Test
    public void calculate_discount_success() {
        AIResult r = router.calculate(AIResult.Type.DISCOUNT, "discount 1000 20%");
        assertEquals(AIResult.Type.DISCOUNT, r.getType());
        assertTrue(r.isSuccess());
    }

    @Test
    public void calculate_gst_success() {
        AIResult r = router.calculate(AIResult.Type.GST, "gst on 1000 at 18%");
        assertEquals(AIResult.Type.GST, r.getType());
        assertTrue(r.isSuccess());
    }
}
