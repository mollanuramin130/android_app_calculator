package com.nuramin.calculator.emi;

/**
 * One row of a reducing-balance EMI amortization schedule.
 */
public final class EmiScheduleRow {

    /** 1-based month index. */
    public final int month;
    public final double openingBalance;
    /** Actual installment this month (equals EMI for all but possibly adjusted last month). */
    public final double payment;
    public final double principalPart;
    public final double interestPart;
    public final double closingBalance;

    public EmiScheduleRow(
            int month,
            double openingBalance,
            double payment,
            double principalPart,
            double interestPart,
            double closingBalance) {
        this.month = month;
        this.openingBalance = openingBalance;
        this.payment = payment;
        this.principalPart = principalPart;
        this.interestPart = interestPart;
        this.closingBalance = closingBalance;
    }
}
