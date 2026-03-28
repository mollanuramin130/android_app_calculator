package com.nuramin.calculator.unitconverter;

import android.util.Log;

import com.nuramin.calculator.unitconverter.exceptions.DuplicateUnitException;
import com.nuramin.calculator.unitconverter.exceptions.IncompatibleUnitsException;
import com.nuramin.calculator.unitconverter.exceptions.InternalConversionErrorException;
import com.nuramin.calculator.unitconverter.exceptions.InvalidValueException;
import com.nuramin.calculator.unitconverter.exceptions.SameUnitException;
import com.nuramin.calculator.unitconverter.exceptions.UnitConversionException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stateless conversion facade:
 * Input -> Validate -> Fetch units -> Check category -> To base -> To target -> Precision -> Return.
 */
public final class UnitConversionService {
    private static final String TAG = "UnitConversionService";
    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);
    private static final UnitRegistry REGISTRY = new UnitRegistry();
    private static volatile PrecisionHandler precisionHandler = new PrecisionHandler(6, RoundingMode.HALF_UP);

    static {
        try {
            registerDefaults();
        } catch (DuplicateUnitException ignored) {
        }
    }

    private UnitConversionService() {}

    public static double convert(double value, String fromUnit, String toUnit) throws UnitConversionException {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new InvalidValueException("Value must be a finite number.");
        }
        BigDecimal out = convert(BigDecimal.valueOf(value), fromUnit, toUnit);
        return out.doubleValue();
    }

    public static BigDecimal convert(BigDecimal value, String fromUnit, String toUnit) throws UnitConversionException {
        try {
            validate(value, fromUnit, toUnit);

            String fromKey = UnitRegistry.normalize(fromUnit);
            String toKey = UnitRegistry.normalize(toUnit);
            if (fromKey.equals(toKey)) {
                throw new SameUnitException("Source and target unit are the same: " + fromUnit);
            }

            UnitDefinition from = REGISTRY.get(fromKey);
            UnitDefinition to = REGISTRY.get(toKey);

            String fromCategory = UnitRegistry.normalize(from.getCategory());
            String toCategory = UnitRegistry.normalize(to.getCategory());
            if (!fromCategory.equals(toCategory)) {
                throw new IncompatibleUnitsException(
                        "Incompatible units: " + fromUnit + " (" + from.getCategory() + ") and "
                                + toUnit + " (" + to.getCategory() + ")"
                );
            }

            BigDecimal base = from.toBase(value.round(MATH_CONTEXT));
            BigDecimal converted = to.fromBase(base.round(MATH_CONTEXT));
            return precisionHandler.apply(converted);
        } catch (UnitConversionException known) {
            Log.w(TAG, "Conversion failed: " + known.getMessage());
            throw known;
        } catch (Exception unknown) {
            Log.e(TAG, "Unexpected conversion error", unknown);
            throw new InternalConversionErrorException("Internal conversion error for " + fromUnit + " -> " + toUnit, unknown);
        }
    }

    public static synchronized void register(String unitKey, UnitDefinition unit) throws DuplicateUnitException {
        REGISTRY.register(unitKey, unit);
    }

    public static synchronized void setPrecision(int decimals) {
        precisionHandler = new PrecisionHandler(Math.max(0, decimals), RoundingMode.HALF_UP);
    }

    public static List<String> getCategories() {
        List<String> normalized = REGISTRY.getCategories();
        List<String> pretty = new ArrayList<>();
        for (String c : normalized) {
            pretty.add(capitalize(c));
        }
        return pretty;
    }

    public static List<String> getUnitsForCategory(String categoryDisplayName) {
        return REGISTRY.getUnitsForCategory(UnitRegistry.normalize(categoryDisplayName));
    }

    private static void validate(BigDecimal value, String fromUnit, String toUnit) throws InvalidValueException {
        if (value == null) throw new InvalidValueException("Value cannot be null.");
        if (fromUnit == null || fromUnit.trim().isEmpty()) {
            throw new InvalidValueException("Source unit cannot be empty.");
        }
        if (toUnit == null || toUnit.trim().isEmpty()) {
            throw new InvalidValueException("Target unit cannot be empty.");
        }
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) return "";
        return input.substring(0, 1).toUpperCase(Locale.US) + input.substring(1);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v, MATH_CONTEXT);
    }

    private static void registerDefaults() throws DuplicateUnitException {
        // Length (base: meter)
        register("meter", new UnitDefinition("Meter", "m", "length", v -> v, v -> v, true));
        register("kilometer", new UnitDefinition("Kilometer", "km", "length",
                v -> v.multiply(bd("1000"), MATH_CONTEXT),
                v -> v.divide(bd("1000"), MATH_CONTEXT), false));
        register("centimeter", new UnitDefinition("Centimeter", "cm", "length",
                v -> v.divide(bd("100"), MATH_CONTEXT),
                v -> v.multiply(bd("100"), MATH_CONTEXT), false));
        register("millimeter", new UnitDefinition("Millimeter", "mm", "length",
                v -> v.divide(bd("1000"), MATH_CONTEXT),
                v -> v.multiply(bd("1000"), MATH_CONTEXT), false));
        register("inch", new UnitDefinition("Inch", "in", "length",
                v -> v.multiply(bd("0.0254"), MATH_CONTEXT),
                v -> v.divide(bd("0.0254"), MATH_CONTEXT), false));
        register("foot", new UnitDefinition("Foot", "ft", "length",
                v -> v.multiply(bd("0.3048"), MATH_CONTEXT),
                v -> v.divide(bd("0.3048"), MATH_CONTEXT), false));
        register("mile", new UnitDefinition("Mile", "mi", "length",
                v -> v.multiply(bd("1609.344"), MATH_CONTEXT),
                v -> v.divide(bd("1609.344"), MATH_CONTEXT), false));

        // Weight (base: kilogram)
        register("kilogram", new UnitDefinition("Kilogram", "kg", "weight", v -> v, v -> v, true));
        register("gram", new UnitDefinition("Gram", "g", "weight",
                v -> v.divide(bd("1000"), MATH_CONTEXT),
                v -> v.multiply(bd("1000"), MATH_CONTEXT), false));
        register("milligram", new UnitDefinition("Milligram", "mg", "weight",
                v -> v.divide(bd("1000000"), MATH_CONTEXT),
                v -> v.multiply(bd("1000000"), MATH_CONTEXT), false));
        register("pound", new UnitDefinition("Pound", "lb", "weight",
                v -> v.multiply(bd("0.45359237"), MATH_CONTEXT),
                v -> v.divide(bd("0.45359237"), MATH_CONTEXT), false));

        // Time (base: second)
        register("second", new UnitDefinition("Second", "s", "time", v -> v, v -> v, true));
        register("minute", new UnitDefinition("Minute", "min", "time",
                v -> v.multiply(bd("60"), MATH_CONTEXT),
                v -> v.divide(bd("60"), MATH_CONTEXT), false));
        register("hour", new UnitDefinition("Hour", "h", "time",
                v -> v.multiply(bd("3600"), MATH_CONTEXT),
                v -> v.divide(bd("3600"), MATH_CONTEXT), false));
        register("day", new UnitDefinition("Day", "d", "time",
                v -> v.multiply(bd("86400"), MATH_CONTEXT),
                v -> v.divide(bd("86400"), MATH_CONTEXT), false));

        // Volume (base: liter)
        register("liter", new UnitDefinition("Liter", "L", "volume", v -> v, v -> v, true));
        register("milliliter", new UnitDefinition("Milliliter", "mL", "volume",
                v -> v.divide(bd("1000"), MATH_CONTEXT),
                v -> v.multiply(bd("1000"), MATH_CONTEXT), false));
        register("gallon", new UnitDefinition("Gallon", "gal", "volume",
                v -> v.multiply(bd("3.785411784"), MATH_CONTEXT),
                v -> v.divide(bd("3.785411784"), MATH_CONTEXT), false));

        // Area (base: square meter)
        register("square meter", new UnitDefinition("Square Meter", "m²", "area", v -> v, v -> v, true));
        register("square kilometer", new UnitDefinition("Square Kilometer", "km²", "area",
                v -> v.multiply(bd("1000000"), MATH_CONTEXT),
                v -> v.divide(bd("1000000"), MATH_CONTEXT), false));
        register("hectare", new UnitDefinition("Hectare", "ha", "area",
                v -> v.multiply(bd("10000"), MATH_CONTEXT),
                v -> v.divide(bd("10000"), MATH_CONTEXT), false));

        // Temperature (base: kelvin) - non-linear handled via affine equations.
        register("kelvin", new UnitDefinition("Kelvin", "K", "temperature", v -> v, v -> v, true));
        register("celsius", new UnitDefinition("Celsius", "°C", "temperature",
                v -> v.add(bd("273.15"), MATH_CONTEXT),
                v -> v.subtract(bd("273.15"), MATH_CONTEXT), false));
        register("fahrenheit", new UnitDefinition("Fahrenheit", "°F", "temperature",
                v -> v.subtract(bd("32"), MATH_CONTEXT)
                        .multiply(bd("5"), MATH_CONTEXT)
                        .divide(bd("9"), MATH_CONTEXT)
                        .add(bd("273.15"), MATH_CONTEXT),
                v -> v.subtract(bd("273.15"), MATH_CONTEXT)
                        .multiply(bd("9"), MATH_CONTEXT)
                        .divide(bd("5"), MATH_CONTEXT)
                        .add(bd("32"), MATH_CONTEXT), false));
    }
}
