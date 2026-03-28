package com.nuramin.calculator.unitconverter;

import java.math.BigDecimal;

/** Metadata + base normalization functions for a single unit. */
public final class UnitDefinition {
    public interface Converter {
        BigDecimal apply(BigDecimal input);
    }

    private final String name;
    private final String symbol;
    private final String category;
    private final Converter toBase;
    private final Converter fromBase;
    private final boolean baseUnit;

    public UnitDefinition(
            String name,
            String symbol,
            String category,
            Converter toBase,
            Converter fromBase,
            boolean baseUnit
    ) {
        this.name = name;
        this.symbol = symbol;
        this.category = category;
        this.toBase = toBase;
        this.fromBase = fromBase;
        this.baseUnit = baseUnit;
    }

    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public String getCategory() { return category; }
    public boolean isBaseUnit() { return baseUnit; }
    public BigDecimal toBase(BigDecimal value) { return toBase.apply(value); }
    public BigDecimal fromBase(BigDecimal value) { return fromBase.apply(value); }
}
