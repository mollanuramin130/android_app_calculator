package com.nuramin.calculator;

import com.nuramin.sunsetcoralcalculator.R;

import android.app.ActivityManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.sunsetcoralcalculator.ai.system.ReviewController;
import com.nuramin.sunsetcoralcalculator.ai.system.ShareController;
import com.nuramin.sunsetcoralcalculator.ai.system.UpdateController;
import com.nuramin.calculator.basic.BasicCalculatorScreen;
import com.nuramin.calculator.bmi.BmiCalculatorPanel;
import com.nuramin.calculator.currency.CurrencyPanel;
import com.nuramin.calculator.discount.DiscountCalculatorPanel;
import com.nuramin.calculator.date.DateCalculatorPanel;
import com.nuramin.calculator.emi.EmiCalculatorPanel;
import com.nuramin.calculator.interest.InterestCalculatorPanel;
import com.nuramin.calculator.tax.TaxCalculatorPanel;
import com.nuramin.calculator.temperature.TemperaturePanel;
import com.nuramin.calculator.unitconverter.UnitConverterPanel;
import com.nuramin.calculator.favorites.FavoritesDialogHelper;
import com.nuramin.calculator.util.FavoriteStorage;
import com.nuramin.calculator.util.favorites.FavoritesEngine;
import com.nuramin.calculator.memorygame.MemoryNumberGridGameScreen;
import com.nuramin.calculator.mathspeed.MathSpeedGameController;
import com.nuramin.calculator.puzzle.NumberTargetPuzzleGameScreen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private View panelTax;
    private View panelCurrency;
    private View panelDateCalc;
    private View panelBmi;
    private View panelDiscount;
    private View panelUnitConverter;
    private View panelMemoryGridGame;
    private View panelNumberTargetPuzzle;
    private View panelMathSpeedGame;
    private LinearLayout scientificRows;
    /** Currently visible content panel (calculator, temp, emi, etc.). */
    private View currentPanel;

    private BasicCalculatorScreen basicCalculatorScreen;
    private UpdateController updateController;

    private static final String PREFS_NAME = "calculator_prefs";
    private static final String KEY_THEME = "theme_mode"; // 0=light, 1=dark, 2=system
    private static final String FEEDBACK_EMAIL = "mollanuramin130@gmail.com";

    /** Intent extra: open this panel when MainActivity starts (e.g. from Date Calculator drawer). */
    public static final String EXTRA_OPEN_PANEL = "open_panel"; // values: basic, temp, emi, interest, tax, currency
    /** Intent extra: clear calculator history on launch. */
    public static final String EXTRA_CLEAR_HISTORY = "clear_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        applySavedTheme();
        setContentView(R.layout.activity_main);

        setTaskDescriptionForRecents();

        drawerLayout = findViewById(R.id.drawer_layout);
        mainToolbar = findViewById(R.id.main_toolbar);
        modeTitle = findViewById(R.id.mode_title);
        calculatorPanel = findViewById(R.id.calculator_panel);
        panelTemp = findViewById(R.id.panel_temp);
        panelEmi = findViewById(R.id.panel_emi);
        panelInterest = findViewById(R.id.panel_interest);
        panelTax = findViewById(R.id.panel_tax);
        panelCurrency = findViewById(R.id.panel_currency);
        panelDateCalc = findViewById(R.id.panel_date_calc);
        panelBmi = findViewById(R.id.panel_bmi);
        panelDiscount = findViewById(R.id.panel_discount);
        panelUnitConverter = findViewById(R.id.panel_unit_converter);
        panelMemoryGridGame = findViewById(R.id.panel_memory_grid_game);
        panelNumberTargetPuzzle = findViewById(R.id.panel_number_target_puzzle);
        panelMathSpeedGame = findViewById(R.id.panel_math_speed_game);
        scientificRows = findViewById(R.id.scientific_rows);

        basicCalculatorScreen = new BasicCalculatorScreen(this);
        basicCalculatorScreen.setup();

        View.OnClickListener onOverflowClick = v -> showOverflowMenu(v);

        TemperaturePanel.setup(panelTemp, drawerLayout, onOverflowClick);
        EmiCalculatorPanel.setup(panelEmi, drawerLayout, onOverflowClick);
        InterestCalculatorPanel.setup(panelInterest, drawerLayout, onOverflowClick);
        TaxCalculatorPanel.setup(panelTax, drawerLayout, onOverflowClick);
        CurrencyPanel.setup(panelCurrency, drawerLayout, onOverflowClick);
        DateCalculatorPanel.setup(panelDateCalc, drawerLayout, onOverflowClick);
        BmiCalculatorPanel.setup(panelBmi, drawerLayout, onOverflowClick);
        DiscountCalculatorPanel.setup(panelDiscount, drawerLayout, onOverflowClick);
        UnitConverterPanel.setup(panelUnitConverter, drawerLayout, onOverflowClick);
        MemoryNumberGridGameScreen.setup(panelMemoryGridGame, drawerLayout, onOverflowClick);
        NumberTargetPuzzleGameScreen.setup(panelNumberTargetPuzzle, drawerLayout, onOverflowClick);
        MathSpeedGameController.setup(panelMathSpeedGame, drawerLayout, onOverflowClick);

        setupDrawer();
        setupQuickBar();
        setupBackPress();
        setupInAppUpdate();
        ReviewController.onMainActivityCreate(this, savedInstanceState);

        // If launched from Date Calculator drawer, open the requested panel
        String openPanel = getIntent() != null ? getIntent().getStringExtra(EXTRA_OPEN_PANEL) : null;
        if ("temp".equals(openPanel)) {
            showPanel(panelTemp, R.string.temp_converter_title);
        } else if ("emi".equals(openPanel)) {
            showPanel(panelEmi, R.string.mode_emi);
        } else if ("interest".equals(openPanel)) {
            showPanel(panelInterest, R.string.mode_interest);
        } else if ("tax".equals(openPanel)) {
            showPanel(panelTax, R.string.tax_calculator_title);
        } else if ("currency".equals(openPanel)) {
            showPanel(panelCurrency, R.string.mode_currency);
        } else if ("date_calc".equals(openPanel)) {
            showPanel(panelDateCalc, R.string.date_calculator_title);
        } else if ("bmi".equals(openPanel)) {
            showPanel(panelBmi, R.string.bmi_calculator_title);
        } else if ("discount".equals(openPanel)) {
            showPanel(panelDiscount, R.string.discount_calculator_title);
        } else if ("unit_converter".equals(openPanel)) {
            showPanel(panelUnitConverter, R.string.mode_unit_converter);
        } else if ("memory_grid_game".equals(openPanel)) {
            showPanel(panelMemoryGridGame, R.string.mode_memory_number_grid);
        } else if ("number_target_puzzle".equals(openPanel)) {
            showPanel(panelNumberTargetPuzzle, R.string.mode_number_target_puzzle);
        } else if ("math_speed_game".equals(openPanel)) {
            showPanel(panelMathSpeedGame, R.string.math_speed_game_title);
        } else {
            showPanel(calculatorPanel, 0);
        }
        basicCalculatorScreen.updateDisplay();

        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_CLEAR_HISTORY, false)) {
            if (basicCalculatorScreen != null) basicCalculatorScreen.clearHistory();
        }

    }

    /**
     * Set task label and icon so recent apps / shortcut center show app name and icon
     * instead of package name and default Android icon.
     */
    private void setTaskDescriptionForRecents() {
        String label = getString(R.string.launcher_name);
        setTitle(label);
        int colorPrimary = 0;
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        if (icon == null) {
            icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        }
        if (icon != null) {
            setTaskDescription(new ActivityManager.TaskDescription(label, icon, colorPrimary));
        }
    }

    /**
     * In-app update: register result launcher and create {@link UpdateController}.
     */
    private void setupInAppUpdate() {
        updateController = new UpdateController();

        ActivityResultLauncher<IntentSenderRequest> updateLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // Flexible update flow; cancel is non-blocking.
                    }
                }
        );
        updateController.setUpdateResultLauncher(updateLauncher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FavoriteStorage.processExpiredAutoDeletes(this);
        if (updateController != null) {
            updateController.checkForUpdate(this);
        }
    }

    @Override
    protected void onDestroy() {
        if (updateController != null) {
            updateController.onDestroy();
            updateController = null;
        }
        super.onDestroy();
    }

    /** Wire 3-bars (drawer) and drawer items. Toolbar is shared; 3-bars/3-dots work on all panels. */
    private void setupDrawer() {
        ImageButton btnDrawer = findViewById(R.id.btn_drawer);
        if (btnDrawer != null) {
            btnDrawer.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));
        }

        setDrawerItemClick(R.id.drawer_item_basic, calculatorPanel, R.string.mode_basic_calculator_title);
        setDrawerItemClick(R.id.drawer_item_temp, panelTemp, R.string.temp_converter_title);
        setDrawerItemClick(R.id.drawer_item_emi, panelEmi, R.string.mode_emi);
        setDrawerItemClick(R.id.drawer_item_interest, panelInterest, R.string.mode_interest);
        setDrawerItemClick(R.id.drawer_item_tax, panelTax, R.string.tax_calculator_title);
        setDrawerItemClick(R.id.drawer_item_currency, panelCurrency, R.string.mode_currency);
        setDrawerItemClick(R.id.drawer_item_date_calc, panelDateCalc, R.string.date_calculator_title);
        setDrawerItemClick(R.id.drawer_item_bmi, panelBmi, R.string.bmi_calculator_title);
        setDrawerItemClick(R.id.drawer_item_discount, panelDiscount, R.string.discount_calculator_title);
        setDrawerItemClick(R.id.drawer_item_unit_converter, panelUnitConverter, R.string.mode_unit_converter);
        setDrawerItemClick(R.id.drawer_item_memory_grid_game, panelMemoryGridGame, R.string.mode_memory_number_grid);
        setDrawerItemClick(R.id.drawer_item_number_target_puzzle, panelNumberTargetPuzzle, R.string.mode_number_target_puzzle);
        setDrawerItemClick(R.id.drawer_item_math_speed_game, panelMathSpeedGame, R.string.math_speed_game_title);
        View favoritesItem = findViewById(R.id.drawer_item_favorites);
        if (favoritesItem != null) {
            favoritesItem.setOnClickListener(v -> {
                drawerLayout.closeDrawer(Gravity.START);
                showFavoritesDialog();
            });
        }

        View aiSmartItem = findViewById(R.id.drawer_item_ai_smart);
        if (aiSmartItem != null && drawerLayout != null) {
            aiSmartItem.setOnClickListener(v -> {
                drawerLayout.closeDrawer(Gravity.START);
                startActivity(new Intent(this, com.nuramin.sunsetcoralcalculator.ai.ui.AISmartActivity.class));
            });
        }

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
        if (panelMathSpeedGame != null && currentPanel == panelMathSpeedGame && panel != panelMathSpeedGame) {
            MathSpeedGameController.pauseWhenPanelHidden(panelMathSpeedGame);
        }
        calculatorPanel.setVisibility(panel == calculatorPanel ? View.VISIBLE : View.GONE);
        panelTemp.setVisibility(panel == panelTemp ? View.VISIBLE : View.GONE);
        panelEmi.setVisibility(panel == panelEmi ? View.VISIBLE : View.GONE);
        panelInterest.setVisibility(panel == panelInterest ? View.VISIBLE : View.GONE);
        panelTax.setVisibility(panel == panelTax ? View.VISIBLE : View.GONE);
        panelCurrency.setVisibility(panel == panelCurrency ? View.VISIBLE : View.GONE);
        panelDateCalc.setVisibility(panel == panelDateCalc ? View.VISIBLE : View.GONE);
        panelBmi.setVisibility(panel == panelBmi ? View.VISIBLE : View.GONE);
        panelDiscount.setVisibility(panel == panelDiscount ? View.VISIBLE : View.GONE);
        if (panelUnitConverter != null) panelUnitConverter.setVisibility(panel == panelUnitConverter ? View.VISIBLE : View.GONE);
        if (panelMemoryGridGame != null) panelMemoryGridGame.setVisibility(panel == panelMemoryGridGame ? View.VISIBLE : View.GONE);
        if (panelNumberTargetPuzzle != null) panelNumberTargetPuzzle.setVisibility(panel == panelNumberTargetPuzzle ? View.VISIBLE : View.GONE);
        if (panelMathSpeedGame != null) panelMathSpeedGame.setVisibility(panel == panelMathSpeedGame ? View.VISIBLE : View.GONE);

        // Single app topbar (app_toolbar.xml): same style for all 8 screens. Only title text changes.
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
        currentPanel = panel;
    }

    /**
     * Back button: from any other screen go to basic calculator; from basic/scientific calculator
     * move app to background (expression is preserved until app is terminated or cleared by user).
     */
    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
                    drawerLayout.closeDrawer(Gravity.START);
                    return;
                }
                if (currentPanel != null && currentPanel != calculatorPanel) {
                    showPanel(calculatorPanel, 0);
                    return;
                }
                // On basic/scientific calculator: move to background, do not finish (expression stays)
                moveTaskToBack(true);
            }
        });
    }

    /** Update topbar title when on calculator: "Basic Calculator" or "Scientific mode". */
    private void updateCalculatorModeTitle() {
        if (modeTitle == null || scientificRows == null || calculatorPanel == null) return;
        if (calculatorPanel.getVisibility() != View.VISIBLE) return;
        boolean scientificVisible = scientificRows.getVisibility() == View.VISIBLE;
        modeTitle.setText(scientificVisible ? R.string.quick_scientific_mode : R.string.mode_basic_calculator);
    }

    /** Wire quick bar (calculator panel) and 3-dot overflow in app_toolbar; 3-dots work on all panels. */
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
        View checkForUpdates = menuView.findViewById(R.id.menu_check_for_updates);
        if (checkForUpdates != null) {
            checkForUpdates.setOnClickListener(v -> {
                popup.dismiss();
                if (updateController != null) updateController.checkForUpdateManual(MainActivity.this);
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

        int[] loc = new int[2];
        if (anchor != null) anchor.getLocationOnScreen(loc);
        popup.showAsDropDown(anchor != null ? anchor : menuView, 0, 0);
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int mode = Math.max(0, Math.min(2, prefs.getInt(KEY_THEME, 2))); // default system; clamp corrupted prefs
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
        int current = Math.max(0, Math.min(2, prefs.getInt(KEY_THEME, 2)));
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

    private void shareAppLink() {
        ShareController.shareApp(this, getString(R.string.share_via_chooser));
    }

    private void launchInAppReview() {
        ReviewController.openReviewFromMenu(this);
    }

    private void showAddFavoriteDialog() {
        List<FavoriteInputData> detected = buildScreenSpecificFavoriteInputs();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, 0);

        TextView titleLabel = new TextView(this);
        titleLabel.setText(R.string.favorites_item_title_label);
        root.addView(titleLabel);

        EditText titleInput = new EditText(this);
        titleInput.setHint(R.string.favorites_item_title_hint);
        titleInput.setText(buildDefaultFavoriteTitle());
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
            if (t.isEmpty()) t = buildDefaultFavoriteTitle();
            StringBuilder noteBuilder = new StringBuilder();
            for (int i = 0; i < dynamicValueInputs.size(); i++) {
                String fieldVal = dynamicValueInputs.get(i).getText() == null ? "" : dynamicValueInputs.get(i).getText().toString().trim();
                if (fieldVal.isEmpty()) continue;
                if (noteBuilder.length() > 0) noteBuilder.append('\n');
                noteBuilder.append(dynamicLabels.get(i)).append(": ").append(fieldVal);
            }
            appendResultToFavoriteNote(noteBuilder);
            if (noteBuilder.length() == 0) noteBuilder.append(buildDefaultFavoriteNote());
            String n = noteBuilder.toString();
            if (saveFavoriteSmart(t, n, getCurrentScreenName(), dynamicLabels, dynamicValueInputs)) {
                dialog.dismiss();
            }
        }));
        dialog.show();
    }

    private void showFavoritesDialog() {
        FavoritesDialogHelper.show(this);
    }

    private String getCurrentScreenName() {
        if (currentPanel == panelTemp) return getString(R.string.temp_converter_title);
        if (currentPanel == panelEmi) return getString(R.string.mode_emi);
        if (currentPanel == panelInterest) return getString(R.string.mode_interest);
        if (currentPanel == panelTax) return getString(R.string.tax_calculator_title);
        if (currentPanel == panelCurrency) return getString(R.string.mode_currency);
        if (currentPanel == panelDateCalc) return getString(R.string.date_calculator_title);
        if (currentPanel == panelBmi) return getString(R.string.bmi_calculator_title);
        if (currentPanel == panelDiscount) return getString(R.string.discount_calculator_title);
        if (currentPanel == panelUnitConverter) return getString(R.string.mode_unit_converter);
        if (currentPanel == panelMemoryGridGame) return getString(R.string.mode_memory_number_grid);
        if (currentPanel == panelNumberTargetPuzzle) return getString(R.string.mode_number_target_puzzle);
        if (currentPanel == panelMathSpeedGame) return getString(R.string.math_speed_game_title);
        return getString(R.string.mode_basic_calculator);
    }

    private String buildDefaultFavoriteTitle() {
        return getCurrentScreenName();
    }

    private String buildDefaultFavoriteNote() {
        if (currentPanel == calculatorPanel) {
            TextView expr = findViewById(R.id.tv_expression);
            TextView res = findViewById(R.id.tv_result);
            String e = expr != null && expr.getText() != null ? expr.getText().toString().trim() : "";
            String r = res != null && res.getText() != null ? res.getText().toString().trim() : "";
            if (!e.isEmpty() || !r.isEmpty()) return "Equation: " + e + (r.isEmpty() ? "" : " = " + r);
        }
        return "";
    }

    private List<FavoriteInputData> buildScreenSpecificFavoriteInputs() {
        List<FavoriteInputData> out = new ArrayList<>();
        if (currentPanel == panelTemp) {
            addIfValue(out, "Value", textOf(panelTemp, R.id.temp_input));
            addIfValue(out, "From", spinnerOf(panelTemp, R.id.temp_from));
            addIfValue(out, "To", spinnerOf(panelTemp, R.id.temp_to));
            return out;
        }
        if (currentPanel == panelEmi) {
            addIfValue(out, "Principal", textOf(panelEmi, R.id.emi_principal));
            addIfValue(out, "Rate (%)", textOf(panelEmi, R.id.emi_rate));
            addIfValue(out, "Tenure", textOf(panelEmi, R.id.emi_tenure));
            return out;
        }
        if (currentPanel == panelInterest) {
            addIfValue(out, "Principal", textOf(panelInterest, R.id.interest_principal));
            addIfValue(out, "Rate (%)", textOf(panelInterest, R.id.interest_rate));
            addIfValue(out, "Time", textOf(panelInterest, R.id.interest_time));
            addIfValue(out, "Type", radioOf(panelInterest, R.id.interest_type));
            addIfValue(out, "Time unit", spinnerOf(panelInterest, R.id.interest_time_unit));
            return out;
        }
        if (currentPanel == panelTax) {
            addIfValue(out, "Mode", radioOf(panelTax, R.id.tax_mode_group));
            addIfValue(out, "Amount", textOf(panelTax, R.id.tax_amount_input));
            addIfValue(out, "Tax rate (%)", textOf(panelTax, R.id.tax_rate_input));
            return out;
        }
        if (currentPanel == panelCurrency) {
            addIfValue(out, "From", spinnerOf(panelCurrency, R.id.currency_from));
            addIfValue(out, "Amount", textOf(panelCurrency, R.id.currency_amount));
            addIfValue(out, "To", spinnerOf(panelCurrency, R.id.currency_to));
            return out;
        }
        if (currentPanel == panelDateCalc) {
            addIfValue(out, "Date 1", joinDate(
                    textOf(panelDateCalc, R.id.date1_day_display),
                    textOf(panelDateCalc, R.id.date1_month_display),
                    textOf(panelDateCalc, R.id.date1_year)));
            addIfValue(out, "Date 2", joinDate(
                    textOf(panelDateCalc, R.id.date2_day_display),
                    textOf(panelDateCalc, R.id.date2_month_display),
                    textOf(panelDateCalc, R.id.date2_year)));
            return out;
        }
        if (currentPanel == panelBmi) {
            addIfValue(out, "Gender", radioOf(panelBmi, R.id.bmi_gender_toggle));
            addIfValue(out, "Height", textOf(panelBmi, R.id.bmi_height_value));
            addIfValue(out, "Weight", textOf(panelBmi, R.id.bmi_weight_value));
            addIfValue(out, "Age", textOf(panelBmi, R.id.bmi_age_value));
            return out;
        }
        if (currentPanel == panelDiscount) {
            addIfValue(out, "Original", textOf(panelDiscount, R.id.discount_original));
            addIfValue(out, "Discount (%)", textOf(panelDiscount, R.id.discount_percent));
            addIfValue(out, "Extra discount", textOf(panelDiscount, R.id.discount_extra_discount));
            return out;
        }
        if (currentPanel == panelUnitConverter) {
            addIfValue(out, "Category", spinnerOf(panelUnitConverter, R.id.unit_category_spinner));
            addIfValue(out, "Value", textOf(panelUnitConverter, R.id.unit_input_value));
            addIfValue(out, "From", spinnerOf(panelUnitConverter, R.id.unit_from_spinner));
            addIfValue(out, "To", spinnerOf(panelUnitConverter, R.id.unit_to_spinner));
            return out;
        }
        if (currentPanel == calculatorPanel || currentPanel == null) {
            addIfValue(out, "Expression", textOf(this.findViewById(android.R.id.content), R.id.tv_expression));
            addIfValue(out, "Result", textOf(this.findViewById(android.R.id.content), R.id.tv_result));
        }
        return out;
    }

    private void addIfValue(List<FavoriteInputData> out, String label, String value) {
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        out.add(new FavoriteInputData(label, v));
    }

    private String textOf(View root, int id) {
        if (root == null) return "";
        View v = root.findViewById(id);
        if (v instanceof TextView) {
            CharSequence cs = ((TextView) v).getText();
            return cs == null ? "" : cs.toString().trim();
        }
        return "";
    }

    private String spinnerOf(View root, int id) {
        if (root == null) return "";
        View v = root.findViewById(id);
        if (v instanceof Spinner) {
            Object selected = ((Spinner) v).getSelectedItem();
            return selected == null ? "" : String.valueOf(selected).trim();
        }
        return "";
    }

    private String radioOf(View root, int id) {
        if (root == null) return "";
        View v = root.findViewById(id);
        if (v instanceof RadioGroup) {
            int checkedId = ((RadioGroup) v).getCheckedRadioButtonId();
            if (checkedId != View.NO_ID) {
                View checked = ((RadioGroup) v).findViewById(checkedId);
                if (checked instanceof RadioButton) {
                    CharSequence cs = ((RadioButton) checked).getText();
                    return cs == null ? "" : cs.toString().trim();
                }
            }
        }
        return "";
    }

    private String joinDate(String d, String m, String y) {
        if ((d == null || d.isEmpty()) && (m == null || m.isEmpty()) && (y == null || y.isEmpty())) return "";
        return (d == null ? "" : d) + "/" + (m == null ? "" : m) + "/" + (y == null ? "" : y);
    }

    private void appendResultToFavoriteNote(StringBuilder noteBuilder) {
        if (noteBuilder == null) return;
        if (currentPanel == calculatorPanel) {
            String equation = textFromId(R.id.tv_expression);
            String result = textFromId(R.id.tv_result);
            if (!equation.isEmpty() || !result.isEmpty()) {
                if (noteBuilder.length() > 0) noteBuilder.append('\n');
                noteBuilder.append("Result: ");
                if (!equation.isEmpty()) noteBuilder.append(equation);
                if (!result.isEmpty()) {
                    if (!equation.isEmpty()) noteBuilder.append(" = ");
                    noteBuilder.append(result);
                }
            }
            return;
        }
        String resultMain = textOf(currentPanel, R.id.date_result_text);
        String resultSub = textOf(currentPanel, R.id.date_result_sub);
        if (!resultMain.isEmpty() || !resultSub.isEmpty()) {
            if (noteBuilder.length() > 0) noteBuilder.append('\n');
            noteBuilder.append("Result: ").append(resultMain);
            if (!resultSub.isEmpty()) noteBuilder.append(" (").append(resultSub).append(")");
        }
    }

    private String textFromId(int id) {
        View v = findViewById(id);
        if (v instanceof TextView) {
            CharSequence cs = ((TextView) v).getText();
            return cs == null ? "" : cs.toString().trim();
        }
        return "";
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
        FavoritesEngine.Response<Void> check = FavoriteStorage.validateForAdd(payload, null);
        if (!check.success) {
            Toast.makeText(this, FavoriteStorage.formatValidationMessage(this, check), Toast.LENGTH_LONG).show();
            return false;
        }
        FavoritesEngine.Response<FavoritesEngine.FavoriteRecord> response = FavoriteStorage.addAdvanced(
                this,
                payload,
                null,
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

    private static final class FavoriteInputData {
        final String label;
        final String value;
        FavoriteInputData(String label, String value) {
            this.label = label;
            this.value = value;
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
