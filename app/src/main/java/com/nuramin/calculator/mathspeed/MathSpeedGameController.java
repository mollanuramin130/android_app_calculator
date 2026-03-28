package com.nuramin.calculator.mathspeed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nuramin.sunsetcoralcalculator.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * UI controller for Math Speed Game (falling answers).
 * Reuses app TopBar; handles spawn loop, falling animation, taps, timer, lives, game over.
 */
public final class MathSpeedGameController {

    private static final String PREFS_NAME = "math_speed_prefs";
    private static final String KEY_HIGH_SCORE = "math_speed_high_score";
    private static final int MAX_FALLING_BUTTONS = 5;
    private static final int SPAWN_INTERVAL_MS = 800;
    private static final int EQUATION_TIMEOUT_MS = 5000;
    private static final int FALL_DURATION_MIN_MS = 3000;
    private static final int FALL_DURATION_MAX_MS = 5000;
    private static final int BUTTON_WIDTH_DP = 100;
    private static final int BUTTON_HEIGHT_DP = 48;
    private static final int FALL_START_OFFSET_DP = 60;

    private final View panel;
    private final Context context;
    private final MathSpeedGameEngine engine;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final float density;

    private TextView scoreLabel;
    private TextView timerLabel;
    private View live1;
    private View live2;
    private View live3;
    private FrameLayout playArea;
    private TextView equationText;
    private View startBtn;
    private View resetBtn;

