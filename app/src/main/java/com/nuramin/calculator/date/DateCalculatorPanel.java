package com.nuramin.calculator.date;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.util.ConverterUiHelper;

import java.util.Calendar;
import java.util.Locale;

/**
 * Date Calculator panel: same execution flow as Temperature/EMI/Interest/Currency.
 * Shown inside MainActivity; toolbar and overflow are handled by MainActivity.
 */
public final class DateCalculatorPanel {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;
        Context ctx = panel.getContext();

        EditText date1Day = panel.findViewById(R.id.date1_day);
        EditText date1Month = panel.findViewById(R.id.date1_month);
        EditText date1Year = panel.findViewById(R.id.date1_year);
        EditText date2Day = panel.findViewById(R.id.date2_day);
        EditText date2Month = panel.findViewById(R.id.date2_month);
        EditText date2Year = panel.findViewById(R.id.date2_year);
        View dateLabelSection2 = panel.findViewById(R.id.date_label_section2);
        View dateRowSection2 = panel.findViewById(R.id.date_row_section2);
        TextView dateLabelSection1 = panel.findViewById(R.id.date_label_section1);
        Button btnCalculate = panel.findViewById(R.id.date_btn_calculate);
        MaterialCardView resultCard = panel.findViewById(R.id.date_result_card);
        TextView resultText = panel.findViewById(R.id.date_result_text);
        TextView resultSub = panel.findViewById(R.id.date_result_sub);
        RadioGroup modeGroup = panel.findViewById(R.id.date_mode_group);

        boolean[] isAgeMode = { true };

