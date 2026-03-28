package com.nuramin.calculator.unitconverter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Controls rounding and precision safety for all conversion outputs. */
public final class PrecisionHandler {
    private final int scale;
    private final RoundingMode roundingMode;

    public PrecisionHandler() {
        this(6, RoundingMode.HALF_UP);
    }

    public PrecisionHandler(int scale, RoundingMode roundingMode) {
        this.scale = Math.max(0, scale);
        this.roundingMode = roundingMode == null ? RoundingMode.HALF_UP : roundingMode;
    }

    public BigDecimal apply(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.setScale(scale, roundingMode).stripTrailingZeros();
    }
}
