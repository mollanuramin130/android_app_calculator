package com.nuramin.calculator.discount;

/**
 * Pure discount math aligned with {@link DiscountCalculatorPanel}.
 * Main discount from percentage; optional extra discount subtracts from the price after the main discount.
 */
public final class DiscountCalculator {

    public static final class Result {
        /** Discount from percentage of original. */
        public final double mainDiscountAmount;
        /** Additional amount off after main discount. */
        public final double extraDiscountAmount;
        /** mainDiscountAmount + extraDiscountAmount */
        public final double totalDiscountAmount;
        /** Original − mainDiscount − extra */
        public final double finalPrice;
        /** Price after main discount only (before extra). */
        public final double priceAfterMainDiscount;
        /** Entered main discount percent (0–100). */
        public final double mainPercent;

        public Result(double mainDiscountAmount, double extraDiscountAmount, double totalDiscountAmount,
                double finalPrice, double priceAfterMainDiscount, double mainPercent) {
            this.mainDiscountAmount = mainDiscountAmount;
            this.extraDiscountAmount = extraDiscountAmount;
            this.totalDiscountAmount = totalDiscountAmount;
            this.finalPrice = finalPrice;
            this.priceAfterMainDiscount = priceAfterMainDiscount;
            this.mainPercent = mainPercent;
        }
    }

    private DiscountCalculator() {}

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    /**
     * @param original       price before any discount (&gt; 0)
     * @param percent        main discount percent (0–100)
     * @param extraDiscount  optional extra amount off after main discount (≥ 0, caller validates ≤ price after main)
     */
    public static Result compute(double original, double percent, double extraDiscount) {
        double mainDiscount = round2(original * (percent / 100.0));
        if (mainDiscount > original) {
            mainDiscount = original;
        }
        double afterMain = round2(original - mainDiscount);
        double extra = round2(Math.max(0, extraDiscount));
        double finalPrice = round2(afterMain - extra);
        double total = round2(mainDiscount + extra);
        double mainPct = round2(percent);
        if (!Double.isFinite(finalPrice) || !Double.isFinite(total)) {
            double safeO = Double.isFinite(original) && original > 0 ? original : 0;
            return new Result(0, 0, 0, safeO, safeO, 0);
        }
        return new Result(mainDiscount, extra, total, finalPrice, afterMain, mainPct);
    }
}
