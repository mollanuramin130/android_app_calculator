package com.nuramin.calculator.date;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.calculator.MainActivity;
import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.sunsetcoralcalculator.ai.system.ReviewController;
import com.nuramin.sunsetcoralcalculator.ai.system.ShareController;

import com.google.android.material.card.MaterialCardView;
import com.nuramin.calculator.favorites.FavoritesDialogHelper;
import com.nuramin.calculator.util.FavoriteStorage;
import com.nuramin.calculator.util.favorites.FavoritesEngine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Legacy Date Calculator Activity (full-screen with own toolbar/drawer).
 * The app now uses DateCalculatorPanel inside MainActivity for the same flow as other screens.
 * This Activity is kept for backward compatibility if started by an intent; normally not used.
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
        applySavedTheme();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_date_calculator);

        drawerLayout = findViewById(R.id.drawer_layout);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
                    drawerLayout.closeDrawer(Gravity.START);
                    return;
                }
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
        });
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

    @Override
    protected void onResume() {
        super.onResume();
        FavoriteStorage.processExpiredAutoDeletes(this);
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
        View favoritesItem = findViewById(R.id.drawer_item_favorites);
        if (favoritesItem != null && drawerLayout != null) {
            favoritesItem.setOnClickListener(v -> {
                drawerLayout.closeDrawer(Gravity.START);
                showFavoritesDialog();
            });
        }
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
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
        View addFavorite = menuView.findViewById(R.id.menu_add_favorite);
        if (addFavorite != null) {
            addFavorite.setOnClickListener(v -> {
                popup.dismiss();
                showAddFavoriteDialog();
            });
        }
        View shareApp = menuView.findViewById(R.id.menu_share_app);
        if (shareApp != null) {
            shareApp.setOnClickListener(v -> {
                popup.dismiss();
                shareAppLink();
            });
        }
        View rateReview = menuView.findViewById(R.id.menu_rate_review);
        if (rateReview != null) {
            rateReview.setOnClickListener(v -> {
                popup.dismiss();
                launchInAppReview();
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

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int mode = Math.max(0, Math.min(2, prefs.getInt(KEY_THEME, 2)));
        applyThemeMode(mode);
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
        int current = Math.max(0, Math.min(2, prefs.getInt(KEY_THEME, 2)));
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

    private void shareAppLink() {
        ShareController.shareApp(this, getString(R.string.share_via_chooser));
    }

    private void launchInAppReview() {
        ReviewController.openReviewFromMenu(this);
    }

    private void showAddFavoriteDialog() {
        List<FavoriteInputData> detected = buildDateFavoriteInputs();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, 0);

        TextView titleLabel = new TextView(this);
        titleLabel.setText(R.string.favorites_item_title_label);
        root.addView(titleLabel);

        EditText titleInput = new EditText(this);
        titleInput.setHint(R.string.favorites_item_title_hint);
        titleInput.setText(getString(R.string.date_calculator_title));
        root.addView(titleInput);

        List<EditText> dynamicValueInputs = new ArrayList<>();
        List<String> dynamicLabels = new ArrayList<>();
        if (!detected.isEmpty()) {
            for (FavoriteInputData data : detected) {
                TextView fieldLabel = new TextView(this);
                fieldLabel.setText(data.label);
                fieldLabel.setPadding(0, pad / 2, 0, 0);
                root.addView(fieldLabel);

                EditText fieldInput = new EditText(this);
                fieldInput.setText(data.value);
                root.addView(fieldInput);

                dynamicLabels.add(data.label);
                dynamicValueInputs.add(fieldInput);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.favorites_add_title)
                .setView(root)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String t = titleInput.getText() == null ? "" : titleInput.getText().toString().trim();
            if (t.isEmpty()) t = getString(R.string.date_calculator_title);
            StringBuilder noteBuilder = new StringBuilder();
            for (int i = 0; i < dynamicValueInputs.size(); i++) {
                String fieldVal = dynamicValueInputs.get(i).getText() == null ? "" : dynamicValueInputs.get(i).getText().toString().trim();
                if (fieldVal.isEmpty()) continue;
                if (noteBuilder.length() > 0) noteBuilder.append('\n');
                noteBuilder.append(dynamicLabels.get(i)).append(": ").append(fieldVal);
            }
            appendResultToFavoriteNote(noteBuilder);
            if (noteBuilder.length() == 0) noteBuilder.append(buildDateFavoriteNote());
            String n = noteBuilder.toString();
            if (saveFavoriteSmart(t, n, getString(R.string.date_calculator_title), dynamicLabels, dynamicValueInputs)) {
                dialog.dismiss();
            }
        }));
        dialog.show();
    }

    private String buildDateFavoriteNote() {
        String d = date1Day != null && date1Day.getText() != null ? date1Day.getText().toString().trim() : "";
        String m = date1Month != null && date1Month.getText() != null ? date1Month.getText().toString().trim() : "";
        String y = date1Year != null && date1Year.getText() != null ? date1Year.getText().toString().trim() : "";
        if (d.isEmpty() && m.isEmpty() && y.isEmpty()) return "";
        return "Date: " + d + "/" + m + "/" + y;
    }

    private List<FavoriteInputData> buildDateFavoriteInputs() {
        List<FavoriteInputData> out = new ArrayList<>();
        addIfValue(out, "Mode", currentDateMode());
        addIfValue(out, "Date 1", joinDate(text(date1Day), text(date1Month), text(date1Year)));
        if (!isAgeMode) {
            addIfValue(out, "Date 2", joinDate(text(date2Day), text(date2Month), text(date2Year)));
        }
        return out;
    }

    private String currentDateMode() {
        if (modeGroup == null) return "";
        int checkedId = modeGroup.getCheckedRadioButtonId();
        if (checkedId == View.NO_ID) return "";
        View checked = modeGroup.findViewById(checkedId);
        if (checked instanceof RadioButton) {
            CharSequence cs = ((RadioButton) checked).getText();
            return cs == null ? "" : cs.toString().trim();
        }
        return "";
    }

    private String text(EditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    private String joinDate(String d, String m, String y) {
        if ((d == null || d.isEmpty()) && (m == null || m.isEmpty()) && (y == null || y.isEmpty())) return "";
        return (d == null ? "" : d) + "/" + (m == null ? "" : m) + "/" + (y == null ? "" : y);
    }

    private void appendResultToFavoriteNote(StringBuilder noteBuilder) {
        if (noteBuilder == null) return;
        String main = resultText != null && resultText.getText() != null ? resultText.getText().toString().trim() : "";
        String sub = resultSub != null && resultSub.getText() != null ? resultSub.getText().toString().trim() : "";
        if (main.isEmpty() && sub.isEmpty()) return;
        if (noteBuilder.length() > 0) noteBuilder.append('\n');
        noteBuilder.append("Result: ").append(main);
        if (!sub.isEmpty()) noteBuilder.append(" (").append(sub).append(")");
    }

    private void addIfValue(List<FavoriteInputData> out, String label, String value) {
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        out.add(new FavoriteInputData(label, v));
    }

    private static final class FavoriteInputData {
        final String label;
        final String value;
        FavoriteInputData(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private void showFavoritesDialog() {
        FavoritesDialogHelper.show(this);
    }

    private Map<String, String> buildFavoritePayload(String title, String note, String screen, List<String> labels, List<EditText> values) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("note", note);
        payload.put("screen", screen);
        if (labels != null && values != null) {
            for (int i = 0; i < labels.size() && i < values.size(); i++) {
                String k = labels.get(i) == null ? "" : labels.get(i).trim();
                String val = values.get(i) != null && values.get(i).getText() != null
                        ? values.get(i).getText().toString().trim() : "";
                if (!k.isEmpty() && !val.isEmpty()) payload.put(k, val);
            }
        }
        return payload;
    }

    private boolean saveFavoriteSmart(String title, String note, String screen, List<String> labels, List<EditText> values) {
        Map<String, String> payload = buildFavoritePayload(title, note, screen, labels, values);
        FavoritesEngine.Response<Void> check = FavoriteStorage.validateForAdd(payload, "date");
        if (!check.success) {
            Toast.makeText(this, FavoriteStorage.formatValidationMessage(this, check), Toast.LENGTH_LONG).show();
            return false;
        }
        FavoritesEngine.Response<FavoritesEngine.FavoriteRecord> response = FavoriteStorage.addAdvanced(
                this,
                payload,
                "date",
                FavoritesEngine.DuplicatePolicy.MERGE
        );
        if (!response.success) {
            Toast.makeText(this, "Could not save: " + (response.error == null ? "Unknown error" : response.error.message), Toast.LENGTH_SHORT).show();
            return false;
        }
        boolean merged = response.metadata.containsKey("merged_into");
        boolean duplicate = response.metadata.containsKey("duplicate");
        if (merged || duplicate) {
            Toast.makeText(this, "Favourite updated (duplicate merged)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.favorites_saved, Toast.LENGTH_SHORT).show();
        }
        return true;
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
