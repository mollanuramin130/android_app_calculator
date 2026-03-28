package com.nuramin.calculator.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tax amounts for exclusive (add tax) and inclusive (extract tax) modes.
 * Mirrors {@link TaxCalculatorPanel} logic for testability.
 */
public final class TaxMath {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int WORK_SCALE = 10;

    private TaxMath() {}

    /**
     * Amount is pre-tax (exclusive). Returns [tax, totalWithTax] scaled to 2 decimal places.
     */
    public static BigDecimal[] computeExclusive(BigDecimal baseAmount, BigDecimal ratePercent) {
        BigDecimal tax = baseAmount.multiply(ratePercent).divide(HUNDRED, WORK_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = baseAmount.add(tax);
        return new BigDecimal[]{
                tax.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP)
        };
    }

    /**
     * Amount includes tax. Returns [tax, baseBeforeTax] scaled to 2 decimal places.
     */
    public static BigDecimal[] computeInclusive(BigDecimal totalInclTax, BigDecimal ratePercent) {
        BigDecimal divisor = BigDecimal.ONE.add(ratePercent.divide(HUNDRED, WORK_SCALE, RoundingMode.HALF_UP));
        BigDecimal base = totalInclTax.divide(divisor, WORK_SCALE, RoundingMode.HALF_UP);
        BigDecimal tax = totalInclTax.subtract(base);
        return new BigDecimal[]{
                tax.setScale(2, RoundingMode.HALF_UP),
                base.setScale(2, RoundingMode.HALF_UP)
        };
    }
}
