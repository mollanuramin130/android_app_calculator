package com.nuramin.calculator.date;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.util.ConverterUiHelper;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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

        Spinner date1Day = panel.findViewById(R.id.date1_day);
        Spinner date1Month = panel.findViewById(R.id.date1_month);
        TextView date1DayDisplay = panel.findViewById(R.id.date1_day_display);
        TextView date1MonthDisplay = panel.findViewById(R.id.date1_month_display);
        EditText date1Year = panel.findViewById(R.id.date1_year);
        Spinner date2Day = panel.findViewById(R.id.date2_day);
        Spinner date2Month = panel.findViewById(R.id.date2_month);
        TextView date2DayDisplay = panel.findViewById(R.id.date2_day_display);
        TextView date2MonthDisplay = panel.findViewById(R.id.date2_month_display);
        EditText date2Year = panel.findViewById(R.id.date2_year);
        TextView dateLabelSection1 = panel.findViewById(R.id.date_label_section1);
        TextView dateLabelSection2 = panel.findViewById(R.id.date_label_section2);
        Button btnCalculate = panel.findViewById(R.id.date_btn_calculate);
        View mainCard = panel.findViewById(R.id.date_card_main);
        MaterialCardView resultCard = panel.findViewById(R.id.date_result_card);
        if (resultCard == null && mainCard != null) resultCard = mainCard.findViewById(R.id.date_result_card);
        if (resultCard == null && ctx instanceof Activity) {
            resultCard = ((Activity) ctx).findViewById(R.id.date_result_card);
        }
        if (resultCard == null && panel.getRootView() != null) resultCard = panel.getRootView().findViewById(R.id.date_result_card);
        TextView resultText = panel.findViewById(R.id.date_result_text);
        TextView resultSub = panel.findViewById(R.id.date_result_sub);
        TextView resultDetail = panel.findViewById(R.id.date_result_detail);
        if (resultText == null && resultCard != null) resultText = resultCard.findViewById(R.id.date_result_text);
        if (resultSub == null && resultCard != null) resultSub = resultCard.findViewById(R.id.date_result_sub);
        if (resultDetail == null && resultCard != null) resultDetail = resultCard.findViewById(R.id.date_result_detail);
        if (resultText == null && ctx instanceof Activity) resultText = ((Activity) ctx).findViewById(R.id.date_result_text);
        if (resultSub == null && ctx instanceof Activity) resultSub = ((Activity) ctx).findViewById(R.id.date_result_sub);
        if (resultDetail == null && ctx instanceof Activity) resultDetail = ((Activity) ctx).findViewById(R.id.date_result_detail);
        if (resultText == null && panel.getRootView() != null) resultText = panel.getRootView().findViewById(R.id.date_result_text);
        if (resultSub == null && panel.getRootView() != null) resultSub = panel.getRootView().findViewById(R.id.date_result_sub);
        if (resultDetail == null && panel.getRootView() != null) resultDetail = panel.getRootView().findViewById(R.id.date_result_detail);
        final MaterialCardView finalResultCard = resultCard;
        final TextView finalResultText = resultText;
        final TextView finalResultSub = resultSub;
        final TextView finalResultDetail = resultDetail;
        CheckBox useTodayCheckbox = panel.findViewById(R.id.date_use_today_checkbox);

        boolean[] settingDateFromCheckbox = { false };
        final boolean isAgeMode = true;

        setLabelSection1(ctx, dateLabelSection1, true);
        setLabelSection2(dateLabelSection2, true);

        initMonthSpinner(ctx, date1Month);
        initMonthSpinner(ctx, date2Month);
        int yearDefault = Calendar.getInstance().get(Calendar.YEAR);
        refreshDaySpinner(ctx, date1Day, 1, yearDefault);
        date1Day.setSelection(0);
        date1Month.setSelection(0);
        fillDateWithToday(ctx, date2Day, date2Month, date2Year);
        setupSpinnerDisplayClicks(date1Day, date1DayDisplay, date1Month, date1MonthDisplay, date2Day, date2DayDisplay, date2Month, date2MonthDisplay);
        syncDayDisplay(date1Day, date1DayDisplay);
        syncMonthDisplay(date1Month, date1MonthDisplay);
        syncDayDisplay(date2Day, date2DayDisplay);
        syncMonthDisplay(date2Month, date2MonthDisplay);
        setupMonthYearListeners(ctx, btnCalculate, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year, date1DayDisplay, date1MonthDisplay, date2DayDisplay, date2MonthDisplay);

        if (useTodayCheckbox != null && date2Day != null && date2Month != null && date2Year != null) {
            useTodayCheckbox.setChecked(true);
            useTodayCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    settingDateFromCheckbox[0] = true;
                    fillDateWithToday(ctx, date2Day, date2Month, date2Year);
                    settingDateFromCheckbox[0] = false;
                    syncDayDisplay(date2Day, date2DayDisplay);
                    syncMonthDisplay(date2Month, date2MonthDisplay);
                    if (date2Year != null) date2Year.setError(null);
                    updateCalculateState(ctx, btnCalculate, true, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
                }
            });
        }

        addDate2ChangeListeners(ctx, btnCalculate, useTodayCheckbox, settingDateFromCheckbox,
                date1Day, date1Month, date1Year, date2Day, date2Month, date2Year, date2DayDisplay, date2MonthDisplay);

        setCalendarPicker(panel, ctx, R.id.date1_calendar, date1Day, date1Month, date1Year, true, null,
                date1DayDisplay, date1MonthDisplay,
                () -> updateCalculateState(ctx, btnCalculate, true, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year));
        setCalendarPicker(panel, ctx, R.id.date2_calendar, date2Day, date2Month, date2Year, false, useTodayCheckbox,
                date2DayDisplay, date2MonthDisplay,
                () -> updateCalculateState(ctx, btnCalculate, true, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year));

        if (btnCalculate != null) {
            btnCalculate.setOnClickListener(v -> {
                if (!validateAndShowErrors(ctx, true, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year)) return;
                computeAge(ctx, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year, finalResultCard, finalResultText, finalResultSub, finalResultDetail);
                ConverterUiHelper.hideSoftKeyboard(panel);
                if (finalResultCard != null) {
                    finalResultCard.setVisibility(View.VISIBLE);
                    finalResultCard.post(() -> ConverterUiHelper.scrollToShowResult(panel, finalResultCard));
                }
            });
        }

        addValidationListeners(ctx, btnCalculate, true, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year,
                date1DayDisplay, date1MonthDisplay, date2DayDisplay, date2MonthDisplay);
        updateCalculateState(ctx, btnCalculate, true, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
    }

    private static void setLabelSection1(Context ctx, TextView dateLabelSection1, boolean isAgeMode) {
        if (dateLabelSection1 == null) return;
        dateLabelSection1.setText(isAgeMode ? R.string.date_dob_label : R.string.date_start_date_label);
    }

    private static void setLabelSection2(TextView dateLabelSection2, boolean isAgeMode) {
        if (dateLabelSection2 == null) return;
        dateLabelSection2.setText(isAgeMode ? R.string.date_todays_date_label : R.string.date_end_date_label);
    }

    private static void syncSpinnerDisplay(Spinner spinner, TextView display) {
        if (display == null) return;
        if (spinner == null || spinner.getAdapter() == null || spinner.getSelectedItemPosition() < 0) {
            display.setText("");
            return;
        }
        Object item = spinner.getSelectedItem();
        if (item == null) {
            display.setText("");
            return;
        }
        display.setText(item.toString());
    }

    /** Sync day display with strict "01"-"31" format to avoid noise like "0.1". */
    private static void syncDayDisplay(Spinner daySpinner, TextView display) {
        if (display == null) return;
        if (daySpinner == null || daySpinner.getAdapter() == null || daySpinner.getSelectedItemPosition() < 0) {
            display.setText("01");
            return;
        }
        int day = getSpinnerDay(daySpinner);
        if (day < 1 || day > 31) day = 1;
        display.setText(String.format(Locale.US, "%02d", day));
    }

    /** Sync month display with strict "01 Jan"-"12 Dec" format to avoid noise like "003Mar". */
    private static void syncMonthDisplay(Spinner monthSpinner, TextView display) {
        if (display == null) return;
        if (monthSpinner == null || monthSpinner.getAdapter() == null || monthSpinner.getSelectedItemPosition() < 0) {
            display.setText("01 Jan");
            return;
        }
        int month1Based = monthSpinner.getSelectedItemPosition() + 1;
        if (month1Based < 1 || month1Based > 12) month1Based = 1;
        String abbr = getMonthAbbrFallback(month1Based);
        try {
            String[] shortMonths = DateFormatSymbols.getInstance(Locale.getDefault()).getShortMonths();
            int idx = month1Based - 1;
            if (idx < shortMonths.length && shortMonths[idx] != null && !shortMonths[idx].isEmpty())
                abbr = shortMonths[idx].trim();
        } catch (Exception ignored) {}
        display.setText(String.format(Locale.US, "%02d %s", month1Based, abbr));
    }

    private static void setupSpinnerDisplayClicks(Spinner date1Day, TextView date1DayDisplay,
                                                   Spinner date1Month, TextView date1MonthDisplay,
                                                   Spinner date2Day, TextView date2DayDisplay,
                                                   Spinner date2Month, TextView date2MonthDisplay) {
        if (date1Day != null && date1DayDisplay != null) date1DayDisplay.setOnClickListener(v -> date1Day.performClick());
        if (date1Month != null && date1MonthDisplay != null) date1MonthDisplay.setOnClickListener(v -> date1Month.performClick());
        if (date2Day != null && date2DayDisplay != null) date2DayDisplay.setOnClickListener(v -> date2Day.performClick());
        if (date2Month != null && date2MonthDisplay != null) date2MonthDisplay.setOnClickListener(v -> date2Month.performClick());
    }

    private static int getMaxDays(int month1Based, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month1Based - 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private static ArrayAdapter<String> createDateSpinnerAdapter(Context ctx, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, R.layout.spinner_item_date, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                hideSpinnerSelectedText(v);
                return v;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                applySpinnerItemStyle(ctx, v);
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_item_date);
        return adapter;
    }

    /** Hides the Spinner's built-in selected text so only our overlay TextView is visible (no double/overlapping text). */
    private static void hideSpinnerSelectedText(View itemView) {
        if (itemView == null) return;
        TextView tv = itemView.findViewById(android.R.id.text1);
        if (tv != null) tv.setTextColor(Color.TRANSPARENT);
    }

    private static void applySpinnerItemStyle(Context ctx, View itemView) {
        if (itemView == null || ctx == null) return;
        TextView tv = itemView.findViewById(android.R.id.text1);
        if (tv != null) {
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.date_spinner_text));
            tv.setTextSize(17);
        }
    }

    private static void initMonthSpinner(Context ctx, Spinner monthSpinner) {
        if (monthSpinner == null || ctx == null) return;
        String[] shortMonths = DateFormatSymbols.getInstance(Locale.getDefault()).getShortMonths();
        List<String> months = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            int idx = i - 1;
            String abbr = (idx < shortMonths.length && shortMonths[idx] != null && !shortMonths[idx].isEmpty())
                    ? shortMonths[idx] : getMonthAbbrFallback(i);
            months.add(String.format(Locale.US, "%02d %s", i, abbr));
        }
        monthSpinner.setAdapter(createDateSpinnerAdapter(ctx, months));
    }

    private static String getMonthAbbrFallback(int month1Based) {
        String[] abbr = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return month1Based >= 1 && month1Based <= 12 ? abbr[month1Based] : "";
    }

    private static void refreshDaySpinner(Context ctx, Spinner daySpinner, int month1Based, int year) {
        if (daySpinner == null || ctx == null) return;
        int maxDays = getMaxDays(month1Based, year);
        int currentDay = 1;
        if (daySpinner.getAdapter() != null && daySpinner.getSelectedItemPosition() >= 0) {
            try {
                Object item = daySpinner.getSelectedItem();
                if (item != null) currentDay = parseDayFromDisplay(item.toString());
            } catch (Exception ignored) {}
        }
        if (currentDay > maxDays) currentDay = maxDays;
        List<String> days = new ArrayList<>();
        for (int i = 1; i <= maxDays; i++) days.add(String.format(Locale.US, "%02d", i));
        daySpinner.setAdapter(createDateSpinnerAdapter(ctx, days));
        daySpinner.setSelection(Math.max(0, Math.min(currentDay - 1, maxDays - 1)));
    }

    /** Parses day from display value "01", "02", ... or "1", "2", ... */
    private static int parseDayFromDisplay(String s) {
        if (s == null || s.isEmpty()) return 1;
        try {
            return Integer.parseInt(s.trim().split("\\s+")[0]);
        } catch (Exception e) {
            return Integer.parseInt(s.trim());
        }
    }

    private static void setupMonthYearListeners(Context ctx, Button btnCalculate,
                                                 Spinner date1Day, Spinner date1Month, EditText date1Year,
                                                 Spinner date2Day, Spinner date2Month, EditText date2Year,
                                                 TextView date1DayDisplay, TextView date1MonthDisplay,
                                                 TextView date2DayDisplay, TextView date2MonthDisplay) {
        if (date1Month != null && date1Day != null && date1Year != null) {
            date1Month.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int m = position + 1;
                    int y = parseInt(date1Year.getText(), Calendar.getInstance().get(Calendar.YEAR));
                    if (y < MIN_YEAR) y = MIN_YEAR;
                    if (y > MAX_YEAR) y = MAX_YEAR;
                    refreshDaySpinner(ctx, date1Day, m, y);
                    syncDayDisplay(date1Day, date1DayDisplay);
                    syncMonthDisplay(date1Month, date1MonthDisplay);
                    if (btnCalculate != null) btnCalculate.setEnabled(true);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            date1Year.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable e) {
                    int m = date1Month.getSelectedItemPosition() + 1;
                    int y = parseInt(date1Year.getText(), Calendar.getInstance().get(Calendar.YEAR));
                    if (m >= 1 && m <= 12 && y >= MIN_YEAR && y <= MAX_YEAR)
                        refreshDaySpinner(ctx, date1Day, m, y);
                    syncDayDisplay(date1Day, date1DayDisplay);
                    if (btnCalculate != null) btnCalculate.setEnabled(true);
                }
            });
        }
    }

    private static void fillDateWithToday(Context ctx, Spinner daySpinner, Spinner monthSpinner, EditText yearEt) {
        if (daySpinner == null || monthSpinner == null || yearEt == null || ctx == null) return;
        Calendar cal = Calendar.getInstance();
        int month1Based = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        monthSpinner.setSelection(month1Based - 1);
        yearEt.setText(String.valueOf(year));
        refreshDaySpinner(ctx, daySpinner, month1Based, year);
        daySpinner.setSelection(day - 1);
    }

    private static void addDate2ChangeListeners(Context ctx, Button btnCalculate,
                                                CheckBox useTodayCheckbox, boolean[] settingDateFromCheckbox,
                                                Spinner date1Day, Spinner date1Month, EditText date1Year,
                                                Spinner date2Day, Spinner date2Month, EditText date2Year,
                                                TextView date2DayDisplay, TextView date2MonthDisplay) {
        if (date2Year != null && useTodayCheckbox != null) {
            date2Year.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable e) {
                    if (!settingDateFromCheckbox[0]) useTodayCheckbox.setChecked(false);
                    if (date2Month != null && date2Day != null && ctx != null) {
                        int m = getSpinnerMonth(date2Month);
                        int y = parseInt(date2Year.getText(), Calendar.getInstance().get(Calendar.YEAR));
                        if (m >= 1 && m <= 12 && y >= MIN_YEAR && y <= MAX_YEAR)
                            refreshDaySpinner(ctx, date2Day, m, y);
                    }
                    syncDayDisplay(date2Day, date2DayDisplay);
                    if (btnCalculate != null) btnCalculate.setEnabled(true);
                }
            });
        }
        if (date2Day != null && useTodayCheckbox != null) {
            date2Day.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!settingDateFromCheckbox[0]) useTodayCheckbox.setChecked(false);
                    syncDayDisplay(date2Day, date2DayDisplay);
                    if (btnCalculate != null) btnCalculate.setEnabled(true);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        if (date2Month != null && date2Day != null && date2Year != null) {
            date2Month.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!settingDateFromCheckbox[0] && useTodayCheckbox != null) useTodayCheckbox.setChecked(false);
                    int m = position + 1;
                    int y = parseInt(date2Year.getText(), Calendar.getInstance().get(Calendar.YEAR));
                    if (y < MIN_YEAR) y = MIN_YEAR;
                    if (y > MAX_YEAR) y = MAX_YEAR;
                    if (ctx != null) refreshDaySpinner(ctx, date2Day, m, y);
                    syncDayDisplay(date2Day, date2DayDisplay);
                    syncMonthDisplay(date2Month, date2MonthDisplay);
                    if (btnCalculate != null) btnCalculate.setEnabled(true);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private static void setCalendarPicker(View panel, Context ctx, int buttonId, Spinner daySpinner, Spinner monthSpinner, EditText yearEt,
                                          boolean isDate1, CheckBox useTodayCheckbox,
                                          TextView dayDisplay, TextView monthDisplay,
                                          Runnable onDateSet) {
        View btn = panel.findViewById(buttonId);
        if (btn == null || daySpinner == null || monthSpinner == null || yearEt == null) return;
        btn.setOnClickListener(v -> {
            int y = parseInt(yearEt.getText(), Calendar.getInstance().get(Calendar.YEAR));
            int m = monthSpinner.getSelectedItemPosition() + 1;
            int d = 1;
            if (daySpinner.getAdapter() != null && daySpinner.getSelectedItemPosition() >= 0) {
                try {
                    Object item = daySpinner.getSelectedItem();
                    if (item != null) d = Integer.parseInt(item.toString());
                } catch (Exception ignored) {}
            }
            if (m < 1) m = 1;
            if (m > 12) m = 12;
            DatePickerDialog dlg = new DatePickerDialog(ctx,
                    (view, year, month, dayOfMonth) -> {
                        yearEt.setText(String.valueOf(year));
                        monthSpinner.setSelection(month);
                        if (ctx != null) refreshDaySpinner(ctx, daySpinner, month + 1, year);
                        daySpinner.setSelection(dayOfMonth - 1);
                        syncDayDisplay(daySpinner, dayDisplay);
                        syncMonthDisplay(monthSpinner, monthDisplay);
                        clearError(yearEt);
                        if (useTodayCheckbox != null) useTodayCheckbox.setChecked(false);
                        if (onDateSet != null) onDateSet.run();
                    }, y, m - 1, d);
            if (isDate1) {
                dlg.getDatePicker().setMaxDate(System.currentTimeMillis());
            }
            dlg.show();
        });
    }

    private static void addValidationListeners(Context ctx, Button btnCalculate, boolean isAgeMode,
                                                Spinner date1Day, Spinner date1Month, EditText date1Year,
                                                Spinner date2Day, Spinner date2Month, EditText date2Year,
                                                TextView date1DayDisplay, TextView date1MonthDisplay,
                                                TextView date2DayDisplay, TextView date2MonthDisplay) {
        if (date1Year != null) {
            date1Year.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable e) {
                    clearError(date1Year);
                    updateCalculateState(ctx, btnCalculate, isAgeMode, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
                }
            });
        }
        if (date2Year != null) {
            date2Year.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable e) {
                    clearError(date2Year);
                    updateCalculateState(ctx, btnCalculate, isAgeMode, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
                }
            });
        }
        AdapterView.OnItemSelectedListener refresh = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                syncDayDisplay(date1Day, date1DayDisplay);
                syncMonthDisplay(date1Month, date1MonthDisplay);
                syncDayDisplay(date2Day, date2DayDisplay);
                syncMonthDisplay(date2Month, date2MonthDisplay);
                updateCalculateState(ctx, btnCalculate, isAgeMode, date1Day, date1Month, date1Year, date2Day, date2Month, date2Year);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        if (date1Day != null) date1Day.setOnItemSelectedListener(refresh);
    }

    private static void updateCalculateState(Context ctx, Button btnCalculate, boolean isAgeMode,
                                             Spinner date1Day, Spinner date1Month, EditText date1Year,
                                             Spinner date2Day, Spinner date2Month, EditText date2Year) {
        if (btnCalculate == null) return;
        btnCalculate.setEnabled(true);
    }

    private static boolean isInputValid(boolean isAgeMode, Spinner date1Day, Spinner date1Month, EditText date1Year,
                                         Spinner date2Day, Spinner date2Month, EditText date2Year) {
        if (isAgeMode) {
            String e1 = validateDateFields(null, date1Day, date1Month, date1Year, true);
            String e2 = validateDateFields(null, date2Day, date2Month, date2Year, false);
            if (e1 != null || e2 != null) return false;
            Calendar birth = toCalendar(date1Day, date1Month, date1Year);
            Calendar asOf = toCalendar(date2Day, date2Month, date2Year);
            return birth != null && asOf != null && !asOf.before(birth);
        }
        String e1 = validateDateFields(null, date1Day, date1Month, date1Year, false);
        String e2 = validateDateFields(null, date2Day, date2Month, date2Year, false);
        if (e1 != null || e2 != null) return false;
        Calendar start = toCalendar(date1Day, date1Month, date1Year);
        Calendar end = toCalendar(date2Day, date2Month, date2Year);
        return start != null && end != null && !end.before(start);
    }

    private static boolean validateAndShowErrors(Context ctx, boolean isAgeMode,
                                                  Spinner date1Day, Spinner date1Month, EditText date1Year,
                                                  Spinner date2Day, Spinner date2Month, EditText date2Year) {
        if (isAgeMode) {
            String err1 = validateDateFields(ctx, date1Day, date1Month, date1Year, true);
            if (err1 != null) {
                Toast.makeText(ctx, err1, Toast.LENGTH_SHORT).show();
                return false;
            }
            String err2 = validateDateFields(ctx, date2Day, date2Month, date2Year, false);
            if (err2 != null) {
                Toast.makeText(ctx, err2, Toast.LENGTH_SHORT).show();
                return false;
            }
            Calendar birth = toCalendar(date1Day, date1Month, date1Year);
            Calendar asOf = toCalendar(date2Day, date2Month, date2Year);
            if (birth != null && asOf != null && asOf.before(birth)) {
                if (date2Year != null) date2Year.setError(ctx.getString(R.string.date_error_end_before_start));
                Toast.makeText(ctx, R.string.date_error_end_before_start, Toast.LENGTH_SHORT).show();
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

    private static int getSpinnerDay(Spinner daySpinner) {
        if (daySpinner == null || daySpinner.getSelectedItemPosition() < 0) return 1;
        try {
            Object item = daySpinner.getSelectedItem();
            return item != null ? parseDayFromDisplay(item.toString()) : 1;
        } catch (Exception e) { return 1; }
    }

    private static int getSpinnerMonth(Spinner monthSpinner) {
        if (monthSpinner == null || monthSpinner.getSelectedItemPosition() < 0) return 1;
        return monthSpinner.getSelectedItemPosition() + 1;
    }

    private static String validateDateFields(Context ctx, Spinner daySpinner, Spinner monthSpinner, EditText yearEt, boolean preventFutureDob) {
        if (yearEt == null) return ctx != null ? " " : null;
        clearError(yearEt);
        if (isEmpty(yearEt)) {
            if (ctx != null) yearEt.setError(ctx.getString(R.string.date_error_fill_all));
            return ctx != null ? ctx.getString(R.string.date_error_fill_all) : " ";
        }
        int d = getSpinnerDay(daySpinner);
        int m = getSpinnerMonth(monthSpinner);
        int y = parseInt(yearEt.getText(), 0);
        if (y < MIN_YEAR || y > MAX_YEAR) {
            if (ctx != null) yearEt.setError(ctx.getString(R.string.date_error_invalid_year));
            return ctx != null ? ctx.getString(R.string.date_error_invalid_year) : " ";
        }
        int maxDays = getMaxDays(m, y);
        if (d < 1 || d > maxDays) {
            if (ctx != null) yearEt.setError(ctx.getString(R.string.date_error_invalid_day));
            return ctx != null ? ctx.getString(R.string.date_error_invalid_day) : " ";
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, y);
        cal.set(Calendar.MONTH, m - 1);
        cal.set(Calendar.DAY_OF_MONTH, d);
        if (preventFutureDob && cal.after(Calendar.getInstance())) {
            if (ctx != null) yearEt.setError(ctx.getString(R.string.date_error_future_dob));
            return ctx != null ? ctx.getString(R.string.date_error_future_dob) : " ";
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

    private static Calendar toCalendar(Spinner daySpinner, Spinner monthSpinner, EditText yearEt) {
        if (daySpinner == null || monthSpinner == null || yearEt == null) return null;
        int d = getSpinnerDay(daySpinner);
        int m = getSpinnerMonth(monthSpinner) - 1;
        int y = parseInt(yearEt.getText(), Calendar.getInstance().get(Calendar.YEAR));
        if (y < MIN_YEAR || y > MAX_YEAR || m < 0 || m > 11 || d < 1 || d > 31) return null;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, y);
        cal.set(Calendar.MONTH, m);
        cal.set(Calendar.DAY_OF_MONTH, Math.min(d, cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
        return cal;
    }

    private static void computeAge(Context ctx, Spinner date1Day, Spinner date1Month, EditText date1Year,
                                    Spinner date2Day, Spinner date2Month, EditText date2Year,
                                    MaterialCardView resultCard, TextView resultText, TextView resultSub, TextView resultDetail) {
        Calendar birth = toCalendar(date1Day, date1Month, date1Year);
        Calendar asOf = toCalendar(date2Day, date2Month, date2Year);
        if (birth == null || asOf == null || ctx == null) return;
        if (birth.after(asOf)) return;

        int years = asOf.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        int months = asOf.get(Calendar.MONTH) - birth.get(Calendar.MONTH);
        int days = asOf.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH);

        if (days < 0) {
            months--;
            Calendar prev = (Calendar) asOf.clone();
            prev.add(Calendar.MONTH, -1);
            days += prev.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        if (months < 0) {
            years--;
            months += 12;
        }

        String main = ctx.getString(R.string.date_result_age_format, years, months, days);
        long diffMs = asOf.getTimeInMillis() - birth.getTimeInMillis();
        long totalDays = diffMs / (1000L * 60 * 60 * 24);
        int totalMonths = years * 12 + months;
        String sub = ctx.getString(R.string.date_result_total_days, (int) totalDays);

        long totalSeconds = diffMs / 1000;
        long totalMinutes = totalSeconds / 60;
        long totalHours = totalSeconds / 3600;
        long hours = totalSeconds / 3600;
        long mins = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        String totalMonthsStr = ctx.getString(R.string.date_result_total_months, totalMonths);
        String totalHoursStr = ctx.getString(R.string.date_result_total_hours, String.format(Locale.US, "%,d", totalHours));
        String totalMinsStr = ctx.getString(R.string.date_result_total_minutes, String.format(Locale.US, "%,d", totalMinutes));
        String totalSecsStr = ctx.getString(R.string.date_result_total_seconds, String.format(Locale.US, "%,d", totalSeconds));
        String hmsStr = ctx.getString(R.string.date_result_hms_format, (int) hours, (int) mins, (int) secs);
        String detail = ctx.getString(R.string.date_result_detail_title) + "\n" + totalMonthsStr + "\n" + totalHoursStr + "\n" + totalMinsStr + "\n" + totalSecsStr + "\n" + hmsStr;

        showResult(resultCard, resultText, resultSub, resultDetail, main, sub, detail);
    }

    private static void computeDateDifference(Context ctx, Spinner date1Day, Spinner date1Month, EditText date1Year,
                                               Spinner date2Day, Spinner date2Month, EditText date2Year,
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
        showResult(resultCard, resultText, resultSub, null, main, sub, null);
    }

    private static void showResult(MaterialCardView resultCard, TextView resultText, TextView resultSub,
                               TextView resultDetail, String main, String sub, String detail) {
        if (resultCard != null) {
            resultCard.setVisibility(View.VISIBLE);
            resultCard.requestLayout();
        }
        if (resultText != null) resultText.setText(main);
        if (resultSub != null) resultSub.setText(sub);
        if (resultDetail != null) {
            resultDetail.setVisibility(detail != null && !detail.isEmpty() ? View.VISIBLE : View.GONE);
            if (detail != null && !detail.isEmpty()) resultDetail.setText(detail);
        }
    }
}
