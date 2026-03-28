package com.nuramin.sunsetcoralcalculator.ai.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class AIEngineTest {

    private final AIEngine engine = new AIEngine();

    @Test
    public void process_empty_unknown() {
        AIResult r = engine.process("");
        assertEquals(AIResult.Type.UNKNOWN, r.getType());
        assertFalse(r.isSuccess());
    }

    @Test
    public void process_emi_returnsSuccess() {
        AIResult r = engine.process("EMI for 100000 loan at 12% for 12 months");
        assertEquals(AIResult.Type.EMI, r.getType());
        assertTrue(r.isSuccess());
    }

    @Test
    public void process_plainNumbers_unknown() {
        AIResult r = engine.process("12345");
        assertEquals(AIResult.Type.UNKNOWN, r.getType());
    }
}
