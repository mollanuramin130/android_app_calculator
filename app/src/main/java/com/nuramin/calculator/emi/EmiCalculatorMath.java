package com.nuramin.calculator.emi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EMI and amortization math shared by {@link EmiCalculatorPanel} and the schedule dialog.
 */
public final class EmiCalculatorMath {

    private EmiCalculatorMath() {}

    public static double roundMoney(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Monthly EMI for a fixed-rate loan (reducing balance).
     *
     * @param principal      loan amount
     * @param annualRatePct  nominal annual rate in percent (e.g. 10.5)
     * @param nMonths        tenure in months
     * @return EMI amount, or NaN if invalid
     */
    public static double monthlyEmi(double principal, double annualRatePct, int nMonths) {
        if (principal <= 0 || nMonths <= 0) {
            return Double.NaN;
        }
        double r = annualRatePct / 12.0 / 100.0;
        if (r < 0) {
            return Double.NaN;
        }
        if (r == 0) {
            return principal / nMonths;
        }
        double factor = Math.pow(1 + r, nMonths);
        if (!Double.isFinite(factor) || factor <= 1) {
            return Double.NaN;
        }
        return principal * r * factor / (factor - 1);
    }

    /**
     * Full reducing-balance schedule. Last month clears remaining principal; interest is on opening balance.
     */
    public static List<EmiScheduleRow> buildSchedule(double principal, double annualRatePct, int nMonths) {
        double emi = monthlyEmi(principal, annualRatePct, nMonths);
        if (Double.isNaN(emi) || nMonths <= 0) {
            return Collections.emptyList();
        }
        double r = annualRatePct / 12.0 / 100.0;
        List<EmiScheduleRow> rows = new ArrayList<>(nMonths);
        double balance = principal;
        for (int month = 1; month <= nMonths; month++) {
            double opening = balance;
            double interestPart;
            double principalPart;
            double payment;
            if (r <= 0) {
                interestPart = 0;
                if (month < nMonths) {
                    principalPart = roundMoney(emi);
                } else {
                    principalPart = roundMoney(opening);
                }
                payment = principalPart;
            } else if (month < nMonths) {
                interestPart = roundMoney(opening * r);
                principalPart = roundMoney(emi - interestPart);
                if (principalPart > opening) {
                    principalPart = roundMoney(opening);
                }
                payment = roundMoney(principalPart + interestPart);
            } else {
                principalPart = roundMoney(opening);
                interestPart = roundMoney(opening * r);
                payment = roundMoney(principalPart + interestPart);
            }
            double closing = month == nMonths ? 0 : roundMoney(opening - principalPart);
            balance = closing;
            rows.add(new EmiScheduleRow(month, opening, payment, principalPart, interestPart, closing));
        }
        return rows;
    }
}
