package com.nuramin.calculator.tax;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.calculator.util.ConverterUiHelper;
import com.nuramin.calculator.util.LocaleFormatManager;
import com.nuramin.sunsetcoralcalculator.R;

import java.math.BigDecimal;

/**
 * Tax calculator panel.
 * Supports two modes:
 * 1) Add tax to amount (exclusive tax)
 * 2) Extract tax from total (inclusive tax)
 */
public final class TaxCalculatorPanel {

    private TaxCalculatorPanel() {}

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;

        RadioGroup modeGroup = panel.findViewById(R.id.tax_mode_group);
        EditText amountEt = panel.findViewById(R.id.tax_amount_input);
        EditText rateEt = panel.findViewById(R.id.tax_rate_input);
        Button calculateBtn = panel.findViewById(R.id.tax_calculate_btn);

        View resultCard = panel.findViewById(R.id.tax_result_card);
        TextView taxLabel = panel.findViewById(R.id.tax_result_tax_label);
        TextView amountLabel = panel.findViewById(R.id.tax_result_amount_label);
        TextView taxAmountTv = panel.findViewById(R.id.tax_result_tax_amount);
        TextView totalTv = panel.findViewById(R.id.tax_result_total_amount);
        TextView noteTv = panel.findViewById(R.id.tax_result_note);

        if (calculateBtn == null || modeGroup == null || amountEt == null || rateEt == null
                || resultCard == null || taxLabel == null || amountLabel == null
                || taxAmountTv == null || totalTv == null || noteTv == null) {
            return;
        }

        amountEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) normalizeInput(amountEt);
        });
        rateEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) normalizeInput(rateEt);
        });

        calculateBtn.setOnClickListener(v -> {
            amountEt.setError(null);
            rateEt.setError(null);

            BigDecimal amount = parseDecimalOrNull(amountEt.getText());
            BigDecimal rate = parseDecimalOrNull(rateEt.getText());

            if (amount == null) {
                amountEt.setError(panel.getContext().getString(R.string.tax_error_amount_required));
                resultCard.setVisibility(View.GONE);
                return;
            }
            if (rate == null) {
                rateEt.setError(panel.getContext().getString(R.string.tax_error_rate_required));
                resultCard.setVisibility(View.GONE);
                return;
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                amountEt.setError(panel.getContext().getString(R.string.tax_error_amount_non_negative));
                resultCard.setVisibility(View.GONE);
                return;
            }
            if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
                rateEt.setError(panel.getContext().getString(R.string.tax_error_rate_range));
                resultCard.setVisibility(View.GONE);
                return;
            }

            boolean inclusiveMode = modeGroup.getCheckedRadioButtonId() == R.id.tax_mode_inclusive;
            BigDecimal taxAmount;
            BigDecimal totalOrBase;
            if (inclusiveMode) {
                BigDecimal[] parts = TaxMath.computeInclusive(amount, rate);
                taxAmount = parts[0];
                totalOrBase = parts[1];
                taxLabel.setText(R.string.tax_breakdown_tax_part);
                amountLabel.setText(R.string.tax_breakdown_base_amount);
                noteTv.setText(panel.getContext().getString(R.string.tax_note_inclusive));
            } else {
                BigDecimal[] parts = TaxMath.computeExclusive(amount, rate);
                taxAmount = parts[0];
                totalOrBase = parts[1];
                taxLabel.setText(R.string.tax_breakdown_tax_part);
                amountLabel.setText(R.string.tax_breakdown_total_amount);
                noteTv.setText(panel.getContext().getString(R.string.tax_note_exclusive));
            }

            taxAmountTv.setText(LocaleFormatManager.formatCurrency(panel.getContext(), taxAmount.doubleValue()));
            totalTv.setText(LocaleFormatManager.formatCurrency(panel.getContext(), totalOrBase.doubleValue()));
            resultCard.setVisibility(View.VISIBLE);

            ConverterUiHelper.hideSoftKeyboard(panel);
            ConverterUiHelper.scrollToShowResult(panel, resultCard);
        });
    }

    private static void normalizeInput(EditText editText) {
        if (editText == null || editText.getText() == null) return;
        String raw = editText.getText().toString().trim();
        if (raw.isEmpty()) return;
        BigDecimal parsed = parseDecimalOrNull(raw);
        if (parsed == null) return;
        if (parsed.compareTo(BigDecimal.ZERO) == 0) {
            editText.setText("0");
            return;
        }
        editText.setText(parsed.stripTrailingZeros().toPlainString());
    }

    private static BigDecimal parseDecimalOrNull(CharSequence s) {
        if (s == null) return null;
        String raw = s.toString().trim();
        if (raw.isEmpty()) return null;
        double value = LocaleFormatManager.parseLocalizedNumber(raw, Double.NaN);
        if (Double.isNaN(value) || Double.isInfinite(value)) return null;
        return BigDecimal.valueOf(value);
    }
}

