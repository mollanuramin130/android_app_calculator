package com.nuramin.calculator.emi;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.calculator.R;
import com.nuramin.calculator.util.CalculatorUtils;

import android.view.Gravity;

/**
 * EMI Calculator: principal, rate, tenure with sliders; calculate; result card and pie chart.
 */
public final class EmiCalculatorPanel {

    private static final double PRINCIPAL_MIN = 10_000;
    private static final double PRINCIPAL_MAX = 10_000_000;
    private static final double RATE_MIN = 1;
    private static final double RATE_MAX = 25;
    private static final int TENURE_MIN = 6;
    private static final int TENURE_MAX = 360;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;
        ImageButton navMenu = panel.findViewById(R.id.emi_nav_menu);
        ImageButton menuDots = panel.findViewById(R.id.emi_menu_dots);
        final DrawerLayout layout = drawerLayout;
        if (navMenu != null && layout != null) {
            navMenu.setOnClickListener(v -> layout.openDrawer(Gravity.START));
        }
        if (menuDots != null && onOverflowClick != null) {
            menuDots.setOnClickListener(onOverflowClick);
        }

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
            principalSlider.setMax(100);
            principalSlider.setProgress(10);
            setPrincipalFromProgress(principalEt, 10);
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
        }
        if (rateSlider != null && rateEt != null) {
            rateSlider.setMax(240);
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
        }

        if (calculateBtn != null && resultCard != null && resultTv != null && totalInterestTv != null
                && totalPaymentTv != null && pieChart != null && principalEt != null && rateEt != null && tenureEt != null) {
            calculateBtn.setOnClickListener(v -> {
                double P = parseDouble(principalEt.getText(), 100000);
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
            });
        }
    }

    private static void setPrincipalFromProgress(EditText et, int progress) {
        double v = PRINCIPAL_MIN + (progress / 100.0) * (PRINCIPAL_MAX - PRINCIPAL_MIN);
        et.setText(CalculatorUtils.formatNumber((long) v));
    }

    private static void setRateFromProgress(EditText et, int progress) {
        double v = RATE_MIN + (progress / 240.0) * (RATE_MAX - RATE_MIN);
        et.setText(CalculatorUtils.formatNumber(v));
    }

    private static void setTenureFromProgress(EditText et, int progress) {
        int v = TENURE_MIN + progress;
        et.setText(String.valueOf(v));
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
