package com.nuramin.calculator.temperature;

import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.calculator.R;
import com.nuramin.calculator.util.CalculatorUtils;

/**
 * Temperature Converter: from/to units, convert button, result, formula, quick swap.
 */
public final class TemperaturePanel {

    private static final int UNIT_C = 0;
    private static final int UNIT_F = 1;
    private static final int UNIT_K = 2;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;
        ImageButton navMenu = panel.findViewById(R.id.temp_nav_menu);
        ImageButton menuDots = panel.findViewById(R.id.temp_menu_dots);
        final DrawerLayout layout = drawerLayout;
        if (navMenu != null && layout != null) {
            navMenu.setOnClickListener(v -> layout.openDrawer(Gravity.START));
        }
        if (menuDots != null && onOverflowClick != null) {
            menuDots.setOnClickListener(onOverflowClick);
        }

        EditText input = panel.findViewById(R.id.temp_input);
        Spinner fromSpinner = panel.findViewById(R.id.temp_from);
        Spinner toSpinner = panel.findViewById(R.id.temp_to);
        View convertBtn = panel.findViewById(R.id.temp_calculate);
        TextView result = panel.findViewById(R.id.temp_result);
        TextView formula = panel.findViewById(R.id.temp_formula);
        View quickSwap = panel.findViewById(R.id.temp_quick_swap);

        if (fromSpinner != null && toSpinner != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(panel.getContext(),
                    R.array.temp_units, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            fromSpinner.setAdapter(adapter);
            toSpinner.setAdapter(adapter);
            fromSpinner.setSelection(UNIT_C);
            toSpinner.setSelection(UNIT_F);
        }

        if (convertBtn != null && input != null && result != null && formula != null && fromSpinner != null && toSpinner != null) {
            convertBtn.setOnClickListener(v -> performConvert(input, fromSpinner, toSpinner, result, formula));
        }

        if (quickSwap != null && fromSpinner != null && toSpinner != null && input != null && result != null && formula != null) {
            quickSwap.setOnClickListener(v -> {
                int from = fromSpinner.getSelectedItemPosition();
                int to = toSpinner.getSelectedItemPosition();
                fromSpinner.setSelection(to);
                toSpinner.setSelection(from);
                performConvert(input, fromSpinner, toSpinner, result, formula);
            });
        }
    }

    private static void performConvert(EditText input, Spinner fromSpinner, Spinner toSpinner,
                                      TextView result, TextView formula) {
        double value;
        try {
            String s = input.getText() != null ? input.getText().toString().trim() : "";
            value = s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            result.setText("—");
            formula.setText("");
            return;
        }
        int from = fromSpinner.getSelectedItemPosition();
        int to = toSpinner.getSelectedItemPosition();
        if (from < 0) from = UNIT_C;
        if (to < 0) to = UNIT_F;
        double celsius = toCelsius(value, from);
        double out = fromCelsius(celsius, to);
        result.setText(CalculatorUtils.formatNumber(out));
        formula.setText(getFormula(value, from, out, to));
    }

    private static double toCelsius(double value, int unit) {
        switch (unit) {
            case UNIT_F: return (value - 32) * 5 / 9;
            case UNIT_K: return value - 273.15;
            default: return value;
        }
    }

    private static double fromCelsius(double celsius, int unit) {
        switch (unit) {
            case UNIT_F: return celsius * 9 / 5 + 32;
            case UNIT_K: return celsius + 273.15;
            default: return celsius;
        }
    }

    private static String getFormula(double in, int from, double out, int to) {
        String fromUnit = from == UNIT_C ? "°C" : (from == UNIT_F ? "°F" : "K");
        String toUnit = to == UNIT_C ? "°C" : (to == UNIT_F ? "°F" : "K");
        return String.format("%s %s = %s %s", CalculatorUtils.formatNumber(in), fromUnit, CalculatorUtils.formatNumber(out), toUnit);
    }
}
