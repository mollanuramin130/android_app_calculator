package com.nuramin.sunsetcoralcalculator.ai.system;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.nuramin.sunsetcoralcalculator.R;

/**
 * In-app review (Play Core) with extra rules stored locally.
 * <p>
 * The Play API does not expose star ratings or review text. We use:
 * <ul>
 *   <li>A pre-dialog {@link RatingBar} so 4–5★ can be treated as “happy” (never prompt again).</li>
 *   <li>1–3★ → ask again after 3 days, max 3 attempts.</li>
 *   <li>“Write a review on Play Store” → opens listing; we treat that as “done” and never prompt again.</li>
 * </ul>
 * <p>
 * Automatic prompt runs after the 4th cold start of {@link com.nuramin.calculator.MainActivity}.
 */
public final class ReviewController {

    private static final String PREFS = "review_controller_v1";
    private static final String KEY_LAUNCH_COUNT = "launch_count";
    private static final String KEY_NEVER_ASK = "never_ask";
    private static final String KEY_HIGH_RATING_DONE = "high_rating_done";
    private static final String KEY_LOW_ATTEMPTS = "low_rating_attempts";
    private static final String KEY_NEXT_ELIGIBLE_MS = "next_eligible_after_low_ms";
    private static final String KEY_OPENED_STORE_REVIEW = "opened_store_for_review";

    private static final int PROMPT_AFTER_LAUNCHES = 4;
    private static final int MAX_LOW_ATTEMPTS = 3;
    private static final long THREE_DAYS_MS = 3L * 24L * 60L * 60L * 1000L;
    private static final long DISMISS_SNOOZE_MS = 24L * 60L * 60L * 1000L;

    private ReviewController() {}

    /**
     * Call from {@link AppCompatActivity#onCreate(Bundle)} with the saved state bundle.
     */
    public static void onMainActivityCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) return;
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, AppCompatActivity.MODE_PRIVATE);
        int count = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1;
        prefs.edit().putInt(KEY_LAUNCH_COUNT, count).apply();

        if (!shouldShowAutomaticPrompt(prefs)) return;
        long scheduled = prefs.getLong(KEY_NEXT_ELIGIBLE_MS, 0L);
        boolean retryAfterLowRating = scheduled > 0;
        if (!retryAfterLowRating && count < PROMPT_AFTER_LAUNCHES) return;

        activity.getWindow().getDecorView().post(() -> showRatingDialog(activity, prefs));
    }

    /**
     * Overflow menu “Rate &amp; review”: always tries in-app review (Play may or may not show UI).
     */
    public static void openReviewFromMenu(AppCompatActivity activity) {
        launchPlayReviewFlow(activity, () -> { });
    }

    private static boolean shouldShowAutomaticPrompt(SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_NEVER_ASK, false)) return false;
        if (prefs.getBoolean(KEY_HIGH_RATING_DONE, false)) return false;
        if (prefs.getBoolean(KEY_OPENED_STORE_REVIEW, false)) return false;
        if (prefs.getInt(KEY_LOW_ATTEMPTS, 0) >= MAX_LOW_ATTEMPTS) return false;
        long next = prefs.getLong(KEY_NEXT_ELIGIBLE_MS, 0L);
        if (next > 0) {
            return System.currentTimeMillis() >= next;
        }
        return true;
    }

    private static void showRatingDialog(AppCompatActivity activity, SharedPreferences prefs) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_review_rating, null);
        RatingBar bar = content.findViewById(R.id.review_rating_bar);
        TextView linkStore = content.findViewById(R.id.review_link_play_store);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.review_dialog_title)
                .setView(content)
                .setPositiveButton(R.string.review_submit, null)
                .setNegativeButton(R.string.review_later, (d, w) -> d.dismiss())
                .create();

        final boolean[] flowCompleted = {false};

        if (linkStore != null) {
            linkStore.setOnClickListener(v -> {
                flowCompleted[0] = true;
                prefs.edit().putBoolean(KEY_OPENED_STORE_REVIEW, true).putBoolean(KEY_NEVER_ASK, true).apply();
                ShareController.openPlayStoreListing(activity);
                Toast.makeText(activity, R.string.review_thanks, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.setOnDismissListener(d -> {
            if (!flowCompleted[0]) {
                prefs.edit().putLong(KEY_NEXT_ELIGIBLE_MS, System.currentTimeMillis() + DISMISS_SNOOZE_MS).apply();
            }
        });

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                float stars = bar.getRating();
                if (stars <= 0f) {
                    Toast.makeText(activity, R.string.review_pick_stars, Toast.LENGTH_SHORT).show();
                    return;
                }
                int rating = Math.round(stars);
                flowCompleted[0] = true;
                dialog.dismiss();
                if (rating >= 4) {
                    prefs.edit().putBoolean(KEY_HIGH_RATING_DONE, true).putBoolean(KEY_NEVER_ASK, true).apply();
                    launchPlayReviewFlow(activity, () -> { });
                } else {
                    int attempts = prefs.getInt(KEY_LOW_ATTEMPTS, 0) + 1;
                    prefs.edit()
                            .putInt(KEY_LOW_ATTEMPTS, attempts)
                            .putLong(KEY_NEXT_ELIGIBLE_MS, System.currentTimeMillis() + THREE_DAYS_MS)
                            .apply();
                    Toast.makeText(activity, R.string.review_feedback_thanks, Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private static void launchPlayReviewFlow(AppCompatActivity activity, Runnable onDone) {
        try {
            ReviewManager manager = ReviewManagerFactory.create(activity);
            manager.requestReviewFlow().addOnCompleteListener(task -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (task.isSuccessful()) {
                    ReviewInfo info = task.getResult();
                    if (info != null) {
                        manager.launchReviewFlow(activity, info).addOnCompleteListener(done -> onDone.run());
                        return;
                    }
                }
                ShareController.openPlayStoreListing(activity);
                onDone.run();
            });
        } catch (Exception e) {
            ShareController.openPlayStoreListing(activity);
            onDone.run();
        }
    }
}
