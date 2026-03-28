package com.nuramin.calculator.unitconverter;

import com.nuramin.calculator.unitconverter.exceptions.IncompatibleUnitsException;
import com.nuramin.calculator.unitconverter.exceptions.InvalidValueException;
import com.nuramin.calculator.unitconverter.exceptions.SameUnitException;
import com.nuramin.calculator.unitconverter.exceptions.UnitConversionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class UnitConversionServiceTest {

    @Test
    public void convert_kilometerToMeter() throws UnitConversionException {
        double m = UnitConversionService.convert(1, "kilometer", "meter");
        assertEquals(1000.0, m, 0.0001);
    }

    @Test
    public void convert_celsiusToFahrenheit_freezing() throws UnitConversionException {
        double f = UnitConversionService.convert(0, "celsius", "fahrenheit");
        assertEquals(32.0, f, 0.01);
    }

    @Test
    public void convert_gramToKilogram() throws UnitConversionException {
        double kg = UnitConversionService.convert(1000, "gram", "kilogram");
        assertEquals(1.0, kg, 0.0001);
    }

    @Test
    public void convert_sameUnit_throws() {
        assertThrows(SameUnitException.class,
                () -> UnitConversionService.convert(5, "meter", "meter"));
    }

    @Test
    public void convert_incompatibleCategories_throws() {
        assertThrows(IncompatibleUnitsException.class,
                () -> UnitConversionService.convert(1, "meter", "kilogram"));
    }

    @Test
    public void convert_nan_throws() {
        assertThrows(InvalidValueException.class,
                () -> UnitConversionService.convert(Double.NaN, "meter", "kilometer"));
    }

    @Test
    public void getCategories_notEmpty() {
        assertTrue(UnitConversionService.getCategories().size() > 0);
    }

    @Test
    public void getUnitsForCategory_length_containsMeter() {
        assertTrue(UnitConversionService.getUnitsForCategory("length").contains("meter"));
    }
}
