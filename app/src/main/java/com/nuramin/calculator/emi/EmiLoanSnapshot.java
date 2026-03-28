package com.nuramin.calculator.emi;

/**
 * Last successful EMI calculation inputs (for schedule / export).
 */
public final class EmiLoanSnapshot {

    public final double principal;
    public final double annualRatePct;
    public final int months;

    public EmiLoanSnapshot(double principal, double annualRatePct, int months) {
        this.principal = principal;
        this.annualRatePct = annualRatePct;
        this.months = months;
    }
}
