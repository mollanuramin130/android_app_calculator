package com.nuramin.calculator;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.calculator.R;

import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.Locale;

/**
 * Date Calculator screen: same toolbar and drawer as MainActivity.
 * Title set to "Date Calculator". Age mode and Date Difference mode with full validation.
 */
public class DateCalculatorActivity extends AppCompatActivity {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;

    private DrawerLayout drawerLayout;
    private EditText date1Day, date1Month, date1Year;
    private EditText date2Day, date2Month, date2Year;
    private View dateLabelSection2, dateRowSection2;
    private TextView dateLabelSection1;
    private Button btnCalculate;
    private MaterialCardView resultCard;
    private TextView resultText, resultSub;
    private RadioGroup modeGroup;
    private boolean isAgeMode = true;

    private static final String PREFS_NAME = "calculator_prefs";
    private static final String KEY_THEME = "theme_mode";
    private static final String FEEDBACK_EMAIL = "mollanuramin130@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_calculator);

        drawerLayout = findViewById(R.id.drawer_layout);
        bindViews();
        setupToolbarTitle();
        setupDrawer();
        setupModeSwitch();
        setupCalendarButtons();
        setupCalculateButton();
        updateSection2Visibility();
        updateLabelSection1();
        updateCalculateState();
        addValidationListeners();
    }

    private void bindViews() {
        date1Day = findViewById(R.id.date1_day);
        date1Month = findViewById(R.id.date1_month);
        date1Year = findViewById(R.id.date1_year);
        date2Day = findViewById(R.id.date2_day);
        date2Month = findViewById(R.id.date2_month);
        date2Year = findViewById(R.id.date2_year);
        dateLabelSection1 = findViewById(R.id.date_label_section1);
        dateLabelSection2 = findViewById(R.id.date_label_section2);
        dateRowSection2 = findViewById(R.id.date_row_section2);
        modeGroup = findViewById(R.id.date_mode_group);
        btnCalculate = findViewById(R.id.date_btn_calculate);
        resultCard = findViewById(R.id.date_result_card);
        resultText = findViewById(R.id.date_result_text);
        resultSub = findViewById(R.id.date_result_sub);
    }

    /** Use existing toolbar; set title to "Date Calculator". */
    private void setupToolbarTitle() {
        TextView toolbarTitle = findViewById(R.id.mode_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.date_calculator_title);
            toolbarTitle.setVisibility(View.VISIBLE);
        }
    }

    /** Drawer: open/close, overflow menu, highlight Date Calculator item, other items go back to Main. */
    private void setupDrawer() {
        View btnDrawer = findViewById(R.id.btn_drawer);
        if (btnDrawer != null && drawerLayout != null) {
            btnDrawer.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));
        }

        View btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> showOverflowMenu(btnMenu));
        }

        // Highlight Date Calculator drawer item (current screen).
        View dateCalcItem = findViewById(R.id.drawer_item_date_calc);
        if (dateCalcItem != null) {
            dateCalcItem.setBackgroundResource(R.drawable.drawer_row_selected);
        }

        setDrawerItemToMain(R.id.drawer_item_basic, "basic");
        setDrawerItemToMain(R.id.drawer_item_temp, "temp");
        setDrawerItemToMain(R.id.drawer_item_emi, "emi");
        setDrawerItemToMain(R.id.drawer_item_interest, "interest");
        setDrawerItemToMain(R.id.drawer_item_currency, "currency");
        setDrawerItemToMain(R.id.drawer_item_history, "basic"); // History opens basic calculator
        // drawer_item_date_calc: do nothing (stay), just close drawer
        View dateItem = findViewById(R.id.drawer_item_date_calc);
        if (dateItem != null) {
            dateItem.setOnClickListener(v -> drawerLayout.closeDrawer(Gravity.START));
        }
    }

    private void setDrawerItemToMain(int itemId, String panelKey) {
        View item = findViewById(itemId);
        if (item != null && drawerLayout != null) {
            item.setOnClickListener(v -> {
                drawerLayout.closeDrawer(Gravity.START);
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra(MainActivity.EXTRA_OPEN_PANEL, panelKey);
                startActivity(i);
                finish();
            });
        }
    }

    private void showOverflowMenu(View anchor) {
        android.widget.PopupWindow popup = new android.widget.PopupWindow(this);
        View menuView = getLayoutInflater().inflate(R.layout.dialog_overflow_menu, null);
        popup.setContentView(menuView);
        popup.setWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(getDrawable(android.R.drawable.screen_background_dark_transparent));

        View clearHistory = menuView.findViewById(R.id.menu_clear_history);
        if (clearHistory != null) {
            clearHistory.setOnClickListener(v -> {
                popup.dismiss();
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra(MainActivity.EXTRA_CLEAR_HISTORY, true);
                startActivity(i);
                finish();
            });
        }
        View chooseTheme = menuView.findViewById(R.id.menu_choose_theme);
        if (chooseTheme != null) {
            chooseTheme.setOnClickListener(v -> {
                popup.dismiss();
                showThemeDialog();
            });
        }
        View privacyPolicy = menuView.findViewById(R.id.menu_privacy_policy);
        if (privacyPolicy != null) {
            privacyPolicy.setOnClickListener(v -> {
                popup.dismiss();
                showPrivacyPolicyDialog();
            });
        }
        View sendFeedback = menuView.findViewById(R.id.menu_send_feedback);
        if (sendFeedback != null) {
            sendFeedback.setOnClickListener(v -> {
                popup.dismiss();
                sendFeedbackEmail();
            });
        }
        View help = menuView.findViewById(R.id.menu_help);
        if (help != null) {
            help.setOnClickListener(v -> {
                popup.dismiss();
                showHelpDialog();
            });
        }
        popup.showAsDropDown(anchor != null ? anchor : menuView, 0, 0);
    }

    private void applyThemeMode(int mode) {
        switch (mode) {
            case 0: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }

    private void showThemeDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int current = prefs.getInt(KEY_THEME, 2);
        String[] options = { getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system) };
        new AlertDialog.Builder(this)
                .setTitle(R.string.theme_dialog_title)
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    prefs.edit().putInt(KEY_THEME, which).apply();
                    applyThemeMode(which);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .show();
    }

    private void showPrivacyPolicyDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.privacy_policy_title)
                .setMessage(getString(R.string.privacy_policy_content))
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .show();
    }

    private void sendFeedbackEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + FEEDBACK_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, "");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(intent, getString(R.string.menu_send_feedback)));
        }
    }

    private void showHelpDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_help, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();
        View close = view.findViewById(R.id.help_close);
        if (close != null) close.setOnClickListener(v -> dialog.dismiss());
        View contactDeveloper = view.findViewById(R.id.help_contact_developer);
        if (contactDeveloper != null) {
            contactDeveloper.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + FEEDBACK_EMAIL));
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.help_email_subject));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(intent, getString(R.string.help_contact_developer)));
                }
            });
        }
        dialog.show();
    }

    private void setupModeSwitch() {
        if (modeGroup == null) return;
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isAgeMode = (checkedId == R.id.date_mode_age);
            updateSection2Visibility();
            updateLabelSection1();
            clearErrors();
            updateCalculateState();
        });
    }

    private void updateLabelSection1() {
        if (dateLabelSection1 == null) return;
        dateLabelSection1.setText(isAgeMode ? R.string.date_dob_label : R.string.date_start_date_label);
    }

    private void updateSection2Visibility() {
        if (dateLabelSection2 != null) dateLabelSection2.setVisibility(isAgeMode ? View.GONE : View.VISIBLE);
        if (dateRowSection2 != null) dateRowSection2.setVisibility(isAgeMode ? View.GONE : View.VISIBLE);
    }

    private void setupCalendarButtons() {
        setCalendarPicker(R.id.date1_calendar, date1Day, date1Month, date1Year);
        setCalendarPicker(R.id.date2_calendar, date2Day, date2Month, date2Year);
    }

    private void setCalendarPicker(int buttonId, EditText dayEt, EditText monthEt, EditText yearEt) {
        View btn = findViewById(buttonId);
        if (btn == null || dayEt == null || monthEt == null || yearEt == null) return;
        btn.setOnClickListener(v -> {
            int y = parseInt(yearEt.getText(), Calendar.getInstance().get(Calendar.YEAR));
            int m = parseInt(monthEt.getText(), 1) - 1;
            int d = parseInt(dayEt.getText(), 1);
            if (m < 0) m = 0;
            if (m > 11) m = 11;
            DatePickerDialog dlg = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        yearEt.setText(String.valueOf(year));
                        monthEt.setText(String.format(Locale.US, "%d", month + 1));
                        dayEt.setText(String.format(Locale.US, "%d", dayOfMonth));
                        clearError(dayEt);
                        clearError(monthEt);
                        clearError(yearEt);
                        updateCalculateState();
                    }, y, m, d);
            if (isAgeMode && dayEt == date1Day) {
                dlg.getDatePicker().setMaxDate(System.currentTimeMillis());
            }
            dlg.show();
        });
    }

    private void setupCalculateButton() {
        if (btnCalculate == null) return;
        btnCalculate.setOnClickListener(v -> {
            if (!validateAndShowErrors()) return;
            if (isAgeMode) {
                computeAge();
            } else {
                computeDateDifference();
            }
        });
    }

    private void addValidationListeners() {
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
                        updateCalculateState();
                    }
                });
            }
        }
    }

    private void updateCalculateState() {
        if (btnCalculate == null) return;
        btnCalculate.setEnabled(isInputValid());
    }

    private boolean isInputValid() {
        if (isAgeMode) {
            return validateDateFields(date1Day, date1Month, date1Year, true) == null;
        }
        String e1 = validateDateFields(date1Day, date1Month, date1Year, false);
        String e2 = validateDateFields(date2Day, date2Month, date2Year, false);
        if (e1 != null || e2 != null) return false;
        Calendar start = toCalendar(date1Day, date1Month, date1Year);
        Calendar end = toCalendar(date2Day, date2Month, date2Year);
        return start != null && end != null && !end.before(start);
    }

    private boolean validateAndShowErrors() {
        if (isAgeMode) {
            String err = validateDateFields(date1Day, date1Month, date1Year, true);
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
        String e1 = validateDateFields(date1Day, date1Month, date1Year, false);
        if (e1 != null) {
            Toast.makeText(this, e1, Toast.LENGTH_SHORT).show();
            return false;
        }
        String e2 = validateDateFields(date2Day, date2Month, date2Year, false);
        if (e2 != null) {
            Toast.makeText(this, e2, Toast.LENGTH_SHORT).show();
            return false;
        }
        Calendar start = toCalendar(date1Day, date1Month, date1Year);
        Calendar end = toCalendar(date2Day, date2Month, date2Year);
        if (start == null || end == null) return false;
        if (end.before(start)) {
            date2Year.setError(getString(R.string.date_error_end_before_start));
            Toast.makeText(this, R.string.date_error_end_before_start, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String validateDateFields(EditText dayEt, EditText monthEt, EditText yearEt, boolean preventFutureDob) {
        clearError(dayEt);
        clearError(monthEt);
        clearError(yearEt);
        if (isEmpty(dayEt) || isEmpty(monthEt) || isEmpty(yearEt)) {
            return getString(R.string.date_error_fill_all);
        }
        int d = parseInt(dayEt.getText(), 0);
        int m = parseInt(monthEt.getText(), 0);
        int y = parseInt(yearEt.getText(), 0);
        if (d < 1 || d > 31) {
            dayEt.setError(getString(R.string.date_error_invalid_day));
            return getString(R.string.date_error_invalid_day);
        }
        if (m < 1 || m > 12) {
            monthEt.setError(getString(R.string.date_error_invalid_month));
            return getString(R.string.date_error_invalid_month);
        }
        if (y < MIN_YEAR || y > MAX_YEAR) {
            yearEt.setError(getString(R.string.date_error_invalid_year));
            return getString(R.string.date_error_invalid_year);
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, y);
        cal.set(Calendar.MONTH, m - 1);
        cal.set(Calendar.DAY_OF_MONTH, Math.min(d, cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
        if (preventFutureDob && cal.after(Calendar.getInstance())) {
            yearEt.setError(getString(R.string.date_error_future_dob));
            return getString(R.string.date_error_future_dob);
        }
        return null;
    }

    private void clearErrors() {
        clearError(date1Day);
        clearError(date1Month);
        clearError(date1Year);
        clearError(date2Day);
        clearError(date2Month);
        clearError(date2Year);
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

    private void computeAge() {
        Calendar birth = toCalendar(date1Day, date1Month, date1Year);
        if (birth == null) return;
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

        String main = getString(R.string.date_result_age_format, years, months, days);
        long totalDays = (today.getTimeInMillis() - birth.getTimeInMillis()) / (1000 * 60 * 60 * 24);
        String sub = getString(R.string.date_result_total_days, (int) totalDays);
        showResult(main, sub);
    }

    private void computeDateDifference() {
        Calendar start = toCalendar(date1Day, date1Month, date1Year);
        Calendar end = toCalendar(date2Day, date2Month, date2Year);
        if (start == null || end == null || end.before(start)) return;

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

        String main = getString(R.string.date_result_total_days, totalDays);
        String sub = getString(R.string.date_result_breakdown, years, months, days);
        showResult(main, sub);
    }

    private void showResult(String main, String sub) {
        if (resultCard != null) resultCard.setVisibility(View.VISIBLE);
        if (resultText != null) resultText.setText(main);
        if (resultSub != null) resultSub.setText(sub);
    }
}
