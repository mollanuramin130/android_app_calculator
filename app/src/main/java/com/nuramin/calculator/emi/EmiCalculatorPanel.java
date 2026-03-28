package com.nuramin.calculator.emi;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.util.AmountFormatter;
import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.ConverterUiHelper;
import com.nuramin.calculator.util.LocaleFormatManager;

/**
 * EMI Calculator: principal, rate, tenure with sliders; calculate; result card and pie chart.
 * Loan amount: min fixed 1k, max fixed 1 L; only the upper value becomes dynamic (steps up) when user enters above 1 L.
 * Default value 15,000. Amount formats as user types. On clear, range resets to 1k–1 L.
 */
public final class EmiCalculatorPanel {

    private static final double PRINCIPAL_DEFAULT_MIN = 1_000;   // 1k
    private static final double PRINCIPAL_DEFAULT_MAX = 100_000; // 1 Lakh

    /** Steps for slider range: min/max snap to these when user enters value outside current range. */
    private static final double[] PRINCIPAL_RANGE_STEPS = {
        1_000,       // 1k
        5_000,       // 5k
        10_000,      // 10k
        25_000,      // 25k
        50_000,      // 50k
        100_000,     // 1 L
        250_000,     // 2.5 L
        500_000,     // 5 L
        1_000_000,   // 10 L
        2_500_000,   // 25 L
        5_000_000,   // 50 L
        10_000_000,  // 1 Cr
        25_000_000,  // 2.5 Cr
        50_000_000,  // 5 Cr
        100_000_000, // 10 Cr
        250_000_000, // 25 Cr
        500_000_000, // 50 Cr
        1_000_000_000 // 100 Cr
    };
    /** Max digits in loan amount input; beyond this show toast to use slider. */
    private static final int MAX_PRINCIPAL_DIGITS = 12;
    private static final double PRINCIPAL_DEFAULT = 15_000;
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
        TextView principalMinLabel = panel.findViewById(R.id.emi_principal_min_label);
        TextView principalMaxLabel = panel.findViewById(R.id.emi_principal_max_label);
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
        View scheduleCard = panel.findViewById(R.id.emi_schedule_card);
        panel.setTag(R.id.emi_last_calc_snapshot, null);

