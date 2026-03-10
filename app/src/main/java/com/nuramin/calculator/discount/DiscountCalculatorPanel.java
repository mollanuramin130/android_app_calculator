package com.nuramin.calculator.discount;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.ConverterUiHelper;

/**
 * Discount / Percentage Calculator. Reuses common TopBar; content only below it.
 * Formula: percentage-based discountAmount = original * (percent/100), finalPrice = original - discountAmount;
 * or direct amount: finalPrice = original - discountAmount, percent = (discountAmount/original)*100.
 * If both percent and amount filled, prioritize percentage.
 */
public final class DiscountCalculatorPanel {

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;

        EditText originalEt = panel.findViewById(R.id.discount_original);
        EditText percentEt = panel.findViewById(R.id.discount_percent);
        EditText amountEt = panel.findViewById(R.id.discount_amount_input);
        ImageButton originalPlus = panel.findViewById(R.id.discount_original_plus);
        ImageButton originalMinus = panel.findViewById(R.id.discount_original_minus);
        ImageButton percentPlus = panel.findViewById(R.id.discount_percent_plus);
        ImageButton percentMinus = panel.findViewById(R.id.discount_percent_minus);
        ImageButton amountPlus = panel.findViewById(R.id.discount_amount_plus);
        ImageButton amountMinus = panel.findViewById(R.id.discount_amount_minus);
        Button calculateBtn = panel.findViewById(R.id.discount_calculate_btn);
        MaterialCardView resultCard = panel.findViewById(R.id.discount_result_card);
        TextView mainValue = panel.findViewById(R.id.discount_main_value);
        TextView youSave = panel.findViewById(R.id.discount_you_save);
        TextView summaryOriginal = panel.findViewById(R.id.discount_summary_original);
        TextView summaryPct = panel.findViewById(R.id.discount_summary_pct);
        TextView summaryOff = panel.findViewById(R.id.discount_summary_off);
        MaterialCardView breakdownCard = panel.findViewById(R.id.discount_breakdown_card);
        TextView breakdownLine1 = panel.findViewById(R.id.discount_breakdown_line1);
        TextView breakdownLine2 = panel.findViewById(R.id.discount_breakdown_line2);
        TextView breakdownLine3 = panel.findViewById(R.id.discount_breakdown_line3);
        TextView breakdownFinal = panel.findViewById(R.id.discount_breakdown_final);

        setIncrementDecrement(panel, originalEt, originalPlus, originalMinus, 1.0, 1.0, 10000.0);
        setIncrementDecrement(panel, percentEt, percentPlus, percentMinus, 1.0, 0.0, 100.0);
        setIncrementDecrement(panel, amountEt, amountPlus, amountMinus, 1.0, 0.0, 100000.0);

        if (calculateBtn != null && resultCard != null && mainValue != null && youSave != null
                && summaryOriginal != null && summaryPct != null && summaryOff != null
                && breakdownCard != null && breakdownLine1 != null && breakdownLine2 != null
                && breakdownLine3 != null && breakdownFinal != null) {
            calculateBtn.setOnClickListener(v -> {
                double original = parseDouble(originalEt != null ? originalEt.getText() : null, 100);
                double percent = parseDouble(percentEt != null ? percentEt.getText() : null, 25);
                double amountInput = parseDouble(amountEt != null ? amountEt.getText() : null, -1);

                if (original <= 0) {
                    Snackbar.make(panel, R.string.discount_error_invalid, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (percent < 0 || percent > 100) {
                    Snackbar.make(panel, R.string.discount_error_invalid, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (amountInput >= 0 && amountInput > original) {
                    Snackbar.make(panel, R.string.discount_error_invalid, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                double discountAmount;
                double finalPrice;
                double effectivePercent;

                boolean usePercent = (percentEt != null && percentEt.getText() != null && !percentEt.getText().toString().trim().isEmpty());
                if (amountInput >= 0 && !usePercent) {
                    effectivePercent = (amountInput / original) * 100;
                    discountAmount = amountInput;
                    finalPrice = original - discountAmount;
                } else {
                    effectivePercent = percent;
                    discountAmount = round2(original * (percent / 100));
                    if (discountAmount > original) discountAmount = original;
                    finalPrice = original - discountAmount;
                }

                finalPrice = round2(finalPrice);
                discountAmount = round2(discountAmount);
                effectivePercent = round2(effectivePercent);

                String origStr = CalculatorUtils.formatNumber(original);
                String finalStr = CalculatorUtils.formatNumber(finalPrice);
                String offStr = CalculatorUtils.formatNumber(discountAmount);
                String pctStr = String.format("%.1f", effectivePercent);

                mainValue.setText(finalStr);
                mainValue.setTextColor(ContextCompat.getColor(panel.getContext(), R.color.discount_positive));
                youSave.setText(panel.getContext().getString(R.string.discount_you_save, offStr));
                summaryOriginal.setText(origStr);
                summaryPct.setText("-" + pctStr + "%");
                summaryOff.setText("-" + offStr);

                breakdownLine1.setText(panel.getContext().getString(R.string.discount_breakdown_original, origStr));
                breakdownLine2.setText(panel.getContext().getString(R.string.discount_breakdown_discount, pctStr, "-" + offStr));
                breakdownLine3.setText(panel.getContext().getString(R.string.discount_breakdown_price_off, "-" + offStr));
                breakdownFinal.setText(panel.getContext().getString(R.string.discount_breakdown_final, finalStr));
                breakdownFinal.setTextColor(ContextCompat.getColor(panel.getContext(), R.color.discount_positive));

                resultCard.setVisibility(View.VISIBLE);
                breakdownCard.setVisibility(View.VISIBLE);
                breakdownCard.setAlpha(0f);
                breakdownCard.animate().alpha(1f).setDuration(250).start();

                mainValue.setScaleX(0.95f);
                mainValue.setScaleY(0.95f);
                ObjectAnimator sx = ObjectAnimator.ofFloat(mainValue, View.SCALE_X, 0.95f, 1f);
                ObjectAnimator sy = ObjectAnimator.ofFloat(mainValue, View.SCALE_Y, 0.95f, 1f);
                sx.setDuration(200);
                sy.setDuration(200);
                AnimatorSet set = new AnimatorSet();
                set.playTogether(sx, sy);
                set.start();

                ConverterUiHelper.hideSoftKeyboard(panel);
                ConverterUiHelper.scrollToShowResult(panel, resultCard);
            });
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static double parseDouble(CharSequence s, double def) {
        if (s == null || s.toString().trim().isEmpty()) return def;
        try {
            return Double.parseDouble(s.toString().replace(",", "").trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void setIncrementDecrement(View panel, EditText et, ImageButton plus, ImageButton minus, double step, double min, double max) {
        if (et == null) return;
        if (plus != null) {
            plus.setOnClickListener(v -> {
                double val = parseDouble(et.getText(), min);
                val = Math.min(max, val + step);
                et.setText(cleanDecimal(val));
            });
        }
        if (minus != null) {
            minus.setOnClickListener(v -> {
                double val = parseDouble(et.getText(), min);
                val = Math.max(min, val - step);
                et.setText(cleanDecimal(val));
            });
        }
    }

    private static String cleanDecimal(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.valueOf(d);
    }
}
