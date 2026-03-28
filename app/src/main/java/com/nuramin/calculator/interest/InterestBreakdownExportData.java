package com.nuramin.calculator.interest;

import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.nuramin.calculator.util.LocaleFormatManager;
import com.nuramin.sunsetcoralcalculator.R;

import java.util.Locale;

/**
 * Snapshot of Interest screen values for PDF / share / print.
 * Uses the same parsing and formulas as {@link InterestCalculatorPanel} so amounts match the UI.
 */
public final class InterestBreakdownExportData {

    private static final int UNIT_YEARS = 0;
    private static final int UNIT_MONTHS = 1;

    public final String interestModeLabel;
    public final String principalLine;
    public final String rateLine;
    public final String timeLine;
    public final String totalInterest;
    public final String totalAmountLine;
    public final String principalBreakdown;
    public final String interestLabel;
    public final String interestBreakdown;

    public InterestBreakdownExportData(
            String interestModeLabel,
            String principalLine,
            String rateLine,
            String timeLine,
            String totalInterest,
            String totalAmountLine,
            String principalBreakdown,
            String interestLabel,
            String interestBreakdown) {
        this.interestModeLabel = interestModeLabel;
        this.principalLine = principalLine;
        this.rateLine = rateLine;
        this.timeLine = timeLine;
        this.totalInterest = totalInterest;
        this.totalAmountLine = totalAmountLine;
        this.principalBreakdown = principalBreakdown;
        this.interestLabel = interestLabel;
        this.interestBreakdown = interestBreakdown;
    }

    @Nullable
    static InterestBreakdownExportData from(View panel) {
        View resultCard = panel.findViewById(R.id.interest_result_card);
        if (resultCard == null || resultCard.getVisibility() != View.VISIBLE) {
            return null;
        }
        RadioGroup interestType = panel.findViewById(R.id.interest_type);
        EditText principalEt = panel.findViewById(R.id.interest_principal);
        EditText rateEt = panel.findViewById(R.id.interest_rate);
        EditText timeEt = panel.findViewById(R.id.interest_time);
        Spinner timeUnitSpinner = panel.findViewById(R.id.interest_time_unit);

        if (principalEt == null || rateEt == null || timeEt == null || interestType == null) {
            return null;
        }

        android.content.Context ctx = panel.getContext();

        double principal = LocaleFormatManager.parseLocalizedNumber(
                principalEt.getText(), InterestCalculatorPanel.DEFAULT_PRINCIPAL);
        double ratePct = LocaleFormatManager.parseLocalizedNumber(
                rateEt.getText(), InterestCalculatorPanel.DEFAULT_RATE_PCT);
        double timeRaw = LocaleFormatManager.parseLocalizedNumber(
                timeEt.getText(), InterestCalculatorPanel.DEFAULT_TIME);
        boolean isMonths = timeUnitSpinner != null && timeUnitSpinner.getSelectedItemPosition() == UNIT_MONTHS;
        double yearsForCalc = InterestCalculator.yearsForInterest(timeRaw, isMonths);

        String unitLabel = "";
        if (timeUnitSpinner != null && timeUnitSpinner.getSelectedItem() != null) {
            unitLabel = timeUnitSpinner.getSelectedItem().toString();
        }

        boolean isCompound = interestType.getCheckedRadioButtonId() == R.id.interest_compound;
        String mode = ctx.getString(isCompound ? R.string.int_compound : R.string.int_simple);

        String principalFormatted = LocaleFormatManager.formatCurrency(ctx, principal);
        String rateLineStr = String.format(Locale.US, "%s %%", LocaleFormatManager.formatNumber(ratePct));
        String timeLineStr = LocaleFormatManager.formatNumber(timeRaw) + " " + unitLabel;

        double interest;
        double totalAmt;
        if (principal > 0 && yearsForCalc > 0) {
            if (isCompound) {
                interest = InterestCalculator.compoundInterest(principal, ratePct, yearsForCalc);
                totalAmt = InterestCalculator.compoundTotalAmount(principal, ratePct, yearsForCalc);
            } else {
                interest = InterestCalculator.simpleInterest(principal, ratePct, yearsForCalc);
                totalAmt = InterestCalculator.simpleTotalAmount(principal, ratePct, yearsForCalc);
            }
        } else {
            TextView resultTv = panel.findViewById(R.id.interest_result);
            TextView totalAmountTv = panel.findViewById(R.id.interest_total_amount);
            TextView bp = panel.findViewById(R.id.interest_breakdown_principal);
            TextView bil = panel.findViewById(R.id.interest_breakdown_interest_label);
            TextView bi = panel.findViewById(R.id.interest_breakdown_interest);
            if (resultTv == null || totalAmountTv == null || bp == null || bil == null || bi == null) {
                return null;
            }
            return new InterestBreakdownExportData(
                    mode,
                    principalFormatted,
                    rateLineStr,
                    timeLineStr,
                    textOrEmpty(resultTv),
                    textOrEmpty(totalAmountTv),
                    textOrEmpty(bp),
                    textOrEmpty(bil),
                    textOrEmpty(bi));
        }

        String interestStr = LocaleFormatManager.formatCurrency(ctx, interest);
        String totalStr = LocaleFormatManager.formatCurrency(ctx, totalAmt);
        String totalAmountLineStr = ctx.getString(R.string.int_total_amount, totalStr);

        return new InterestBreakdownExportData(
                mode,
                principalFormatted,
                rateLineStr,
                timeLineStr,
                interestStr,
                totalAmountLineStr,
                principalFormatted,
                ctx.getString(isCompound ? R.string.int_compound_interest_label : R.string.int_simple_interest_label),
                interestStr);
    }

    private static String textOrEmpty(@Nullable TextView tv) {
        if (tv == null || tv.getText() == null) {
            return "";
        }
        return tv.getText().toString();
    }
}
