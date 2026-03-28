package com.nuramin.calculator.puzzle;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.gridlayout.widget.GridLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.nuramin.sunsetcoralcalculator.R;

/**
 * UI for Number Target Puzzle. Reuses shared app TopBar. Builds expression from number/operator
 * taps, submit/clear, animations, game complete and high score.
 */
public final class NumberTargetPuzzleGameScreen {

    private static final String PREFS_PUZZLE = "number_target_puzzle_prefs";
    private static final String KEY_HIGH_SCORE = "TARGET_PUZZLE_HIGH_SCORE";

    private final View panel;
    private final Context context;
    private final NumberTargetPuzzleEngine engine;

    private TextView targetValue;
    private TextView expressionText;
    private TextView scoreText;
    private MaterialButton[] numButtons;
    private View[] opButtons;
    private MaterialButton btnClear;
    private MaterialButton btnSubmit;
    private View expressionCard;
    private StringBuilder expression = new StringBuilder();
    private boolean[] numberUsed = new boolean[NumberTargetPuzzleEngine.NUMBERS_COUNT];
    private boolean transitionInProgress;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout,
                            @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;
        new NumberTargetPuzzleGameScreen(panel).bind();
    }

    private NumberTargetPuzzleGameScreen(View panel) {
        this.panel = panel;
        this.context = panel.getContext();
        this.engine = new NumberTargetPuzzleEngine();
    }

    private void bind() {
        targetValue = panel.findViewById(R.id.puzzle_target_value);
        expressionText = panel.findViewById(R.id.puzzle_expression_text);
        scoreText = panel.findViewById(R.id.puzzle_score_text);
        expressionCard = panel.findViewById(R.id.puzzle_expression_card);
        btnClear = panel.findViewById(R.id.puzzle_btn_clear);
        btnSubmit = panel.findViewById(R.id.puzzle_btn_submit);

        int[] numIds = { R.id.puzzle_num_0, R.id.puzzle_num_1, R.id.puzzle_num_2, R.id.puzzle_num_3 };
        numButtons = new MaterialButton[4];
        for (int i = 0; i < 4; i++) {
            numButtons[i] = panel.findViewById(numIds[i]);
            final int index = i;
            if (numButtons[i] != null) {
                numButtons[i].setOnClickListener(v -> onNumberTap(index));
                numButtons[i].setOnLongClickListener(v -> true);
            }
        }

        int[] opIds = { R.id.puzzle_op_plus, R.id.puzzle_op_minus, R.id.puzzle_op_multiply, R.id.puzzle_op_divide };
        String[] opLabels = { " + ", " − ", " × ", " ÷ " };
        opButtons = new View[4];
        for (int i = 0; i < 4; i++) {
            opButtons[i] = panel.findViewById(opIds[i]);
            if (opButtons[i] != null) {
                final String op = opLabels[i];
                opButtons[i].setOnClickListener(v -> appendOperator(op));
            }
        }

        if (btnClear != null) btnClear.setOnClickListener(v -> clearExpression());
        if (btnSubmit != null) btnSubmit.setOnClickListener(v -> onSubmit());

        engine.reset();
        refreshPuzzleUi();
        refreshScoreAndExpression();
    }

    private void onNumberTap(int index) {
        if (transitionInProgress || numberUsed[index]) return;
        int n = engine.getCurrentNumbers()[index];
        expression.append(n);
        numberUsed[index] = true;
        if (numButtons[index] != null) numButtons[index].setEnabled(false);
        refreshExpressionDisplay();
    }

    private void appendOperator(String op) {
        if (transitionInProgress) return;
        if (expression.length() == 0) return;
        char last = expression.charAt(expression.length() - 1);
        if (last == '+' || last == '−' || last == '×' || last == '÷' || last == ' ') return;
        expression.append(op);
        refreshExpressionDisplay();
    }

    private void clearExpression() {
        if (transitionInProgress) return;
        expression.setLength(0);
        for (int i = 0; i < numberUsed.length; i++) {
            numberUsed[i] = false;
            if (numButtons[i] != null) numButtons[i].setEnabled(true);
        }
        refreshExpressionDisplay();
    }

    private void refreshExpressionDisplay() {
        if (expressionText != null) {
            String s = expression.toString().trim();
            expressionText.setText(s.isEmpty() ? "" : s);
        }
    }

    private void refreshPuzzleUi() {
        applyPuzzleNumbersAndTarget();
        resetExpressionState();
    }

    private void applyPuzzleNumbersAndTarget() {
        int[] nums = engine.getCurrentNumbers();
        for (int i = 0; i < 4 && i < numButtons.length; i++) {
            if (numButtons[i] != null) {
                numButtons[i].setText(String.valueOf(nums[i]));
                numButtons[i].setEnabled(true);
            }
        }
        if (targetValue != null) {
            targetValue.setText(context.getString(R.string.puzzle_target, engine.getCurrentTarget()));
        }
    }

    private void resetExpressionState() {
        expression.setLength(0);
        for (int i = 0; i < numberUsed.length; i++) {
            numberUsed[i] = false;
            if (numButtons[i] != null) numButtons[i].setEnabled(true);
        }
        refreshExpressionDisplay();
    }

    private void refreshScoreAndExpression() {
        if (scoreText != null) {
            scoreText.setText(context.getString(R.string.puzzle_score, engine.getScore()));
        }
        refreshExpressionDisplay();
    }

    private void setButtonsEnabled(boolean enabled) {
        transitionInProgress = !enabled;
        for (MaterialButton b : numButtons) {
            if (b != null) b.setEnabled(enabled);
        }
        if (btnClear != null) btnClear.setEnabled(enabled);
        if (btnSubmit != null) btnSubmit.setEnabled(enabled);
        if (opButtons != null) {
            for (View v : opButtons) {
                if (v != null) v.setEnabled(enabled);
            }
        }
    }

    private void onSubmit() {
        if (transitionInProgress) return;
        String expr = expression.toString().trim();
        if (expr.isEmpty()) {
            Toast.makeText(context, R.string.puzzle_incorrect_solution, Toast.LENGTH_SHORT).show();
            return;
        }

        NumberTargetPuzzleEngine.SubmitResult result = engine.submitExpression(expr);

        switch (result) {
            case CORRECT:
                playCorrectAnimation();
                if (scoreText != null) {
                    scoreText.setText(context.getString(R.string.puzzle_score, engine.getScore()));
                }
                if (engine.isGameComplete()) {
                    showGameCompleteDialog();
                } else {
                    setButtonsEnabled(false);
                    panel.postDelayed(() -> {
                        engine.generatePuzzle();
                        refreshPuzzleUi();
                        setButtonsEnabled(true);
                    }, 600);
                }
                break;
            case WRONG:
                Toast.makeText(context, R.string.puzzle_incorrect_solution, Toast.LENGTH_SHORT).show();
                playWrongAnimation();
                clearExpression();
                break;
            case WRONG_ATTEMPTS_EXHAUSTED:
            case NO_ATTEMPTS_LEFT:
                Toast.makeText(context, R.string.puzzle_incorrect_solution, Toast.LENGTH_SHORT).show();
                playWrongAnimation();
                setButtonsEnabled(false);
                panel.postDelayed(() -> {
                    engine.generatePuzzle();
                    refreshPuzzleUi();
                    setButtonsEnabled(true);
                }, 800);
                break;
            case INVALID_EXPRESSION:
            case INVALID_NUMBERS:
                Toast.makeText(context, R.string.puzzle_incorrect_solution, Toast.LENGTH_SHORT).show();
                playWrongAnimation();
                clearExpression();
                break;
        }
    }

    private void playCorrectAnimation() {
        if (expressionCard != null) {
            expressionCard.setBackgroundColor(0xFF4CAF50);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(expressionCard, View.SCALE_X, 1f, 1.03f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(expressionCard, View.SCALE_Y, 1f, 1.03f, 1f);
            scaleX.setDuration(150);
            scaleY.setDuration(150);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(scaleX, scaleY);
            set.setInterpolator(new AccelerateDecelerateInterpolator());
            set.start();
            panel.postDelayed(() -> {
                if (expressionCard instanceof MaterialCardView) {
                    ((MaterialCardView) expressionCard).setCardBackgroundColor(0xFF455A64);
                } else if (expressionCard != null) {
                    expressionCard.setBackgroundColor(0xFF455A64);
                }
            }, 400);
        }
    }

    private void playWrongAnimation() {
        if (expressionCard != null) {
            ObjectAnimator shake = ObjectAnimator.ofFloat(expressionCard, View.TRANSLATION_X, 0f, -12f, 12f, -8f, 8f, 0f);
            shake.setDuration(350);
            shake.start();
        }
    }

    private void showGameCompleteDialog() {
        int score = engine.getScore();
        saveHighScoreIfNeeded(score);
        int highScore = getHighScore();
        String perf = getPerformanceString(score);

        String message = context.getString(R.string.puzzle_final_score, score)
                + "\n\n" + perf
                + "\n\n" + context.getString(R.string.puzzle_high_score_label, highScore);

        setButtonsEnabled(false);
        new AlertDialog.Builder(panel.getContext())
                .setTitle(R.string.puzzle_complete_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.puzzle_play_again, (dialog, which) -> {
                    dialog.dismiss();
                    engine.reset();
                    refreshPuzzleUi();
                    refreshScoreAndExpression();
                    setButtonsEnabled(true);
                })
                .setOnDismissListener(dialog -> setButtonsEnabled(true))
                .show();
    }

    private String getPerformanceString(int score) {
        if (score < 40) return context.getString(R.string.puzzle_perf_beginner);
        if (score < 70) return context.getString(R.string.puzzle_perf_player);
        if (score < 100) return context.getString(R.string.puzzle_perf_expert);
        return context.getString(R.string.puzzle_perf_champion);
    }

    private int getHighScore() {
        return context.getSharedPreferences(PREFS_PUZZLE, Context.MODE_PRIVATE)
                .getInt(KEY_HIGH_SCORE, 0);
    }

    private void saveHighScoreIfNeeded(int score) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PUZZLE, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_HIGH_SCORE, 0);
        if (score > current) {
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply();
        }
    }
}
