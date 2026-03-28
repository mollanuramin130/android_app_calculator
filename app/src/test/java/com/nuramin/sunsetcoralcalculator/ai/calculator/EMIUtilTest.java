package com.nuramin.sunsetcoralcalculator.ai.calculator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class EMIUtilTest {

    @Test
    public void calculateEMI_knownValues() {
        Double emi = EMIUtil.calculateEMI(100_000, 12, 12);
        assertNotNull(emi);
        assertEquals(8884.88, emi, 0.01);
    }

    @Test
    public void calculateEMI_zeroRate_dividesPrincipal() {
        Double emi = EMIUtil.calculateEMI(12_000, 0, 12);
        assertNotNull(emi);
        assertEquals(1000.0, emi, 0.001);
    }

    @Test
    public void calculateEMI_invalid_returnsNull() {
        assertNull(EMIUtil.calculateEMI(0, 8, 60));
        assertNull(EMIUtil.calculateEMI(1000, 8, 0));
        assertNull(EMIUtil.calculateEMI(1000, -5, 12));
    }

    @Test
    public void totalPayment_andInterest() {
        double emi = 1000;
        int months = 12;
        assertEquals(12000.0, EMIUtil.totalPayment(emi, months), 0.01);
        assertEquals(2000.0, EMIUtil.totalInterest(12000, 10000), 0.01);
    }
}
