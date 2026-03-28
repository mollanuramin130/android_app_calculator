package com.nuramin.calculator.emi;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EmiCalculatorMathTest {

    @Test
    public void monthlyEmi_matchesStandardFormula() {
        double emi = EmiCalculatorMath.monthlyEmi(1_000_000, 10, 120);
        assertTrue(emi > 0);
        assertTrue(Double.isFinite(emi));
    }

    @Test
    public void schedule_principalPartsSumToLoan() {
        double p = 500_000;
        double rate = 9.5;
        int n = 60;
        List<EmiScheduleRow> rows = EmiCalculatorMath.buildSchedule(p, rate, n);
        assertEquals(n, rows.size());
        double sumPrincipal = 0;
        for (EmiScheduleRow row : rows) {
            sumPrincipal += row.principalPart;
        }
        assertEquals(p, sumPrincipal, 2.0);
        assertEquals(0, rows.get(n - 1).closingBalance, 0.01);
    }

    @Test
    public void zeroRate_equalPrincipalEachMonth() {
        List<EmiScheduleRow> rows = EmiCalculatorMath.buildSchedule(120_000, 0, 12);
        assertEquals(12, rows.size());
        for (EmiScheduleRow row : rows) {
            assertEquals(0, row.interestPart, 0.01);
        }
    }

    @Test
    public void invalidInputs_emptySchedule() {
        assertTrue(EmiCalculatorMath.buildSchedule(-1, 10, 12).isEmpty());
        assertTrue(EmiCalculatorMath.buildSchedule(1000, 10, 0).isEmpty());
    }

    @Test
    public void monthlyEmi_nanWhenInvalid() {
        assertTrue(Double.isNaN(EmiCalculatorMath.monthlyEmi(0, 10, 12)));
    }
}