        RadioGroup.OnCheckedChangeListener modeListener = (group, checkedId) -> {
            isAgeMode[0] = (checkedId == R.id.date_mode_age);
            setSection2Visibility(dateLabelSection2, dateRowSection2, isAgeMode[0]);
            setLabelSection1(ctx, dateLabelSection1, isAgeMode[0]);
            clearErrors(date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
            updateCalculateState(ctx, btnCalculate, isAgeMode[0], date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
        };
        if (modeGroup != null) modeGroup.setOnCheckedChangeListener(modeListener);

        setLabelSection1(ctx, dateLabelSection1, isAgeMode[0]);
        setSection2Visibility(dateLabelSection2, dateRowSection2, isAgeMode[0]);

        setCalendarPicker(panel, ctx, R.id.date1_calendar, date1Day, date1Month, date1Year, true, isAgeMode,
                () -> updateCalculateState(ctx, btnCalculate, isAgeMode[0], date1Day, date1Month, date1Year, date2Day, date2Month, date2Year));
        setCalendarPicker(panel, ctx, R.id.date2_calendar, date2Day, date2Month, date2Year, false, isAgeMode,
                () -> updateCalculateState(ctx, btnCalculate, isAgeMode[0], date1Day, date1Month, date1Year, date2Day, date2Month, date2Year));

        if (btnCalculate != null) {
            btnCalculate.setOnClickListener(v -> {
                if (!validateAndShowErrors(ctx, isAgeMode[0], date1Day, date1Month, date1Year, date2Day, date2Month, date2Year)) return;
                if (isAgeMode[0]) {
                    computeAge(ctx, date1Day, date1Month, date1Year, resultCard, resultText, resultSub);
                } else {
                    computeDateDifference(ctx, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year, resultCard, resultText, resultSub);
                }
                ConverterUiHelper.hideSoftKeyboard(panel);
                if (resultCard != null) {
                    ConverterUiHelper.scrollToShowResult(panel, resultCard);
                }
            });
        }

        addValidationListeners(ctx, btnCalculate, isAgeMode, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
        updateCalculateState(ctx, btnCalculate, isAgeMode[0], date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
    }

    private static void setLabelSection1(Context ctx, TextView dateLabelSection1, boolean isAgeMode) {
        if (dateLabelSection1 == null) return;
        dateLabelSection1.setText(isAgeMode ? R.string.date_dob_label : R.string.date_start_date_label);
    }

    private static void setSection2Visibility(View dateLabelSection2, View dateRowSection2, boolean isAgeMode) {
        if (dateLabelSection2 != null) dateLabelSection2.setVisibility(isAgeMode ? View.GONE : View.VISIBLE);
        if (dateRowSection2 != null) dateRowSection2.setVisibility(isAgeMode ? View.GONE : View.VISIBLE);
    }

    private static void setCalendarPicker(View panel, Context ctx, int buttonId, EditText dayEt, EditText monthEt, EditText yearEt,
                                          boolean isDate1, boolean[] isAgeMode, Runnable onDateSet) {
        View btn = panel.findViewById(buttonId);
        if (btn == null || dayEt == null || monthEt == null || yearEt == null) return;
        btn.setOnClickListener(v -> {
            int y = parseInt(yearEt.getText(), Calendar.getInstance().get(Calendar.YEAR));
            int m = parseInt(monthEt.getText(), 1) - 1;
            int d = parseInt(dayEt.getText(), 1);
            if (m < 0) m = 0;
            if (m > 11) m = 11;
            DatePickerDialog dlg = new DatePickerDialog(ctx,
                    (view, year, month, dayOfMonth) -> {
                        yearEt.setText(String.valueOf(year));
                        monthEt.setText(String.format(Locale.US, "%d", month + 1));
                        dayEt.setText(String.format(Locale.US, "%d", dayOfMonth));
                        clearError(dayEt);
                        clearError(monthEt);
                        clearError(yearEt);
                        if (onDateSet != null) onDateSet.run();
                    }, y, m, d);
            if (isAgeMode != null && isAgeMode[0] && isDate1) {
                dlg.getDatePicker().setMaxDate(System.currentTimeMillis());
            }
            dlg.show();
        });
    }

    private static void addValidationListeners(Context ctx, Button btnCalculate, boolean[] isAgeMode,
                                                EditText date1Day, EditText date1Month, EditText date1Year,
                                                EditText date2Day, EditText date2Month, EditText date2Year) {
        EditText[] all = { date1Day, date1Month, date1Year, date2Day, date2Month, date2Year };
        for (EditText et : all) {
            if (et != null) {
                et.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable e) {
                        clearError(et);
                        updateCalculateState(ctx, btnCalculate, isAgeMode[0], date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
                    }
                });
            }
        }
    }

    private static void updateCalculateState(Context ctx, Button btnCalculate, boolean isAgeMode,
                                             EditText date1Day, EditText date1Month, EditText date1Year,
                                             EditText date2Day, EditText date2Month, EditText date2Year) {
        if (btnCalculate == null) return;
        btnCalculate.setEnabled(isInputValid(isAgeMode, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year));
    }

    private static boolean isInputValid(boolean isAgeMode, EditText date1Day, EditText date1Month, EditText date1Year,
                                         EditText date2Day, EditText date2Month, EditText date2Year) {
        if (isAgeMode) {
            return validateDateFields(null, date1Day, date1Month, date1Year, true) == null;
        }
        String e1 = validateDateFields(null, date1Day, date1Month, date1Year, false);
        String e2 = validateDateFields(null, date2Day, date2Month, date2Year, false);
        if (e1 != null || e2 != null) return false;
        Calendar start = toCalendar(date1Day, date1Month, date1Year);
        Calendar end = toCalendar(date2Day, date2Month, date2Year);
        return start != null && end != null && !end.before(start);
    }

    private static boolean validateAndShowErrors(Context ctx, boolean isAgeMode,
                                                  EditText date1Day, EditText date1Month, EditText date1Year,
                                                  EditText date2Day, EditText date2Month, EditText date2Year) {
        if (isAgeMode) {
            String err = validateDateFields(ctx, date1Day, date1Month, date1Year, true);
            if (err != null) {
                Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
        String e1 = validateDateFields(ctx, date1Day, date1Month, date1Year, false);
        if (e1 != null) {
            Toast.makeText(ctx, e1, Toast.LENGTH_SHORT).show();
            return false;
        }
        String e2 = validateDateFields(ctx, date2Day, date2Month, date2Year, false);
        if (e2 != null) {
            Toast.makeText(ctx, e2, Toast.LENGTH_SHORT).show();
            return false;
        }
        Calendar start = toCalendar(date1Day, date1Month, date1Year);
        Calendar end = toCalendar(date2Day, date2Month, date2Year);
        if (start == null || end == null) return false;
        if (end.before(start)) {
            if (date2Year != null) date2Year.setError(ctx.getString(R.string.date_error_end_before_start));
            Toast.makeText(ctx, R.string.date_error_end_before_start, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private static String validateDateFields(Context ctx, EditText dayEt, EditText monthEt, EditText yearEt, boolean preventFutureDob) {
        if (ctx == null) return "";
        clearError(dayEt);
        clearError(monthEt);
        clearError(yearEt);
        if (isEmpty(dayEt) || isEmpty(monthEt) || isEmpty(yearEt)) {
            return ctx.getString(R.string.date_error_fill_all);
        }
        int d = parseInt(dayEt.getText(), 0);
        int m = parseInt(monthEt.getText(), 0);
        int y = parseInt(yearEt.getText(), 0);
        if (d < 1 || d > 31) {
            if (dayEt != null) dayEt.setError(ctx.getString(R.string.date_error_invalid_day));
            return ctx.getString(R.string.date_error_invalid_day);
        }
        if (m < 1 || m > 12) {
            if (monthEt != null) monthEt.setError(ctx.getString(R.string.date_error_invalid_month));
            return ctx.getString(R.string.date_error_invalid_month);
        }
        if (y < MIN_YEAR || y > MAX_YEAR) {
            if (yearEt != null) yearEt.setError(ctx.getString(R.string.date_error_invalid_year));
            return ctx.getString(R.string.date_error_invalid_year);
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, y);
        cal.set(Calendar.MONTH, m - 1);
        cal.set(Calendar.DAY_OF_MONTH, Math.min(d, cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
        if (preventFutureDob && cal.after(Calendar.getInstance())) {
            if (yearEt != null) yearEt.setError(ctx.getString(R.string.date_error_future_dob));
            return ctx.getString(R.string.date_error_future_dob);
        }
        return null;
    }

    private static void clearErrors(EditText... editTexts) {
        for (EditText et : editTexts) clearError(et);
    }

    private static void clearError(EditText et) {
        if (et != null) et.setError(null);
    }

    private static boolean isEmpty(EditText et) {
        return et == null || TextUtils.isEmpty(et.getText());
    }

    private static int parseInt(CharSequence s, int def) {
        if (s == null || s.length() == 0) return def;
        try {
            return Integer.parseInt(s.toString().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static Calendar toCalendar(EditText dayEt, EditText monthEt, EditText yearEt) {
        if (dayEt == null || monthEt == null || yearEt == null) return null;
        int d = parseInt(dayEt.getText(), 1);
        int m = parseInt(monthEt.getText(), 1) - 1;
        int y = parseInt(yearEt.getText(), Calendar.getInstance().get(Calendar.YEAR));
        if (y < MIN_YEAR || y > MAX_YEAR || m < 0 || m > 11 || d < 1 || d > 31) return null;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, y);
        cal.set(Calendar.MONTH, m);
        cal.set(Calendar.DAY_OF_MONTH, Math.min(d, cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
        return cal;
    }

    private static void computeAge(Context ctx, EditText date1Day, EditText date1Month, EditText date1Year,
                                    MaterialCardView resultCard, TextView resultText, TextView resultSub) {
        Calendar birth = toCalendar(date1Day, date1Month, date1Year);
        if (birth == null || ctx == null) return;
        Calendar today = Calendar.getInstance();
        if (birth.after(today)) return;

        int years = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        int months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH);
        int days = today.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH);

        if (days < 0) {
            months--;
            Calendar prev = (Calendar) today.clone();
            prev.add(Calendar.MONTH, -1);
            days += prev.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        if (months < 0) {
            years--;
            months += 12;
        }

        String main = ctx.getString(R.string.date_result_age_format, years, months, days);
        long totalDays = (today.getTimeInMillis() - birth.getTimeInMillis()) / (1000 * 60 * 60 * 24);
        String sub = ctx.getString(R.string.date_result_total_days, (int) totalDays);
        showResult(resultCard, resultText, resultSub, main, sub);
    }

    private static void computeDateDifference(Context ctx, EditText date1Day, EditText date1Month, EditText date1Year,
                                               EditText date2Day, EditText date2Month, EditText date2Year,
                                               MaterialCardView resultCard, TextView resultText, TextView resultSub) {
        Calendar start = toCalendar(date1Day, date1Month, date1Year);
        Calendar end = toCalendar(date2Day, date2Month, date2Year);
        if (start == null || end == null || end.before(start) || ctx == null) return;

        long diffMs = end.getTimeInMillis() - start.getTimeInMillis();
        int totalDays = (int) (diffMs / (1000 * 60 * 60 * 24));

        int years = end.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        int months = end.get(Calendar.MONTH) - start.get(Calendar.MONTH);
        int days = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH);
        if (days < 0) {
            months--;
            Calendar prev = (Calendar) end.clone();
            prev.add(Calendar.MONTH, -1);
            days += prev.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        if (months < 0) {
            years--;
            months += 12;
        }

        String main = ctx.getString(R.string.date_result_total_days, totalDays);
        String sub = ctx.getString(R.string.date_result_breakdown, years, months, days);
        showResult(resultCard, resultText, resultSub, main, sub);
    }

    private static void showResult(MaterialCardView resultCard, TextView resultText, TextView resultSub, String main, String sub) {
        if (resultCard != null) resultCard.setVisibility(View.VISIBLE);
        if (resultText != null) resultText.setText(main);
        if (resultSub != null) resultSub.setText(sub);
    }
}
