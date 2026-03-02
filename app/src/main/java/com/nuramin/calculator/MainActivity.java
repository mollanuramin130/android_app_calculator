package com.nuramin.calculator;

import com.nuramin.sunsetcoralcalculator.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.calculator.basic.BasicCalculatorScreen;
import com.nuramin.calculator.bmi.BmiCalculatorPanel;
import com.nuramin.calculator.currency.CurrencyPanel;
import com.nuramin.calculator.discount.DiscountCalculatorPanel;
import com.nuramin.calculator.date.DateCalculatorPanel;
import com.nuramin.calculator.emi.EmiCalculatorPanel;
import com.nuramin.calculator.interest.InterestCalculatorPanel;
import com.nuramin.calculator.temperature.TemperaturePanel;

/**
 * Main activity: hosts drawer, toolbar, and all mode panels.
 * Screen-specific logic lives in separate packages (basic, temperature, emi, interest, currency).
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private View mainToolbar;
    private TextView modeTitle;
    private View calculatorPanel;
    private View panelTemp;
    private View panelEmi;
    private View panelInterest;
    private View panelCurrency;
    private View panelDateCalc;
    private View panelBmi;
    private View panelDiscount;
    private LinearLayout scientificRows;

    private BasicCalculatorScreen basicCalculatorScreen;

    private static final String PREFS_NAME = "calculator_prefs";
    private static final String KEY_THEME = "theme_mode"; // 0=light, 1=dark, 2=system
    private static final String FEEDBACK_EMAIL = "mollanuramin130@gmail.com";

    /** Intent extra: open this panel when MainActivity starts (e.g. from Date Calculator drawer). */
    public static final String EXTRA_OPEN_PANEL = "open_panel"; // values: basic, temp, emi, interest, currency
    /** Intent extra: clear calculator history on launch. */
    public static final String EXTRA_CLEAR_HISTORY = "clear_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        mainToolbar = findViewById(R.id.main_toolbar);
        modeTitle = findViewById(R.id.mode_title);
        calculatorPanel = findViewById(R.id.calculator_panel);
        panelTemp = findViewById(R.id.panel_temp);
        panelEmi = findViewById(R.id.panel_emi);
        panelInterest = findViewById(R.id.panel_interest);
        panelCurrency = findViewById(R.id.panel_currency);
        panelDateCalc = findViewById(R.id.panel_date_calc);
        panelBmi = findViewById(R.id.panel_bmi);
        panelDiscount = findViewById(R.id.panel_discount);
        scientificRows = findViewById(R.id.scientific_rows);

        basicCalculatorScreen = new BasicCalculatorScreen(this);
        basicCalculatorScreen.setup();

        View.OnClickListener onOverflowClick = v -> showOverflowMenu(v);

        TemperaturePanel.setup(panelTemp, drawerLayout, onOverflowClick);
        EmiCalculatorPanel.setup(panelEmi, drawerLayout, onOverflowClick);
        InterestCalculatorPanel.setup(panelInterest, drawerLayout, onOverflowClick);
        CurrencyPanel.setup(panelCurrency, drawerLayout, onOverflowClick);
        DateCalculatorPanel.setup(panelDateCalc, drawerLayout, onOverflowClick);
        BmiCalculatorPanel.setup(panelBmi, drawerLayout, onOverflowClick);
        DiscountCalculatorPanel.setup(panelDiscount, drawerLayout, onOverflowClick);

        setupDrawer();
        setupQuickBar();

        // If launched from Date Calculator drawer, open the requested panel
        String openPanel = getIntent() != null ? getIntent().getStringExtra(EXTRA_OPEN_PANEL) : null;
        if ("temp".equals(openPanel)) {
            showPanel(panelTemp, R.string.temp_converter_title);
        } else if ("emi".equals(openPanel)) {
            showPanel(panelEmi, R.string.mode_emi);
        } else if ("interest".equals(openPanel)) {
            showPanel(panelInterest, R.string.mode_interest);
        } else if ("currency".equals(openPanel)) {
            showPanel(panelCurrency, R.string.mode_currency);
        } else if ("date_calc".equals(openPanel)) {
            showPanel(panelDateCalc, R.string.date_calculator_title);
        } else if ("bmi".equals(openPanel)) {
            showPanel(panelBmi, R.string.bmi_calculator_title);
        } else if ("discount".equals(openPanel)) {
            showPanel(panelDiscount, R.string.discount_calculator_title);
        } else {
            showPanel(calculatorPanel, 0);
        }
        basicCalculatorScreen.updateDisplay();

        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_CLEAR_HISTORY, false)) {
            if (basicCalculatorScreen != null) basicCalculatorScreen.clearHistory();
        }
    }

    private void setupDrawer() {
        ImageButton btnDrawer = findViewById(R.id.btn_drawer);
        if (btnDrawer != null) {
            btnDrawer.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));
        }

        setDrawerItemClick(R.id.drawer_item_basic, calculatorPanel, R.string.mode_basic_calculator_title);
        setDrawerItemClick(R.id.drawer_item_temp, panelTemp, R.string.temp_converter_title);
        setDrawerItemClick(R.id.drawer_item_emi, panelEmi, R.string.mode_emi);
        setDrawerItemClick(R.id.drawer_item_interest, panelInterest, R.string.mode_interest);
        setDrawerItemClick(R.id.drawer_item_currency, panelCurrency, R.string.mode_currency);
        setDrawerItemClick(R.id.drawer_item_date_calc, panelDateCalc, R.string.date_calculator_title);
        setDrawerItemClick(R.id.drawer_item_bmi, panelBmi, R.string.bmi_calculator_title);
        setDrawerItemClick(R.id.drawer_item_discount, panelDiscount, R.string.discount_calculator_title);

        View historyItem = findViewById(R.id.drawer_item_history);
        if (historyItem != null) {
            historyItem.setOnClickListener(v -> {
                showPanel(calculatorPanel, 0);
                drawerLayout.closeDrawer(Gravity.START);
            });
        }
    }

    private void setDrawerItemClick(int itemId, View panel, int titleResId) {
        View item = findViewById(itemId);
        if (item != null && panel != null) {
            item.setOnClickListener(v -> {
                showPanel(panel, titleResId);
                drawerLayout.closeDrawer(Gravity.START);
            });
        }
    }

    private void showPanel(View panel, int titleResId) {
        calculatorPanel.setVisibility(panel == calculatorPanel ? View.VISIBLE : View.GONE);
        panelTemp.setVisibility(panel == panelTemp ? View.VISIBLE : View.GONE);
        panelEmi.setVisibility(panel == panelEmi ? View.VISIBLE : View.GONE);
        panelInterest.setVisibility(panel == panelInterest ? View.VISIBLE : View.GONE);
        panelCurrency.setVisibility(panel == panelCurrency ? View.VISIBLE : View.GONE);
        panelDateCalc.setVisibility(panel == panelDateCalc ? View.VISIBLE : View.GONE);
        panelBmi.setVisibility(panel == panelBmi ? View.VISIBLE : View.GONE);
        panelDiscount.setVisibility(panel == panelDiscount ? View.VISIBLE : View.GONE);

        // Always show main toolbar (same as Basic Calculator); only title changes per screen.
        boolean isBasicCalculator = (panel == calculatorPanel);
        if (mainToolbar != null) {
            mainToolbar.setVisibility(View.VISIBLE);
        }
        if (modeTitle != null) {
            modeTitle.setVisibility(View.VISIBLE);
            if (isBasicCalculator) {
                updateCalculatorModeTitle();
            } else {
                modeTitle.setText(titleResId);
            }
        }
        if (panel == panelCurrency) {
            com.nuramin.calculator.currency.CurrencyPanel.onPanelVisible(this, panelCurrency);
        }
    }

    /** Update topbar title when on calculator: "Basic Calculator" or "Scientific mode". */
    private void updateCalculatorModeTitle() {
        if (modeTitle == null || scientificRows == null || calculatorPanel == null) return;
        if (calculatorPanel.getVisibility() != View.VISIBLE) return;
        boolean scientificVisible = scientificRows.getVisibility() == View.VISIBLE;
        modeTitle.setText(scientificVisible ? R.string.quick_scientific_mode : R.string.mode_basic_calculator);
    }

    private void setupQuickBar() {
        View quickScientific = findViewById(R.id.quick_scientific_toggle_wrapper);
        if (quickScientific != null && scientificRows != null) {
            quickScientific.setOnClickListener(v -> {
                int vis = scientificRows.getVisibility();
                scientificRows.setVisibility(vis == View.VISIBLE ? View.GONE : View.VISIBLE);
                updateCalculatorModeTitle();
            });
        }
        View quickModes = findViewById(R.id.quick_modes_wrapper);
        if (quickModes != null) {
            quickModes.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));
        }
        ImageButton btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> showOverflowMenu(v));
        }
    }

    /**
     * Show overflow menu (e.g. Clear history, Theme, Privacy, Feedback, Help).
     * Override or extend to add menu actions.
     */
    protected void showOverflowMenu(View anchor) {
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
                if (basicCalculatorScreen != null) basicCalculatorScreen.clearHistory();
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

        int[] loc = new int[2];
        if (anchor != null) anchor.getLocationOnScreen(loc);
        popup.showAsDropDown(anchor != null ? anchor : menuView, 0, 0);
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int mode = prefs.getInt(KEY_THEME, 2); // default system
        applyThemeMode(mode);
    }

    private void applyThemeMode(int mode) {
        switch (mode) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void showThemeDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int current = prefs.getInt(KEY_THEME, 2);
        String[] options = {
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_system)
        };
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
}