        if (principalSlider != null && principalEt != null) {
            double[] range = new double[] { PRINCIPAL_DEFAULT_MIN, PRINCIPAL_DEFAULT_MAX };
            panel.setTag(R.id.emi_principal, range);
            principalSlider.setMax(SLIDER_MAX);
            setPrincipalValue(principalEt, principalSlider, principalMinLabel, principalMaxLabel, panel, PRINCIPAL_DEFAULT);
            updatePrincipalRangeLabels(principalMinLabel, principalMaxLabel, range[0], range[1]);
            principalSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        double[] r = getPrincipalRange(panel);
                        double value = r[0] + (progress / (double) SLIDER_MAX) * (r[1] - r[0]);
                        setPrincipalText(principalEt, (long) value);
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            principalEt.setFilters(new InputFilter[] { new MaxPrincipalDigitsFilter(MAX_PRINCIPAL_DIGITS, panel.getContext()) });
            syncPrincipalFromInput(principalEt, principalSlider, principalMinLabel, principalMaxLabel, panel);
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
                double P = parseDouble(principalEt.getText(), PRINCIPAL_DEFAULT);
                double ratePct = parseDouble(rateEt.getText(), 10.5);
                int n = parseInt(tenureEt.getText(), 24);
                if (P <= 0 || n <= 0) {
                    resultCard.setVisibility(View.GONE);
                    clearEmiSnapshot(panel);
                    return;
                }
                double emi = EmiCalculatorMath.monthlyEmi(P, ratePct, n);
                if (Double.isNaN(emi)) {
                    resultCard.setVisibility(View.GONE);
                    clearEmiSnapshot(panel);
                    return;
                }
                double totalPayment = emi * n;
                double totalInterest = totalPayment - P;
                String emiAmount = LocaleFormatManager.formatCurrency(panel.getContext(), emi);
                String totalInterestAmount = LocaleFormatManager.formatCurrency(panel.getContext(), totalInterest);
                String totalPaymentAmount = LocaleFormatManager.formatCurrency(panel.getContext(), totalPayment);
                resultTv.setText(emiAmount + " " + panel.getContext().getString(R.string.emi_per_month));
                totalInterestTv.setText(panel.getContext().getString(R.string.emi_total_interest, totalInterestAmount));
                totalPaymentTv.setText(panel.getContext().getString(R.string.emi_total_payment, totalPaymentAmount));
                pieChart.setAmounts(P, totalInterest);
                resultCard.setVisibility(View.VISIBLE);
                panel.setTag(R.id.emi_last_calc_snapshot, new EmiLoanSnapshot(P, ratePct, n));
                ConverterUiHelper.hideSoftKeyboard(panel);
                ConverterUiHelper.scrollToShowResult(panel, resultCard);
            });
        }
        if (scheduleCard != null) {
            scheduleCard.setOnClickListener(v -> {
                Object tag = panel.getTag(R.id.emi_last_calc_snapshot);
                if (!(tag instanceof EmiLoanSnapshot)) {
                    Toast.makeText(panel.getContext(), R.string.emi_calculate_first_schedule, Toast.LENGTH_SHORT).show();
                    return;
                }
                EmiScheduleDialogHelper.show(panel.getContext(), (EmiLoanSnapshot) tag);
            });
        }
        if (resultTv != null) {
            resultTv.setOnLongClickListener(v -> {
                LocaleFormatManager.showCurrencyPickerDialog(panel.getContext(), null);
                return true;
            });
        }
    }

    private static void clearEmiSnapshot(View panel) {
        panel.setTag(R.id.emi_last_calc_snapshot, null);
    }

    @SuppressWarnings("unchecked")
    private static double[] getPrincipalRange(View panel) {
        Object tag = panel != null ? panel.getTag(R.id.emi_principal) : null;
        if (tag instanceof double[] && ((double[]) tag).length >= 2) {
            return (double[]) tag;
        }
        return new double[] { PRINCIPAL_DEFAULT_MIN, PRINCIPAL_DEFAULT_MAX };
    }

    private static void setPrincipalValue(EditText et, SeekBar slider, TextView minLabel, TextView maxLabel, View panel, double value) {
        double[] range = getPrincipalRange(panel);
        long clamped = (long) Math.max(range[0], Math.min(range[1], value));
        setPrincipalText(et, clamped);
        int progress = (int) Math.round((clamped - range[0]) / (range[1] - range[0]) * SLIDER_MAX);
        progress = Math.max(0, Math.min(SLIDER_MAX, progress));
        slider.setProgress(progress);
    }

    /** Format principal for display: always full digits with grouping, no exponential. */
    private static void setPrincipalText(EditText et, long value) {
        String formatted = AmountFormatter.format(value);
        if (!formatted.equals(et.getText().toString())) {
            et.setText(formatted);
            et.setSelection(formatted.length());
        }
    }

    private static String formatRangeLabel(double value) {
        long v = (long) value;
        return LocaleFormatManager.formatCurrency(null, v);
    }

    private static void updatePrincipalRangeLabels(TextView minLabel, TextView maxLabel, double min, double max) {
        if (minLabel != null) minLabel.setText(formatRangeLabel(min));
        if (maxLabel != null) maxLabel.setText(formatRangeLabel(max));
    }

    /** Smallest step >= value; or value if beyond last step. */
    private static double nextStepUp(double currentMax, double value) {
        for (double step : PRINCIPAL_RANGE_STEPS) {
            if (step >= value) return step;
        }
        return value;
    }

    private static void syncPrincipalFromInput(EditText et, SeekBar slider, TextView minLabel, TextView maxLabel, View panel) {
        final boolean[] updating = { false };
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (updating[0]) return;
                String raw = AmountFormatter.stripGrouping(editable);
                if (raw.isEmpty() || raw.trim().isEmpty()) {
                    double[] range = getPrincipalRange(panel);
                    range[0] = PRINCIPAL_DEFAULT_MIN;
                    range[1] = PRINCIPAL_DEFAULT_MAX;
                    updatePrincipalRangeLabels(minLabel, maxLabel, range[0], range[1]);
                    slider.setProgress(0);
                    return;
                }
                double v = parseDouble(editable, -1);
                if (v < 0) return;
                double[] range = getPrincipalRange(panel);
                range[0] = PRINCIPAL_DEFAULT_MIN; // Min always fixed at 1k
                boolean rangeChanged = false;
                if (v > range[1]) {
                    range[1] = nextStepUp(range[1], v);
                    rangeChanged = true;
                }
                if (rangeChanged) {
                    updatePrincipalRangeLabels(minLabel, maxLabel, range[0], range[1]);
                }
                updating[0] = true;
                String formatted = AmountFormatter.format((long) v);
                if (!editable.toString().equals(formatted)) {
                    et.setText(formatted);
                    et.setSelection(formatted.length());
                }
                updating[0] = false;
                int progress = (int) Math.round((v - range[0]) / (range[1] - range[0]) * SLIDER_MAX);
                progress = Math.max(0, Math.min(SLIDER_MAX, progress));
                if (slider.getProgress() != progress) slider.setProgress(progress);
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
        return LocaleFormatManager.parseLocalizedNumber(s, def);
    }

    private static int parseInt(CharSequence s, int def) {
        return LocaleFormatManager.parseLocalizedInt(s, def);
    }

    /** Limits loan amount to maxDigits (digits only). When user would exceed, rejects and shows toast to use slider. */
    private static final class MaxPrincipalDigitsFilter implements InputFilter {
        private final int maxDigits;
        private final Context context;

        MaxPrincipalDigitsFilter(int maxDigits, Context context) {
            this.maxDigits = maxDigits;
            this.context = context != null ? context.getApplicationContext() : null;
        }

        private static int countDigits(CharSequence s) {
            int n = 0;
            for (int i = 0; i < s.length(); i++) {
                if (Character.isDigit(s.charAt(i))) n++;
            }
            return n;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            CharSequence before = dest.subSequence(0, dstart);
            CharSequence replacement = source.subSequence(start, end);
            CharSequence after = dest.subSequence(dend, dest.length());
            StringBuilder sb = new StringBuilder(before).append(replacement).append(after);
            if (countDigits(sb) <= maxDigits) return null;
            if (context != null) {
                Toast.makeText(context, R.string.emi_use_slider_for_large, Toast.LENGTH_SHORT).show();
            }
            return "";
        }
    }
}
