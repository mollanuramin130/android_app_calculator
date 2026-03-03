package com.nuramin.calculator.interest;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.ConverterUiHelper;

/**
 * Interest Calculator: simple/compound, principal, rate, time; calculate; result and breakdown.
 * Nav and overflow are handled by the main activity toolbar.
 */
public final class InterestCalculatorPanel {

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;

        RadioGroup interestType = panel.findViewById(R.id.interest_type);
        EditText principalEt = panel.findViewById(R.id.interest_principal);
        EditText rateEt = panel.findViewById(R.id.interest_rate);
        EditText timeEt = panel.findViewById(R.id.interest_time);
        Button calculateBtn = panel.findViewById(R.id.interest_calculate);
        View resultCard = panel.findViewById(R.id.interest_result_card);
        TextView resultTv = panel.findViewById(R.id.interest_result);
        TextView totalAmountTv = panel.findViewById(R.id.interest_total_amount);
        TextView breakdownPrincipalTv = panel.findViewById(R.id.interest_breakdown_principal);
        TextView breakdownInterestLabelTv = panel.findViewById(R.id.interest_breakdown_interest_label);
        TextView breakdownInterestTv = panel.findViewById(R.id.interest_breakdown_interest);

        if (calculateBtn != null && resultCard != null && resultTv != null && totalAmountTv != null
                && breakdownPrincipalTv != null && breakdownInterestLabelTv != null && breakdownInterestTv != null
                && principalEt != null && rateEt != null && timeEt != null && interestType != null) {
            calculateBtn.setOnClickListener(v -> {
                double principal = parseDouble(principalEt.getText(), 50000);
                double ratePct = parseDouble(rateEt.getText(), 8.5);
                int years = parseInt(timeEt.getText(), 5);
                if (principal <= 0 || years <= 0) {
                    resultCard.setVisibility(View.GONE);
                    return;
                }
                boolean isCompound = interestType.getCheckedRadioButtonId() == R.id.interest_compound;
                double interest;
                double totalAmount;
                if (isCompound) {
                    totalAmount = principal * Math.pow(1 + ratePct / 100, years);
                    interest = totalAmount - principal;
                } else {
                    interest = principal * ratePct * years / 100;
                    totalAmount = principal + interest;
                }
                String rupee = panel.getContext().getString(R.string.int_rupee);
                resultTv.setText(rupee + " " + CalculatorUtils.formatNumber(interest));
                totalAmountTv.setText(panel.getContext().getString(R.string.int_total_amount, rupee + " " + CalculatorUtils.formatNumber(totalAmount)));
                breakdownPrincipalTv.setText(rupee + " " + CalculatorUtils.formatNumber(principal));
                breakdownInterestLabelTv.setText(panel.getContext().getString(
                        isCompound ? R.string.int_compound_interest_label : R.string.int_simple_interest_label));
                breakdownInterestTv.setText(rupee + " " + CalculatorUtils.formatNumber(interest));
                resultCard.setVisibility(View.VISIBLE);
                ConverterUiHelper.hideSoftKeyboard(panel);
                ConverterUiHelper.scrollToShowResult(panel, resultCard);
            });
        }

    }

    private static double parseDouble(CharSequence s, double def) {
        if (s == null) return def;
        try {
            return Double.parseDouble(s.toString().replace(",", "").trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parseInt(CharSequence s, int def) {
        if (s == null) return def;
        try {
            return Integer.parseInt(s.toString().replace(",", "").trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