    private CountDownTimer gameTimer;
    private Runnable spawnRunnable;
    private Runnable equationTimeoutRunnable;
    private final List<View> activeFallingViews = new ArrayList<>();
    private final List<ObjectAnimator> activeAnimators = new ArrayList<>();

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout,
                             @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;
        MathSpeedGameController c = new MathSpeedGameController(panel);
        c.bind();
        panel.setTag(c);
    }

    /** Call when user navigates away from Math Speed Game so the game stops and no dialog is shown. */
    public static void pauseWhenPanelHidden(View mathSpeedPanel) {
        if (mathSpeedPanel == null) return;
        Object tag = mathSpeedPanel.getTag();
        if (tag instanceof MathSpeedGameController) {
            ((MathSpeedGameController) tag).pauseGameWithoutDialog();
        }
    }

    /** True if panel and context are still valid (activity not finishing). */
    private boolean isContextValid() {
        if (context == null) return false;
        if (context instanceof Activity) {
            Activity a = (Activity) context;
            if (a.isFinishing() || a.isDestroyed()) return false;
        }
        return true;
    }

    private boolean isPanelAttached() {
        return panel != null && panel.getParent() != null;
    }

    private MathSpeedGameController(View panel) {
        this.panel = panel;
        this.context = panel.getContext();
        this.engine = new MathSpeedGameEngine();
        this.density = context.getResources().getDisplayMetrics().density;
    }

    private void bind() {
        scoreLabel = panel.findViewById(R.id.math_speed_score_label);
        timerLabel = panel.findViewById(R.id.math_speed_timer_label);
        live1 = panel.findViewById(R.id.math_speed_live_1);
        live2 = panel.findViewById(R.id.math_speed_live_2);
        live3 = panel.findViewById(R.id.math_speed_live_3);
        playArea = panel.findViewById(R.id.math_speed_play_area);
        equationText = panel.findViewById(R.id.math_speed_equation_text);
        startBtn = panel.findViewById(R.id.math_speed_start_btn);
        resetBtn = panel.findViewById(R.id.math_speed_reset_btn);

        if (playArea == null || equationText == null || startBtn == null) return;

        updateStatusUi();
        equationText.setText(engine.getCurrentEquationText());

        startBtn.setOnClickListener(v -> startGame());
        if (resetBtn != null) resetBtn.setOnClickListener(v -> resetGame());
    }

    private void startGame() {
        stopGame();
        engine.reset();
        updateStatusUi();
        equationText.setText(engine.getCurrentEquationText());
        startBtn.setVisibility(View.GONE);

        startGameTimer();
        startSpawnLoop();
        startEquationTimeout();
    }

    private void stopGame() {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        if (spawnRunnable != null) {
            handler.removeCallbacks(spawnRunnable);
            spawnRunnable = null;
        }
        if (equationTimeoutRunnable != null) {
            handler.removeCallbacks(equationTimeoutRunnable);
            equationTimeoutRunnable = null;
        }
        clearFallingButtons();
    }

    /** Stops the game without showing the Game Over dialog. Used when user leaves the screen. */
    private void pauseGameWithoutDialog() {
        stopGame();
        if (startBtn != null) startBtn.setVisibility(View.VISIBLE);
    }

    private void resetGame() {
        stopGame();
        engine.reset();
        updateStatusUi();
        if (equationText != null) equationText.setText(engine.getCurrentEquationText());
        if (startBtn != null) startBtn.setVisibility(View.VISIBLE);
    }

    private void startGameTimer() {
        refreshGameTimer();
    }

    /** Restart countdown timer to match engine's current time (e.g. after time bonus). */
    private void refreshGameTimer() {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        long remainingMs = engine.getTimeRemainingSec() * 1000L;
        if (remainingMs <= 0) {
            updateStatusUi();
            onGameOver();
            return;
        }
        gameTimer = new CountDownTimer(remainingMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isContextValid()) return;
                boolean ended = engine.tickTimer();
                updateStatusUi();
                if (ended) {
                    cancel();
                    gameTimer = null;
                    onGameOver();
                }
            }

            @Override
            public void onFinish() {
                if (!isContextValid()) return;
                if (engine.isGameRunning()) engine.tickTimer();
                gameTimer = null;
                updateStatusUi();
                onGameOver();
            }
        };
        gameTimer.start();
    }

    private void startSpawnLoop() {
        spawnRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isContextValid() || !isPanelAttached()) return;
                if (!engine.isGameRunning()) return;
                if (activeFallingViews.size() < MAX_FALLING_BUTTONS) {
                    spawnFallingButton();
                }
                handler.postDelayed(this, SPAWN_INTERVAL_MS);
            }
        };
        handler.postDelayed(spawnRunnable, SPAWN_INTERVAL_MS);
    }

    private void startEquationTimeout() {
        if (equationTimeoutRunnable != null) {
            handler.removeCallbacks(equationTimeoutRunnable);
        }
        equationTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isContextValid() || !engine.isGameRunning()) return;
                engine.generateEquation();
                if (equationText != null) {
                    equationText.setText(engine.getCurrentEquationText());
                }
                startEquationTimeout();
            }
        };
        handler.postDelayed(equationTimeoutRunnable, EQUATION_TIMEOUT_MS);
    }

    private void spawnFallingButton() {
        if (!isContextValid() || playArea == null || playArea.getParent() == null || !engine.isGameRunning()) return;
        List<Integer> options = engine.getOptionValues();
        if (options.isEmpty()) return;

        int value = options.get(random.nextInt(options.size()));
        MaterialButton button = createFallingButton(value);

        int wPx = (int) (BUTTON_WIDTH_DP * density);
        int hPx = (int) (BUTTON_HEIGHT_DP * density);
        int startOffsetPx = (int) (FALL_START_OFFSET_DP * density);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(wPx, hPx);
        int maxLeft = Math.max(0, playArea.getWidth() - wPx);
        lp.leftMargin = maxLeft > 0 ? random.nextInt(maxLeft + 1) : 0;
        lp.topMargin = -startOffsetPx;
        lp.gravity = Gravity.TOP | Gravity.START;
        playArea.addView(button, lp);
        button.bringToFront();

        int playHeight = playArea.getHeight();
        if (playHeight <= 0) playHeight = (int) (400 * density);
        float endTranslationY = playHeight + startOffsetPx;

        ObjectAnimator anim = ObjectAnimator.ofFloat(button, View.TRANSLATION_Y, 0f, endTranslationY);
        int duration = FALL_DURATION_MIN_MS + random.nextInt(FALL_DURATION_MAX_MS - FALL_DURATION_MIN_MS + 1);
        anim.setDuration(duration);
        anim.setInterpolator(new LinearInterpolator());
        activeFallingViews.add(button);
        activeAnimators.add(anim);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isContextValid()) return;
                int idx = activeAnimators.indexOf(anim);
                if (idx < 0 || idx >= activeFallingViews.size()) return;
                View v = activeFallingViews.get(idx);
                activeAnimators.remove(idx);
                activeFallingViews.remove(idx);
                removeButtonFromParent(v);
            }
        });
        anim.start();
    }

    private MaterialButton createFallingButton(int value) {
        MaterialButton button = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        int wPx = (int) (BUTTON_WIDTH_DP * density);
        int hPx = (int) (BUTTON_HEIGHT_DP * density);
        button.setLayoutParams(new FrameLayout.LayoutParams(wPx, hPx));
        button.setText(String.valueOf(value));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setCornerRadius((int) (24 * density));
        try {
            button.setBackgroundColor(context.getResources().getColor(R.color.math_speed_falling_btn_bg, context.getTheme()));
            button.setTextColor(context.getResources().getColor(R.color.math_speed_falling_btn_text, context.getTheme()));
        } catch (Exception ignored) { }
        button.setTag(value);
        button.setClickable(true);
        button.setFocusable(false);
        button.setFocusableInTouchMode(false);
        button.setElevation(16f);

        button.setOnClickListener(v -> {
            Object tag = v.getTag();
            if (tag == null || !engine.isGameRunning()) return;
            int val;
            try {
                val = tag instanceof Integer ? (Integer) tag : Integer.parseInt(String.valueOf(tag));
            } catch (NumberFormatException e) {
                return;
            }
            boolean correct = engine.submitAnswer(val);

            int idx = activeFallingViews.indexOf(v);
            if (idx >= 0 && idx < activeAnimators.size()) {
                ObjectAnimator toCancel = activeAnimators.get(idx);
                if (toCancel != null) toCancel.cancel();
                activeAnimators.remove(idx);
                activeFallingViews.remove(idx);
            }

            if (!isContextValid()) return;
            if (correct) {
                try {
                    v.setBackgroundColor(context.getResources().getColor(R.color.math_speed_correct, context.getTheme()));
                    ((MaterialButton) v).setTextColor(context.getResources().getColor(android.R.color.white, context.getTheme()));
                } catch (Exception ignored) { }
                engine.addTimeBonus(5);
                refreshGameTimer();
                if (equationTimeoutRunnable != null) handler.removeCallbacks(equationTimeoutRunnable);
                if (equationText != null) equationText.setText(engine.getCurrentEquationText());
                startEquationTimeout();
            } else {
                try {
                    v.setBackgroundColor(context.getResources().getColor(R.color.math_speed_wrong, context.getTheme()));
                    ((MaterialButton) v).setTextColor(context.getResources().getColor(android.R.color.white, context.getTheme()));
                } catch (Exception ignored) { }
                if (engine.isGameOver()) {
                    stopGame();
                    onGameOver();
                    removeButtonFromParent(v);
                    updateStatusUi();
                    return;
                }
            }
            updateStatusUi();
            removeButtonFromParent(v);
        });

        return button;
    }

    private void removeButtonFromParent(View v) {
        if (v == null) return;
        try {
            if (v.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) v.getParent()).removeView(v);
            }
        } catch (Exception ignored) { }
    }

    private void clearFallingButtons() {
        for (int i = activeFallingViews.size() - 1; i >= 0; i--) {
            View v = i < activeFallingViews.size() ? activeFallingViews.get(i) : null;
            ObjectAnimator a = i < activeAnimators.size() ? activeAnimators.get(i) : null;
            if (a != null) a.cancel();
            removeButtonFromParent(v);
        }
        activeFallingViews.clear();
        activeAnimators.clear();
    }

    private void updateStatusUi() {
        if (!isContextValid()) return;
        try {
            if (scoreLabel != null) {
                scoreLabel.setText(context.getString(R.string.math_game_score, engine.getScore()));
            }
            if (timerLabel != null) {
                timerLabel.setText(context.getString(R.string.math_game_time, engine.getTimeRemainingSec()));
            }
            int lives = engine.getLives();
            if (live1 != null) live1.setVisibility(lives >= 1 ? View.VISIBLE : View.INVISIBLE);
            if (live2 != null) live2.setVisibility(lives >= 2 ? View.VISIBLE : View.INVISIBLE);
            if (live3 != null) live3.setVisibility(lives >= 3 ? View.VISIBLE : View.INVISIBLE);
        } catch (Exception ignored) { }
    }

    private void onGameOver() {
        if (startBtn != null) startBtn.setVisibility(View.VISIBLE);
        if (!isContextValid()) return;
        if (panel == null || panel.getVisibility() != View.VISIBLE || !panel.isShown()) {
            return;
        }
        int score = engine.getScore();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int previousBest = prefs.getInt(KEY_HIGH_SCORE, 0);
            if (score > previousBest) {
                prefs.edit().putInt(KEY_HIGH_SCORE, score).apply();
                previousBest = score;
            }
            int bestScore = previousBest;
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.math_game_over_title)
                    .setMessage(context.getString(R.string.math_game_over_message, score, bestScore))
                    .setPositiveButton(R.string.math_game_play_again, (d, w) -> startGame())
                    .setNegativeButton(R.string.math_game_exit, (d, w) -> resetGame())
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            // Avoid crash if activity is finishing (e.g. BadTokenException)
        }
    }
}
