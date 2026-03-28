package com.nuramin.calculator.unitconverter;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.snackbar.Snackbar;
import com.nuramin.calculator.unitconverter.exceptions.SameUnitException;
import com.nuramin.calculator.unitconverter.exceptions.UnitConversionException;
import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.LocaleFormatManager;
import com.nuramin.calculator.util.ConverterUiHelper;
import com.nuramin.sunsetcoralcalculator.R;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** UI controller for the Unit Converter panel (no activity). */
public final class UnitConverterPanel {
    private UnitConverterPanel() {}

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;

        Spinner categorySpinner = panel.findViewById(R.id.unit_category_spinner);
        Spinner fromSpinner = panel.findViewById(R.id.unit_from_spinner);
        Spinner toSpinner = panel.findViewById(R.id.unit_to_spinner);
        EditText valueInput = panel.findViewById(R.id.unit_input_value);
        View convertBtn = panel.findViewById(R.id.unit_convert_btn);
        View swapBtn = panel.findViewById(R.id.unit_swap_btn);
        TextView resultTv = panel.findViewById(R.id.unit_result_value);
        TextView formulaTv = panel.findViewById(R.id.unit_result_formula);

        if (categorySpinner == null || fromSpinner == null || toSpinner == null
                || valueInput == null || convertBtn == null || swapBtn == null
                || resultTv == null || formulaTv == null) return;

        List<String> categories = UnitConversionService.getCategories();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(panel.getContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        Runnable refreshUnits = () -> {
            String selectedCategory = String.valueOf(categorySpinner.getSelectedItem());
            List<String> units = new ArrayList<>(UnitConversionService.getUnitsForCategory(selectedCategory));
            ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(panel.getContext(), android.R.layout.simple_spinner_item, units);
            unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            fromSpinner.setAdapter(unitAdapter);
            toSpinner.setAdapter(unitAdapter);
            if (units.size() > 1) toSpinner.setSelection(1);
        };
        categorySpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(refreshUnits));
        refreshUnits.run();

        convertBtn.setOnClickListener(v -> {
            try {
                BigDecimal converted = convert(valueInput, fromSpinner, toSpinner);
                resultTv.setText(CalculatorUtils.formatNumber(converted.doubleValue()));
                formulaTv.setText(buildFormula(valueInput.getText() == null ? "0" : valueInput.getText().toString(), fromSpinner, toSpinner));
                ConverterUiHelper.hideSoftKeyboard(panel);
                ConverterUiHelper.scrollToShowResult(panel, resultTv);
            } catch (SameUnitException same) {
                String in = valueInput.getText() == null || valueInput.getText().toString().trim().isEmpty() ? "0" : valueInput.getText().toString().trim();
                resultTv.setText(in);
                formulaTv.setText(panel.getContext().getString(R.string.unit_same_unit_note));
            } catch (UnitConversionException ex) {
                Snackbar.make(panel, ex.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });

        swapBtn.setOnClickListener(v -> {
            int from = fromSpinner.getSelectedItemPosition();
            int to = toSpinner.getSelectedItemPosition();
            fromSpinner.setSelection(Math.max(0, to));
            toSpinner.setSelection(Math.max(0, from));
        });
    }

    private static BigDecimal convert(EditText input, Spinner fromSpinner, Spinner toSpinner) throws UnitConversionException {
        String raw = input.getText() == null ? "" : input.getText().toString().trim();
        if (raw.isEmpty()) raw = "0";
        double value = LocaleFormatManager.parseLocalizedNumber(raw, Double.NaN);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new com.nuramin.calculator.unitconverter.exceptions.InvalidValueException("Value must be a valid number.");
        }
        String from = String.valueOf(fromSpinner.getSelectedItem());
        String to = String.valueOf(toSpinner.getSelectedItem());
        return UnitConversionService.convert(BigDecimal.valueOf(value), from, to);
    }

    private static String buildFormula(String input, Spinner fromSpinner, Spinner toSpinner) {
        String from = String.valueOf(fromSpinner.getSelectedItem());
        String to = String.valueOf(toSpinner.getSelectedItem());
        return String.format(Locale.US, "%s %s -> %s", input, from, to);
    }
}
