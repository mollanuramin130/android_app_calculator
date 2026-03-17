package com.nuramin.calculator.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class AppConstantsTest {

    @Test
    public void maxHistoryItems() {
        assertEquals(50, AppConstants.MAX_HISTORY_ITEMS);
    }
}
