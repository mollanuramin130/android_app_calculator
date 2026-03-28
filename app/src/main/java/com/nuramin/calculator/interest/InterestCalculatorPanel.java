package com.nuramin.calculator.interest;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.util.ConverterUiHelper;
import com.nuramin.calculator.util.LocaleFormatManager;

/**
 * Interest Calculator: simple/compound, principal, rate, time (in Years or Months); calculate; result and breakdown.
 * Nav and overflow are handled by the main activity toolbar.
 */
public final class InterestCalculatorPanel {

    private static final int UNIT_YEARS = 0;
    private static final int UNIT_MONTHS = 1;

    /** Empty fields use these defaults (matches EditText hints in interest_content). */
    static final double DEFAULT_PRINCIPAL = 50000;
    static final double DEFAULT_RATE_PCT = 8.5;
    static final double DEFAULT_TIME = 5;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;

        RadioGroup interestType = panel.findViewById(R.id.interest_type);
        EditText principalEt = panel.findViewById(R.id.interest_principal);
        EditText rateEt = panel.findViewById(R.id.interest_rate);
        EditText timeEt = panel.findViewById(R.id.interest_time);
        Spinner timeUnitSpinner = panel.findViewById(R.id.interest_time_unit);
        Button calculateBtn = panel.findViewById(R.id.interest_calculate);
        View resultCard = panel.findViewById(R.id.interest_result_card);
        TextView resultTv = panel.findViewById(R.id.interest_result);
        TextView totalAmountTv = panel.findViewById(R.id.interest_total_amount);
        TextView breakdownPrincipalTv = panel.findViewById(R.id.interest_breakdown_principal);
        TextView breakdownInterestLabelTv = panel.findViewById(R.id.interest_breakdown_interest_label);
        TextView breakdownInterestTv = panel.findViewById(R.id.interest_breakdown_interest);

        if (timeUnitSpinner != null) {
            String[] units = new String[]{
                    panel.getContext().getString(R.string.int_years),
                    panel.getContext().getString(R.string.int_months)
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(panel.getContext(),
                    android.R.layout.simple_spinner_item, units);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            timeUnitSpinner.setAdapter(adapter);
            timeUnitSpinner.setSelection(UNIT_YEARS);
        }

        Button viewBreakdownBtn = panel.findViewById(R.id.interest_view_breakdown);
        if (viewBreakdownBtn != null) {
            viewBreakdownBtn.setOnClickListener(v -> InterestBreakdownExporter.showMenu(panel, v));
        }

        if (calculateBtn != null && resultCard != null && resultTv != null && totalAmountTv != null
                && breakdownPrincipalTv != null && breakdownInterestLabelTv != null && breakdownInterestTv != null
                && principalEt != null && rateEt != null && timeEt != null && interestType != null) {
            calculateBtn.setOnClickListener(v -> {
                double principal = parseDouble(principalEt.getText(), DEFAULT_PRINCIPAL);
                double ratePct = parseDouble(rateEt.getText(), DEFAULT_RATE_PCT);
                double timeValue = parseDouble(timeEt.getText(), DEFAULT_TIME);
                boolean isMonths = timeUnitSpinner != null && timeUnitSpinner.getSelectedItemPosition() == UNIT_MONTHS;
                double yearsForCalc = InterestCalculator.yearsForInterest(timeValue, isMonths);
                if (principal <= 0 || yearsForCalc <= 0) {
                    resultCard.setVisibility(View.GONE);
                    return;
                }
                boolean isCompound = interestType.getCheckedRadioButtonId() == R.id.interest_compound;
                double interest;
                double totalAmount;
                if (isCompound) {
                    interest = InterestCalculator.compoundInterest(principal, ratePct, yearsForCalc);
                    totalAmount = InterestCalculator.compoundTotalAmount(principal, ratePct, yearsForCalc);
                } else {
                    interest = InterestCalculator.simpleInterest(principal, ratePct, yearsForCalc);
                    totalAmount = InterestCalculator.simpleTotalAmount(principal, ratePct, yearsForCalc);
                }
                String interestAmount = LocaleFormatManager.formatCurrency(panel.getContext(), interest);
                String totalAmountCurrency = LocaleFormatManager.formatCurrency(panel.getContext(), totalAmount);
                String principalAmount = LocaleFormatManager.formatCurrency(panel.getContext(), principal);
                resultTv.setText(interestAmount);
                totalAmountTv.setText(panel.getContext().getString(R.string.int_total_amount, totalAmountCurrency));
                breakdownPrincipalTv.setText(principalAmount);
                breakdownInterestLabelTv.setText(panel.getContext().getString(
                        isCompound ? R.string.int_compound_interest_label : R.string.int_simple_interest_label));
                breakdownInterestTv.setText(interestAmount);
                resultCard.setVisibility(View.VISIBLE);
                ConverterUiHelper.hideSoftKeyboard(panel);
                ConverterUiHelper.scrollToShowResult(panel, resultCard);
            });
        }

    }

    private static double parseDouble(CharSequence s, double def) {
        return LocaleFormatManager.parseLocalizedNumber(s, def);
    }

}
