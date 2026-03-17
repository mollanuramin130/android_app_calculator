package com.nuramin.calculator.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class HistoryEntryTest {

    @Test
    public void constructorTwoArgs() {
        HistoryEntry e = new HistoryEntry("1+2", "3");
        assertEquals("1+2", e.getExpression());
        assertEquals("3", e.getResult());
        assertNull(e.getTimestamp());
    }

    @Test
    public void constructorThreeArgs() {
        Long ts = 1234567890L;
        HistoryEntry e = new HistoryEntry("5*6", "30", ts);
        assertEquals("5*6", e.getExpression());
        assertEquals("30", e.getResult());
        assertEquals(ts, e.getTimestamp());
    }

    @Test
    public void constructorThreeArgs_nullTimestamp() {
        HistoryEntry e = new HistoryEntry("a", "b", null);
        assertNull(e.getTimestamp());
    }
}
