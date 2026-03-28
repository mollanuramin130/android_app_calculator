package com.nuramin.sunsetcoralcalculator.ai.calculator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class AgeUtilTest {

    @Test
    public void ageBetween_exactBirthday() {
        String s = AgeUtil.ageBetween(1990, 3, 15, 2020, 3, 15);
        assertTrue(s.contains("30 year"));
        assertTrue(s.contains("0 day") || s.contains("0 days"));
    }

    @Test
    public void formatAgeExplanation_nonEmpty() {
        String s = AgeUtil.formatAgeExplanation(1990, 3, 15);
        assertTrue(s != null && s.length() > 0);
    }

    @Test
    public void ageBetween_dayPlural_oneDay() {
        String s = AgeUtil.ageBetween(1990, 3, 14, 2020, 3, 15);
        assertTrue(s.contains("1 day"));
    }
}
