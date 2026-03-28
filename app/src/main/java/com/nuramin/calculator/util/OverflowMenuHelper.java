package com.nuramin.calculator.util;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.PopupWindow;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.nuramin.sunsetcoralcalculator.R;

/**
 * Shared overflow menu (Theme, Privacy, Feedback, Help) for activities
 * that have a toolbar with a menu icon.
 */
public final class OverflowMenuHelper {

    private static final String PREFS_NAME = "calculator_prefs";
    private static final String KEY_THEME = "theme_mode";
    private static final String FEEDBACK_EMAIL = "mollanuramin130@gmail.com";

    /**
     * Show the same overflow menu as MainActivity.
     *
     * @param activity       the activity (for theme/prefs and dialogs)
     * @param anchor         the view to anchor the popup (e.g. menu ImageButton)
     * @param onClearHistory optional; if null, Clear history row is hidden
     */
    public static void show(AppCompatActivity activity, View anchor, Runnable onClearHistory) {
        View menuView = activity.getLayoutInflater().inflate(R.layout.dialog_overflow_menu, null);

        PopupWindow popup = new PopupWindow(activity);
        popup.setContentView(menuView);
        popup.setWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(activity.getDrawable(android.R.drawable.screen_background_dark_transparent));

        View clearHistory = menuView.findViewById(R.id.menu_clear_history);
        if (clearHistory != null) {
            if (onClearHistory != null) {
                clearHistory.setVisibility(View.VISIBLE);
                clearHistory.setOnClickListener(v -> {
                    popup.dismiss();
                    onClearHistory.run();
                });
            } else {
                clearHistory.setVisibility(View.GONE);
            }
        }

        View chooseTheme = menuView.findViewById(R.id.menu_choose_theme);
        if (chooseTheme != null) {
            chooseTheme.setOnClickListener(v -> {
                popup.dismiss();
                showThemeDialog(activity);
            });
        }
        View privacyPolicy = menuView.findViewById(R.id.menu_privacy_policy);
        if (privacyPolicy != null) {
            privacyPolicy.setOnClickListener(v -> {
                popup.dismiss();
                showPrivacyPolicyDialog(activity);
            });
        }
        View sendFeedback = menuView.findViewById(R.id.menu_send_feedback);
        if (sendFeedback != null) {
            sendFeedback.setOnClickListener(v -> {
                popup.dismiss();
                sendFeedbackEmail(activity);
            });
        }
        View help = menuView.findViewById(R.id.menu_help);
        if (help != null) {
            help.setOnClickListener(v -> {
                popup.dismiss();
                showHelpDialog(activity);
            });
        }

        popup.showAsDropDown(anchor != null ? anchor : menuView, 0, 0);
    }

    /** Public for use from Toolbar menu (onOptionsItemSelected). */
    public static void showThemeDialog(AppCompatActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE);
        int current = Math.max(0, Math.min(2, prefs.getInt(KEY_THEME, 2)));
        String[] options = {
                activity.getString(R.string.theme_light),
                activity.getString(R.string.theme_dark),
                activity.getString(R.string.theme_system)
        };
        new AlertDialog.Builder(activity)
                .setTitle(R.string.theme_dialog_title)
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    prefs.edit().putInt(KEY_THEME, which).apply();
                    applyThemeMode(which);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .show();
    }

    private static void applyThemeMode(int mode) {
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

    /** Public for use from Toolbar menu. */
    public static void showPrivacyPolicyDialog(AppCompatActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.privacy_policy_title)
                .setMessage(activity.getString(R.string.privacy_policy_content))
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .show();
    }

    /** Public for use from Toolbar menu. */
    public static void sendFeedbackEmail(AppCompatActivity activity) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + FEEDBACK_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.feedback_email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, "");
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.menu_send_feedback)));
        }
    }

    /** Public for use from Toolbar menu. */
    public static void showHelpDialog(AppCompatActivity activity) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_help, null);
        AlertDialog dialog = new AlertDialog.Builder(activity)
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
                intent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.help_email_subject));
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.help_contact_developer)));
                }
            });
        }
        dialog.show();
    }
}
