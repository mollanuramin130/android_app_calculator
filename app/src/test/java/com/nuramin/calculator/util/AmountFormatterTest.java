package com.nuramin.calculator.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for AmountFormatter: number formatting, grouping, scientific, index mapping.
 */
@RunWith(JUnit4.class)
public class AmountFormatterTest {

    @Test
    public void format_integers() {
        assertEquals("0", AmountFormatter.format(0));
        assertEquals("1", AmountFormatter.format(1));
        assertEquals("1,234", AmountFormatter.format(1234));
        assertEquals("1,234,567", AmountFormatter.format(1234567));
    }

    @Test
    public void format_decimals() {
        assertEquals("0.008", AmountFormatter.format(0.008));
        assertEquals("0.002", AmountFormatter.format(0.002));
        assertEquals("0.00056", AmountFormatter.format(0.00056));
        assertEquals("1.5", AmountFormatter.format(1.5));
    }

    @Test
    public void format_verySmall_scientific() {
        String s = AmountFormatter.format(0.00056 * 0.00564);
        assertNotNull(s);
        assertEquals("Error", AmountFormatter.format(Double.NaN));
        assertEquals("Error", AmountFormatter.format(Double.POSITIVE_INFINITY));
    }

    @Test
    public void format_veryLarge_scientific() {
        String s = AmountFormatter.format(1e10);
        assertNotNull(s);
        assertTrue(s.contains("E") || s.contains("e"));
    }

    @Test
    public void stripGrouping() {
        assertEquals("", AmountFormatter.stripGrouping(null));
        assertEquals("1234", AmountFormatter.stripGrouping("1,234"));
        assertEquals("1234567", AmountFormatter.stripGrouping("1,234,567"));
        assertEquals("1000.50", AmountFormatter.stripGrouping("1,000.50"));
    }

    @Test
    public void parse() {
        assertEquals(0.0, AmountFormatter.parse(null), 1e-9);
        assertEquals(0.0, AmountFormatter.parse(""), 1e-9);
        assertEquals(1234.0, AmountFormatter.parse("1,234"), 1e-9);
        assertEquals(1000.5, AmountFormatter.parse("1,000.50"), 1e-9);
    }

    @Test
    public void formatExpressionForDisplay() {
        assertEquals("", AmountFormatter.formatExpressionForDisplay(null));
        assertEquals("", AmountFormatter.formatExpressionForDisplay(""));
        assertEquals("1,234+5", AmountFormatter.formatExpressionForDisplay("1234+5"));
        assertEquals("2.006", AmountFormatter.formatExpressionForDisplay("2.006"));
    }

    @Test
    public void rawIndexToFormattedIndex() {
        // formatted "1,234": rawIndex = chars before cursor; return = formatted position after that many raw chars
        String formatted = new String(new char[] { '1', ',', '2', '3', '4' });
        String raw = "1234";
        assertEquals(0, AmountFormatter.rawIndexToFormattedIndex(formatted, raw, 0));
        assertEquals(1, AmountFormatter.rawIndexToFormattedIndex(formatted, raw, 1)); // after "1"
        assertEquals(3, AmountFormatter.rawIndexToFormattedIndex(formatted, raw, 2)); // after "12" -> position 3
        assertEquals(5, AmountFormatter.rawIndexToFormattedIndex(formatted, raw, 4));
    }

    @Test
    public void formattedIndexToRawIndex() {
        // formatted "1,234": formattedIndex 2 covers indices 0,1 -> only '1' is raw -> raw count 1
        String formatted = new String(new char[] { '1', ',', '2', '3', '4' });
        assertEquals(0, AmountFormatter.formattedIndexToRawIndex(formatted, 0));
        assertEquals(1, AmountFormatter.formattedIndexToRawIndex(formatted, 1));
        assertEquals(1, AmountFormatter.formattedIndexToRawIndex(formatted, 2)); // indices 0,1: one raw char
        assertEquals(2, AmountFormatter.formattedIndexToRawIndex(formatted, 3));
        assertEquals(4, AmountFormatter.formattedIndexToRawIndex(formatted, 5));
    }

    @Test
    public void rawAndFormattedIndexRoundTrip() {
        String formatted = new String(new char[] { '1', ',', '2', '3', '4' });
        String raw = "1234";
        for (int r = 0; r <= raw.length(); r++) {
            int f = AmountFormatter.rawIndexToFormattedIndex(formatted, raw, r);
            assertEquals("rawIndex " + r + " -> formatted " + f + " should round-trip", r, AmountFormatter.formattedIndexToRawIndex(formatted, f));
        }
    }

    @Test
    public void formatWithGroupingOnly() {
        assertEquals("Error", AmountFormatter.formatWithGroupingOnly(Double.NaN));
        assertEquals("1,234", AmountFormatter.formatWithGroupingOnly(1234));
        assertEquals("1,234.56", AmountFormatter.formatWithGroupingOnly(1234.56));
    }

    @Test
    public void formatLong() {
        assertEquals("0", AmountFormatter.format(0L));
        assertEquals("1,234,567", AmountFormatter.format(1234567L));
    }

    @Test
    public void stripGrouping_spaceAndNbsp() {
        assertEquals("1234", AmountFormatter.stripGrouping("1 234"));
        assertEquals("1234", AmountFormatter.stripGrouping("1\u00A0234"));
    }

    @Test
    public void rawIndexToFormattedIndex_edgeCases() {
        assertEquals(0, AmountFormatter.rawIndexToFormattedIndex("1,234", "1234", 0));
        assertEquals(5, AmountFormatter.rawIndexToFormattedIndex("1,234", "1234", 4));
        assertEquals(0, AmountFormatter.rawIndexToFormattedIndex(null, "12", 1));
        assertEquals(0, AmountFormatter.rawIndexToFormattedIndex("1,2", null, 1));
    }

    @Test
    public void formattedIndexToRawIndex_edgeCases() {
        assertEquals(0, AmountFormatter.formattedIndexToRawIndex(null, 2));
        assertEquals(0, AmountFormatter.formattedIndexToRawIndex("1,234", 0));
        assertEquals(4, AmountFormatter.formattedIndexToRawIndex("1,234", 99));
    }
}
