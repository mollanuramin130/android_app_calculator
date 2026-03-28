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
import com.nuramin.calculator.util.ConverterUiHelper;
import com.nuramin.calculator.util.LocaleFormatManager;

import java.util.Locale;

/**
 * Discount / Percentage Calculator. Main discount from % of original; optional extra discount
 * subtracts from the price after that discount. Requires original price and discount % to calculate.
 */
public final class DiscountCalculatorPanel {

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;

        EditText originalEt = panel.findViewById(R.id.discount_original);
        EditText percentEt = panel.findViewById(R.id.discount_percent);
        EditText extraEt = panel.findViewById(R.id.discount_extra_discount);
        ImageButton originalPlus = panel.findViewById(R.id.discount_original_plus);
        ImageButton originalMinus = panel.findViewById(R.id.discount_original_minus);
        ImageButton percentPlus = panel.findViewById(R.id.discount_percent_plus);
        ImageButton percentMinus = panel.findViewById(R.id.discount_percent_minus);
        ImageButton extraPlus = panel.findViewById(R.id.discount_extra_plus);
        ImageButton extraMinus = panel.findViewById(R.id.discount_extra_minus);
        Button calculateBtn = panel.findViewById(R.id.discount_calculate_btn);
        MaterialCardView resultCard = panel.findViewById(R.id.discount_result_card);
        TextView mainValue = panel.findViewById(R.id.discount_main_value);
        TextView youSave = panel.findViewById(R.id.discount_you_save);
        TextView summaryOriginal = panel.findViewById(R.id.discount_summary_original);
        TextView summaryPct = panel.findViewById(R.id.discount_summary_pct);
        TextView summaryOff = panel.findViewById(R.id.discount_summary_off);
        MaterialCardView breakdownCard = panel.findViewById(R.id.discount_breakdown_card);
        TextView breakdownValOriginal = panel.findViewById(R.id.discount_breakdown_value_original);
        TextView breakdownValDiscount = panel.findViewById(R.id.discount_breakdown_value_discount);
        TextView breakdownValExtra = panel.findViewById(R.id.discount_breakdown_value_extra);
        TextView breakdownValFinal = panel.findViewById(R.id.discount_breakdown_value_final);

        if (resultCard != null) {
            resultCard.setVisibility(View.GONE);
        }
        if (breakdownCard != null) {
            breakdownCard.setVisibility(View.GONE);
        }

        setIncrementDecrement(panel, originalEt, originalPlus, originalMinus, 1.0, 1.0, 10000.0);
        setIncrementDecrement(panel, percentEt, percentPlus, percentMinus, 1.0, 0.0, 100.0);
        setIncrementDecrement(panel, extraEt, extraPlus, extraMinus, 1.0, 0.0, 100000.0);

        if (calculateBtn != null && resultCard != null && mainValue != null && youSave != null
                && summaryOriginal != null && summaryPct != null && summaryOff != null
                && breakdownCard != null && breakdownValOriginal != null && breakdownValDiscount != null
                && breakdownValExtra != null && breakdownValFinal != null) {
            calculateBtn.setOnClickListener(v -> {
                if (isBlank(originalEt != null ? originalEt.getText() : null)) {
                    Snackbar.make(panel, R.string.discount_error_original_required, Snackbar.LENGTH_SHORT).show();
                    requestFocus(originalEt);
                    return;
                }
                if (isBlank(percentEt != null ? percentEt.getText() : null)) {
                    Snackbar.make(panel, R.string.discount_error_percent_required, Snackbar.LENGTH_SHORT).show();
                    requestFocus(percentEt);
                    return;
                }

                double original = LocaleFormatManager.parseLocalizedNumber(
                        originalEt != null ? originalEt.getText() : null, Double.NaN);
                double percent = LocaleFormatManager.parseLocalizedNumber(
                        percentEt != null ? percentEt.getText() : null, Double.NaN);
                if (!Double.isFinite(original) || original <= 0) {
                    Snackbar.make(panel, R.string.discount_error_invalid, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (!Double.isFinite(percent) || percent < 0 || percent > 100) {
                    Snackbar.make(panel, R.string.discount_error_invalid, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                double extraInput = 0;
                if (extraEt != null && !isBlank(extraEt.getText())) {
                    extraInput = LocaleFormatManager.parseLocalizedNumber(extraEt.getText(), 0);
                    if (extraInput < 0) {
                        Snackbar.make(panel, R.string.discount_error_invalid, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                }

                double mainDiscountPreview = round2(original * (percent / 100.0));
                if (mainDiscountPreview > original) {
                    mainDiscountPreview = original;
                }
                double afterMainPreview = round2(original - mainDiscountPreview);
                if (extraInput > afterMainPreview + 1e-9) {
                    Snackbar.make(panel, R.string.discount_error_extra_too_large, Snackbar.LENGTH_SHORT).show();
                    requestFocus(extraEt);
                    return;
                }

                DiscountCalculator.Result calc = DiscountCalculator.compute(original, percent, extraInput);
                double mainDiscount = calc.mainDiscountAmount;
                double extraDiscount = calc.extraDiscountAmount;
                double totalDiscount = calc.totalDiscountAmount;
                double finalPrice = calc.finalPrice;
                double mainPct = calc.mainPercent;

                String origStr = LocaleFormatManager.formatCurrency(panel.getContext(), original);
                String finalStr = LocaleFormatManager.formatCurrency(panel.getContext(), finalPrice);
                String mainOffStr = LocaleFormatManager.formatCurrency(panel.getContext(), mainDiscount);
                String extraStr = LocaleFormatManager.formatCurrency(panel.getContext(), extraDiscount);
                String totalStr = LocaleFormatManager.formatCurrency(panel.getContext(), totalDiscount);
                String pctStr = String.format(Locale.getDefault(), "%.1f", mainPct);

                mainValue.setText(finalStr);
                mainValue.setTextColor(ContextCompat.getColor(panel.getContext(), R.color.discount_positive));
                youSave.setText(panel.getContext().getString(R.string.discount_you_save, totalStr));
                summaryOriginal.setText(origStr);
                summaryPct.setText("-" + pctStr + "%");
                summaryOff.setText("-" + totalStr);

                breakdownValOriginal.setText(origStr);
                breakdownValDiscount.setText(String.format(Locale.getDefault(), "%s%% · %s", pctStr, mainOffStr));
                breakdownValExtra.setText(extraDiscount > 0 ? "-" + extraStr : extraStr);
                breakdownValFinal.setText(finalStr);
                breakdownValFinal.setTextColor(ContextCompat.getColor(panel.getContext(), R.color.discount_positive));

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

    private static boolean isBlank(CharSequence s) {
        return s == null || s.toString().trim().isEmpty();
    }

    private static void requestFocus(@Nullable EditText et) {
        if (et != null) {
            et.requestFocus();
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
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

    private static double parseDouble(CharSequence s, double def) {
        if (s == null || s.toString().trim().isEmpty()) return def;
        return LocaleFormatManager.parseLocalizedNumber(s, def);
    }

    private static String cleanDecimal(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.valueOf(d);
    }
}
