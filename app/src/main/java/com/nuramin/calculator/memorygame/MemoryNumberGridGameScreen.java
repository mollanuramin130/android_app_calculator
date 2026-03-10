package com.nuramin.calculator.memorygame;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.gridlayout.widget.GridLayout;

import com.google.android.material.card.MaterialCardView;
import com.nuramin.sunsetcoralcalculator.R;

/**
 * UI and flow for Memory Number Grid Game. Uses shared app TopBar; content only.
 * Builds grid in code, wires engine, display/hide phases, game over dialog.
 */
public final class MemoryNumberGridGameScreen {

    private static final String PREFS_HIGH_SCORE = "memory_grid_game_prefs";
    private static final String KEY_HIGH_SCORE = "MEMORY_GRID_HIGH_SCORE";

    private final View panel;
    private final Context context;
    private final MemoryGridGameEngine engine;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private GridLayout gridLayout;
    private TextView levelText;
    private TextView scoreText;
    private View startBtn;
    private MaterialCardView[] cellCards;
    private TextView[] cellLabels;
    private boolean displayPhase = true; // true = numbers visible, no taps
    private Runnable hideNumbersRunnable;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout,
                            @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;
        new MemoryNumberGridGameScreen(panel).bind();
    }

    private MemoryNumberGridGameScreen(View panel) {
        this.panel = panel;
        this.context = panel.getContext();
        this.engine = new MemoryGridGameEngine();
    }

    private void bind() {
        levelText = panel.findViewById(R.id.memory_grid_level_text);
        scoreText = panel.findViewById(R.id.memory_grid_score_text);
        startBtn = panel.findViewById(R.id.memory_grid_start_btn);
        gridLayout = panel.findViewById(R.id.memory_grid_layout);

        if (gridLayout == null || startBtn == null) return;

        buildGridCells();
        updateLevelScoreUi();

        startBtn.setOnClickListener(v -> startGame());
    }

    private void buildGridCells() {
        cellCards = new MaterialCardView[MemoryGridGameEngine.GRID_SIZE];
        cellLabels = new TextView[MemoryGridGameEngine.GRID_SIZE];
        LayoutInflater inflater = LayoutInflater.from(context);
        int marginPx = (int) (4 * context.getResources().getDisplayMetrics().density);
        int sizePx = (int) (70 * context.getResources().getDisplayMetrics().density);

        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            View cell = inflater.inflate(R.layout.memory_grid_cell, gridLayout, false);
            MaterialCardView card = cell.findViewById(R.id.memory_cell_card);
            TextView label = cell.findViewById(R.id.memory_cell_label);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 4), GridLayout.spec(i % 4));
            lp.width = sizePx;
            lp.height = sizePx;
            lp.setMargins(marginPx, marginPx, marginPx, marginPx);

            final int index = i;
            card.setOnClickListener(v -> onCellClicked(index));

            gridLayout.addView(cell, lp);
            cellCards[i] = card;
            cellLabels[i] = label;
        }
    }

    private void startGame() {
        if (hideNumbersRunnable != null) {
            handler.removeCallbacks(hideNumbersRunnable);
            hideNumbersRunnable = null;
        }
        engine.reset();
        engine.startLevel();
        updateLevelScoreUi();
        startBtn.setVisibility(View.GONE);
        startDisplayPhase();
    }

    private void startDisplayPhase() {
        displayPhase = true;
        clearGridDisplay();
        int count = engine.getNumbersCount();
        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            int num = engine.getNumberAtCell(i);
            if (num >= 1 && num <= count) {
                cellLabels[i].setText(String.valueOf(num));
                cellLabels[i].setVisibility(View.VISIBLE);
                setCellBackground(cellCards[i], 0xFF455A64); // default
            } else {
                cellLabels[i].setVisibility(View.GONE);
            }
        }

        long displayTimeMs = engine.getDisplayTimeMs();
        hideNumbersRunnable = this::startInputPhase;
        handler.postDelayed(hideNumbersRunnable, displayTimeMs);
    }

    private void startInputPhase() {
        hideNumbersRunnable = null;
        displayPhase = false;
        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            cellLabels[i].setVisibility(View.GONE);
            setCellBackground(cellCards[i], 0xFF455A64);
        }
    }

    private void onCellClicked(int cellIndex) {
        if (displayPhase) return;
        if (engine.isGameOver()) return;

        MemoryGridGameEngine.TapResult result = engine.onCellTapped(cellIndex);

        switch (result) {
            case CORRECT:
                setCellBackground(cellCards[cellIndex], 0xFF4CAF50); // green
                playCorrectTapAnimation(cellCards[cellIndex]);
                scoreText.setText(context.getString(R.string.memory_grid_score_format, engine.getScore()));
                break;
            case WRONG:
                setCellBackground(cellCards[cellIndex], 0xFFE53935); // red
                playWrongTapAnimation(cellCards[cellIndex]);
                handler.postDelayed(this::showGameOverDialog, 300);
                break;
            case LEVEL_COMPLETE:
                setCellBackground(cellCards[cellIndex], 0xFF4CAF50);
                playCorrectTapAnimation(cellCards[cellIndex]);
                scoreText.setText(context.getString(R.string.memory_grid_score_format, engine.getScore()));
                handler.postDelayed(this::onLevelComplete, 400);
                break;
            default:
                break;
        }
    }

    private void onLevelComplete() {
        engine.advanceLevel();
        updateLevelScoreUi();
        startDisplayPhase();
    }

    private void showGameOverDialog() {
        int score = engine.getScore();
        saveHighScoreIfNeeded(score);
        int highScore = getHighScore();

        String perf = getPerformanceString(score);
        String message = context.getString(R.string.memory_grid_your_score, score)
                + "\n\n" + perf
                + "\n\n" + context.getString(R.string.memory_grid_high_score_label, highScore);

        new AlertDialog.Builder(panel.getContext())
                .setTitle(R.string.memory_grid_game_over_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.memory_grid_play_again, (dialog, which) -> {
                    dialog.dismiss();
                    resetUiAndShowStart();
                })
                .show();
    }

    private String getPerformanceString(int score) {
        if (score < 40) return context.getString(R.string.memory_grid_perf_beginner);
        if (score <= 80) return context.getString(R.string.memory_grid_perf_skilled);
        if (score <= 150) return context.getString(R.string.memory_grid_perf_expert);
        return context.getString(R.string.memory_grid_perf_master);
    }

    private void resetUiAndShowStart() {
        if (hideNumbersRunnable != null) {
            handler.removeCallbacks(hideNumbersRunnable);
            hideNumbersRunnable = null;
        }
        displayPhase = true;
        clearGridDisplay();
        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            setCellBackground(cellCards[i], 0xFF455A64);
        }
        startBtn.setVisibility(View.VISIBLE);
        engine.reset();
        updateLevelScoreUi();
    }

    private void clearGridDisplay() {
        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            if (cellLabels[i] != null) {
                cellLabels[i].setText("");
                cellLabels[i].setVisibility(View.GONE);
            }
        }
    }

    private void updateLevelScoreUi() {
        if (levelText != null) levelText.setText(context.getString(R.string.memory_grid_level_format, engine.getLevel()));
        if (scoreText != null) scoreText.setText(context.getString(R.string.memory_grid_score_format, engine.getScore()));
    }

    private void setCellBackground(MaterialCardView card, int colorArgb) {
        if (card != null) card.setCardBackgroundColor(colorArgb);
    }

    private void playCorrectTapAnimation(View cell) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cell, View.SCALE_X, 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cell, View.SCALE_Y, 1f, 1.15f, 1f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    private void playWrongTapAnimation(View cell) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(cell, View.TRANSLATION_X, 0f, -15f, 15f, -10f, 10f, 0f);
        shake.setDuration(300);
        shake.start();
    }

    private int getHighScore() {
        return context.getSharedPreferences(PREFS_HIGH_SCORE, Context.MODE_PRIVATE)
                .getInt(KEY_HIGH_SCORE, 0);
    }

    private void saveHighScoreIfNeeded(int score) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_HIGH_SCORE, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_HIGH_SCORE, 0);
        if (score > current) {
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply();
        }
    }
}
