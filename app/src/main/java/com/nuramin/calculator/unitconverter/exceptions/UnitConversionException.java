package com.nuramin.calculator.unitconverter.exceptions;

/** Base typed exception for all unit-conversion failures. */
public class UnitConversionException extends Exception {
    public UnitConversionException(String message) {
        super(message);
    }

    public UnitConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
