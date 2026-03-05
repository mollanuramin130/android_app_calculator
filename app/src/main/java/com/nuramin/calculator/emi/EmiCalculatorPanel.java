package com.nuramin.calculator.emi;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.ConverterUiHelper;

/**
 * EMI Calculator: principal, rate, tenure with sliders; calculate; result card and pie chart.
 * Loan amount starts from 1k; slider and input stay in sync when user types or moves slider.
 */
public final class EmiCalculatorPanel {

    private static final double PRINCIPAL_MIN = 1_000;
    private static final double PRINCIPAL_MAX = 10_000_000;
    private static final double RATE_MIN = 1;
    private static final double RATE_MAX = 25;
    private static final int TENURE_MIN = 6;
    private static final int TENURE_MAX = 360;
    private static final int SLIDER_MAX = 100;
    private static final int RATE_SLIDER_MAX = 240;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;

        EditText principalEt = panel.findViewById(R.id.emi_principal);
        SeekBar principalSlider = panel.findViewById(R.id.emi_slider_principal);
        EditText rateEt = panel.findViewById(R.id.emi_rate);
        SeekBar rateSlider = panel.findViewById(R.id.emi_slider_rate);
        EditText tenureEt = panel.findViewById(R.id.emi_tenure);
        SeekBar tenureSlider = panel.findViewById(R.id.emi_slider_tenure);
        Button calculateBtn = panel.findViewById(R.id.emi_calculate);
        View resultCard = panel.findViewById(R.id.emi_result_card);
        TextView resultTv = panel.findViewById(R.id.emi_result);
        TextView totalInterestTv = panel.findViewById(R.id.emi_total_interest);
        TextView totalPaymentTv = panel.findViewById(R.id.emi_total_payment);
        EmiPieChartView pieChart = panel.findViewById(R.id.emi_pie_chart);

        if (principalSlider != null && principalEt != null) {
            principalSlider.setMax(SLIDER_MAX);
            principalSlider.setProgress(0);
            setPrincipalFromProgress(principalEt, 0);
            principalSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) setPrincipalFromProgress(principalEt, progress);
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            syncPrincipalSliderFromInput(principalEt, principalSlider);
        }
        if (rateSlider != null && rateEt != null) {
            rateSlider.setMax(RATE_SLIDER_MAX);
            rateSlider.setProgress(105);
            setRateFromProgress(rateEt, 105);
            rateSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) setRateFromProgress(rateEt, progress);
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            syncRateSliderFromInput(rateEt, rateSlider);
        }
        if (tenureSlider != null && tenureEt != null) {
            tenureSlider.setMax(TENURE_MAX - TENURE_MIN);
            tenureSlider.setProgress(18);
            setTenureFromProgress(tenureEt, 18);
            tenureSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) setTenureFromProgress(tenureEt, progress);
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            syncTenureSliderFromInput(tenureEt, tenureSlider);
        }

        if (calculateBtn != null && resultCard != null && resultTv != null && totalInterestTv != null
                && totalPaymentTv != null && pieChart != null && principalEt != null && rateEt != null && tenureEt != null) {
            calculateBtn.setOnClickListener(v -> {
                double P = parseDouble(principalEt.getText(), 1000);
                double ratePct = parseDouble(rateEt.getText(), 10.5);
                int n = parseInt(tenureEt.getText(), 24);
                if (P <= 0 || n <= 0) {
                    resultCard.setVisibility(View.GONE);
                    return;
                }
                double r = ratePct / 12 / 100;
                double emi;
                if (r <= 0) {
                    emi = P / n;
                } else {
                    double factor = Math.pow(1 + r, n);
                    emi = P * r * factor / (factor - 1);
                }
                double totalPayment = emi * n;
                double totalInterest = totalPayment - P;
                resultTv.setText(panel.getContext().getString(R.string.emi_rupee_symbol) + " " + CalculatorUtils.formatNumber(emi) + " " + panel.getContext().getString(R.string.emi_per_month));
                totalInterestTv.setText(panel.getContext().getString(R.string.emi_total_interest, "₹ " + CalculatorUtils.formatNumber(totalInterest)));
                totalPaymentTv.setText(panel.getContext().getString(R.string.emi_total_payment, "₹ " + CalculatorUtils.formatNumber(totalPayment)));
                pieChart.setAmounts(P, totalInterest);
                resultCard.setVisibility(View.VISIBLE);
                ConverterUiHelper.hideSoftKeyboard(panel);
                ConverterUiHelper.scrollToShowResult(panel, resultCard);
            });
        }
    }

    private static void setPrincipalFromProgress(EditText et, int progress) {
        double v = PRINCIPAL_MIN + (progress / (double) SLIDER_MAX) * (PRINCIPAL_MAX - PRINCIPAL_MIN);
        et.setText(CalculatorUtils.formatNumber((long) v));
    }

    private static int progressFromPrincipal(double value) {
        double clamped = Math.max(PRINCIPAL_MIN, Math.min(PRINCIPAL_MAX, value));
        return (int) Math.round((clamped - PRINCIPAL_MIN) / (PRINCIPAL_MAX - PRINCIPAL_MIN) * SLIDER_MAX);
    }

    private static void syncPrincipalSliderFromInput(EditText et, SeekBar slider) {
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                double v = parseDouble(editable, -1);
                if (v >= 0) {
                    int progress = progressFromPrincipal(v);
                    if (slider.getProgress() != progress) slider.setProgress(progress);
                }
            }
        });
    }

    private static void setRateFromProgress(EditText et, int progress) {
        double v = RATE_MIN + (progress / (double) RATE_SLIDER_MAX) * (RATE_MAX - RATE_MIN);
        et.setText(CalculatorUtils.formatNumber(v));
    }

    private static int progressFromRate(double value) {
        double clamped = Math.max(RATE_MIN, Math.min(RATE_MAX, value));
        return (int) Math.round((clamped - RATE_MIN) / (RATE_MAX - RATE_MIN) * RATE_SLIDER_MAX);
    }

    private static void syncRateSliderFromInput(EditText et, SeekBar slider) {
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                double v = parseDouble(editable, -1);
                if (v >= 0) {
                    int progress = progressFromRate(v);
                    if (slider.getProgress() != progress) slider.setProgress(progress);
                }
            }
        });
    }

    private static void setTenureFromProgress(EditText et, int progress) {
        int v = TENURE_MIN + progress;
        et.setText(String.valueOf(v));
    }

    private static int progressFromTenure(int value) {
        int clamped = Math.max(TENURE_MIN, Math.min(TENURE_MAX, value));
        return clamped - TENURE_MIN;
    }

    private static void syncTenureSliderFromInput(EditText et, SeekBar slider) {
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                int v = parseInt(editable, -1);
                if (v >= 0) {
                    int progress = progressFromTenure(v);
                    if (slider.getProgress() != progress) slider.setProgress(progress);
                }
            }
        });
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
