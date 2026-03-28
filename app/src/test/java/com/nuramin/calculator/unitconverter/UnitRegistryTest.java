package com.nuramin.calculator.unitconverter;

import com.nuramin.calculator.unitconverter.exceptions.DuplicateUnitException;
import com.nuramin.calculator.unitconverter.exceptions.UnitNotFoundException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class UnitRegistryTest {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    @Test
    public void normalize_trimsAndLowercases() {
        assertEquals("meter", UnitRegistry.normalize("  Meter "));
        assertEquals("", UnitRegistry.normalize(null));
    }

    @Test
    public void register_andGet_roundTrip() throws DuplicateUnitException, UnitNotFoundException {
        UnitRegistry reg = new UnitRegistry();
        reg.register("testmeter", new UnitDefinition(
                "Test Meter", "tm", "testcat",
                v -> v,
                v -> v,
                true));
        assertEquals("Test Meter", reg.get("testmeter").getName());
        assertEquals("Test Meter", reg.get("TestMeter").getName());
    }

    @Test
    public void register_duplicate_throws() throws DuplicateUnitException {
        UnitRegistry reg = new UnitRegistry();
        UnitDefinition def = new UnitDefinition("A", "a", "c", v -> v, v -> v, true);
        reg.register("a", def);
        assertThrows(DuplicateUnitException.class, () -> reg.register("A", def));
    }

    @Test
    public void get_unknown_throws() {
        UnitRegistry reg = new UnitRegistry();
        assertThrows(UnitNotFoundException.class, () -> reg.get("nope"));
    }

    @Test
    public void getUnitsForCategory_filters() throws DuplicateUnitException {
        UnitRegistry reg = new UnitRegistry();
        reg.register("u1", new UnitDefinition("U1", "u1", "length", v -> v.multiply(BigDecimal.TEN, MC), v -> v.divide(BigDecimal.TEN, MC), true));
        reg.register("u2", new UnitDefinition("U2", "u2", "length", v -> v, v -> v, false));
        reg.register("w1", new UnitDefinition("W1", "w1", "weight", v -> v, v -> v, true));
        assertTrue(reg.getUnitsForCategory("length").contains("u1"));
        assertTrue(reg.getUnitsForCategory("length").contains("u2"));
        assertTrue(reg.getUnitsForCategory("weight").contains("w1"));
    }
}
