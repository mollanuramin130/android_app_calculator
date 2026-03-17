package com.nuramin.sunsetcoralcalculator.ai.calculator;

import androidx.annotation.NonNull;

import com.nuramin.sunsetcoralcalculator.ai.core.AIResult;
import com.nuramin.sunsetcoralcalculator.ai.core.ExpressionParser;

import java.util.Calendar;
import java.util.List;

/**
 * Routes to EMI / Age / Discount / GST utils. Does not call existing calculator classes.
 */
public final class CalculationRouter {

    @NonNull
    public AIResult calculate(@NonNull AIResult.Type type, @NonNull String input) {
        switch (type) {
            case EMI:
                return calculateEMI(input);
            case AGE:
                return calculateAge(input);
            case DISCOUNT:
            case GST:
                return calculateDiscountOrGst(input, type);
            default:
                return AIResult.unknown("Try: 5000 loan at 8% for 5 years");
        }
    }

    private AIResult calculateEMI(String input) {
        List<Double> numbers = ExpressionParser.extractNumbers(input);
        Double ratePct = ExpressionParser.extractPercentage(input);
        int months = ExpressionParser.extractMonths(input);
        if (months == 0) {
            int years = ExpressionParser.extractYears(input);
            if (years > 0) months = years * 12;
        }
        double principal = 0;
        if (!numbers.isEmpty()) {
            principal = numbers.get(0);
            if (input.toLowerCase().contains("lakh") || input.toLowerCase().contains("lac")) principal *= 100_000;
            else if (input.toLowerCase().contains("crore") || input.toLowerCase().contains("cr")) principal *= 10_000_000;
            else if (input.toLowerCase().contains("k")) principal *= 1_000;
        }
        if (ratePct == null && numbers.size() >= 2) ratePct = numbers.get(1);
        if (months == 0 && numbers.size() >= 3) months = numbers.get(2).intValue();
        if (months == 0 && numbers.size() >= 2) months = numbers.get(numbers.size() - 1).intValue() * 12;

        if (principal <= 0 || months <= 0) {
            return AIResult.unknown("Example: 5 lakh loan at 8% for 5 years");
        }
        double rate = ratePct != null ? ratePct : 8;
        Double emi = EMIUtil.calculateEMI(principal, rate, months);
        if (emi == null) return AIResult.unknown("Check loan amount and tenure.");

        double total = EMIUtil.totalPayment(emi, months);
        double interest = EMIUtil.totalInterest(total, principal);
        String explanation = String.format("Based on %s loan at %.1f%% for %d months",
                EMIUtil.formatCurrency(principal), rate, months);
        return new AIResult(AIResult.Type.EMI, "EMI Calculator", EMIUtil.formatMonthly(emi), explanation, true);
    }

    private AIResult calculateAge(String input) {
        List<Double> numbers = ExpressionParser.extractNumbers(input);
        if (numbers.size() < 3) {
            return AIResult.unknown("Try: age from 15 March 1990");
        }
        int day = numbers.get(0).intValue();
        int month = numbers.size() >= 2 ? numbers.get(1).intValue() : 1;
        int year = numbers.size() >= 3 ? numbers.get(2).intValue() : 2000;
        if (day > 31) { year = day; day = numbers.size() > 1 ? numbers.get(1).intValue() : 1; month = numbers.size() > 2 ? numbers.get(2).intValue() : 1; }
        if (month > 12) { int t = month; month = day; day = t; }
        Calendar now = Calendar.getInstance();
        String ageStr = AgeUtil.ageBetween(year, month, day, now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        String explanation = AgeUtil.formatAgeExplanation(year, month, day);
        return new AIResult(AIResult.Type.AGE, "Age Calculator", ageStr, explanation, true);
    }

    private AIResult calculateDiscountOrGst(String input, AIResult.Type type) {
        List<Double> numbers = ExpressionParser.extractNumbers(input);
        Double pct = ExpressionParser.extractPercentage(input);
        if (numbers.isEmpty()) return AIResult.unknown("Try: 1000 with 18% GST");
        double amount = numbers.get(0);
        double percent = pct != null ? pct : (numbers.size() >= 2 ? numbers.get(1) : 18);
        if (type == AIResult.Type.GST) {
            double gst = DiscountUtil.gstAmount(amount, percent);
            double total = DiscountUtil.priceWithGst(amount, percent);
            String explanation = String.format("Base ₹%,.0f + %.1f%% GST", amount, percent);
            return new AIResult(AIResult.Type.GST, "GST Calculator", DiscountUtil.formatCurrency(total), explanation, true);
        }
        double after = DiscountUtil.priceAfterDiscount(amount, percent);
        double saved = DiscountUtil.discountAmount(amount, percent);
        String explanation = String.format("%.1f%% off ₹%,.0f — you save %s", percent, amount, DiscountUtil.formatCurrency(saved));
        return new AIResult(AIResult.Type.DISCOUNT, "Discount Calculator", DiscountUtil.formatCurrency(after), explanation, true);
    }
}
