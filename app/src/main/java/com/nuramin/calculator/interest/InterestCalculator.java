package com.nuramin.calculator.interest;

/**
 * Pure interest math (simple / compound annual) shared by the panel and unit tests.
 */
public final class InterestCalculator {

    private InterestCalculator() {}

    /** Converts time input to years (spinner may use months). */
    public static double yearsForInterest(double timeValue, boolean timeInMonths) {
        return timeInMonths ? timeValue / 12.0 : timeValue;
    }

    public static double simpleInterest(double principal, double annualRatePercent, double years) {
        return principal * annualRatePercent * years / 100.0;
    }

    public static double simpleTotalAmount(double principal, double annualRatePercent, double years) {
        return principal + simpleInterest(principal, annualRatePercent, years);
    }

    /** Annual compounding once per year (standard for "compound interest" with time in years). */
    public static double compoundTotalAmount(double principal, double annualRatePercent, double years) {
        return principal * Math.pow(1 + annualRatePercent / 100.0, years);
    }

    public static double compoundInterest(double principal, double annualRatePercent, double years) {
        return compoundTotalAmount(principal, annualRatePercent, years) - principal;
    }
}
