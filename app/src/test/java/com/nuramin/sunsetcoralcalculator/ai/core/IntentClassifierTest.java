package com.nuramin.sunsetcoralcalculator.ai.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class IntentClassifierTest {

    @Test
    public void classify_emiKeywords() {
        assertEquals(AIResult.Type.EMI, IntentClassifier.classify("calculate EMI for 5 lakh"));
        assertEquals(AIResult.Type.EMI, IntentClassifier.classify("loan at 8%"));
    }

    @Test
    public void classify_ageKeywords() {
        assertEquals(AIResult.Type.AGE, IntentClassifier.classify("age from birthday 1990"));
        assertEquals(AIResult.Type.AGE, IntentClassifier.classify("what is my DOB"));
    }

    @Test
    public void classify_gstBeforeDiscount() {
        assertEquals(AIResult.Type.GST, IntentClassifier.classify("add 18% gst to 1000"));
    }

    @Test
    public void classify_discount() {
        assertEquals(AIResult.Type.DISCOUNT, IntentClassifier.classify("10% discount on 500"));
    }

    @Test
    public void classify_unknown() {
        assertEquals(AIResult.Type.UNKNOWN, IntentClassifier.classify("hello world"));
        assertEquals(AIResult.Type.UNKNOWN, IntentClassifier.classify(""));
    }
}
