package com.nuramin.calculator.memorygame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Memory Number Grid. Level, score, difficulty, validation.
 * <p>Levels 1–{@value #TUTORIAL_MAX_LEVEL} are tutorial: always 3 numbers, fixed memorize time; any wrong tap ends the game.
 * From level {@value #TUTORIAL_MAX_LEVEL} + 1 onward, more numbers appear and extra memorize time is added per level.
 */
public final class MemoryGridGameEngine {

    public static final int GRID_SIZE = 16; // 4x4
    public static final int CORRECT_TAP_SCORE = 5;
    public static final int LEVEL_COMPLETE_BONUS = 20;

    /** Levels 1..this (inclusive): same as “level 1” grid size (3 numbers) and no memorize-time ramp yet. */
    public static final int TUTORIAL_MAX_LEVEL = 3;

    /** After tutorial: wrong taps consume one chance; at zero chances the game ends. */
    public static final int CHANCES_AFTER_TUTORIAL = 3;

    /** Starting memorization window (ms) during tutorial levels — numbers stay visible this long before hiding. */
    public static final long BASE_DISPLAY_MS = 2800L;
    /** From level {@value #TUTORIAL_MAX_LEVEL} + 1 onward: each level adds this many ms (stacks on top of {@link #BASE_DISPLAY_MS}). */
    public static final long DISPLAY_MS_BONUS_PER_LEVEL = 220L;
    private static final long MIN_DISPLAY_MS = 1400L;
    private static final long MAX_DISPLAY_MS = 12000L;

    private final Random random = new Random();

    private int level = 1;
    private int score = 0;
    /** For current level: cell index -> number (1..count), or -1 if empty. */
    private int[] cellNumbers = new int[GRID_SIZE];
    /** Order of cells the user must tap (1, 2, 3, ...). */
    private int[] tapOrder;
    /** Next expected tap index in tapOrder (0-based). */
    private int nextTapIndex;
    private boolean gameOver;
    private boolean levelComplete;
    /** After tutorial: wrong taps left this level (reset in {@link #startLevel()}). */
    private int remainingChances;
    /** Correct cell to briefly reveal after a wrong tap (post-tutorial); -1 if none. */
    private int wrongRevealCellIndex = -1;

    public int getLevel() {
        return level;
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isLevelComplete() {
        return levelComplete;
    }

    /** Remaining wrong-tap allowances this level (0 in tutorial). */
    public int getRemainingChances() {
        if (level <= TUTORIAL_MAX_LEVEL) return 0;
        return remainingChances;
    }

    /**
     * After a post-tutorial wrong tap, the cell index that should have been tapped next (for UI peek).
     * Meaningful only for {@link TapResult#WRONG_LOST_CHANCE} and {@link TapResult#WRONG_GAME_OVER}.
     */
    public int getWrongRevealCellIndex() {
        return wrongRevealCellIndex;
    }

    /** Whether this cell was already tapped correctly earlier in the current level. */
    public boolean isCellAlreadyCompleted(int cellIndex) {
        if (tapOrder == null || nextTapIndex <= 0) return false;
        for (int t = 0; t < nextTapIndex; t++) {
            if (tapOrder[t] == cellIndex) return true;
        }
        return false;
    }

    /**
     * How many numbers appear on the grid (3..10). Tutorial levels 1–{@value #TUTORIAL_MAX_LEVEL} always use 3;
     * after that, count matches level (capped at 10).
     */
    public int getNumbersCount() {
        if (level <= TUTORIAL_MAX_LEVEL) return 3;
        return Math.min(10, level);
    }

    /**
     * How long numbers stay visible (memorization phase). Tutorial: fixed {@link #BASE_DISPLAY_MS}.
     * After tutorial: {@code BASE_DISPLAY_MS + (level - TUTORIAL_MAX_LEVEL) * DISPLAY_MS_BONUS_PER_LEVEL}, clamped.
     */
    public long getDisplayTimeMs() {
        long t = BASE_DISPLAY_MS;
        if (level > TUTORIAL_MAX_LEVEL) {
            t += (long) (level - TUTORIAL_MAX_LEVEL) * DISPLAY_MS_BONUS_PER_LEVEL;
        }
        if (t < MIN_DISPLAY_MS) return MIN_DISPLAY_MS;
        if (t > MAX_DISPLAY_MS) return MAX_DISPLAY_MS;
        return t;
    }

    /** Reset state and prepare for a new game (level 1, score 0). */
    public void reset() {
        level = 1;
        score = 0;
        gameOver = false;
        levelComplete = false;
        cellNumbers = new int[GRID_SIZE];
        tapOrder = null;
        nextTapIndex = 0;
    }

    /**
     * Start the current level: fill random cells with 1..N, build tap order.
     * Call after reset() or after completing previous level.
     */
    public void startLevel() {
        gameOver = false;
        levelComplete = false;
        int count = getNumbersCount();
        for (int i = 0; i < GRID_SIZE; i++) {
            cellNumbers[i] = -1;
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < GRID_SIZE; i++) indices.add(i);
        Collections.shuffle(indices, random);
        tapOrder = new int[count];
        for (int n = 0; n < count; n++) {
            int cellIndex = indices.get(n);
            cellNumbers[cellIndex] = n + 1; // 1, 2, 3, ...
            tapOrder[n] = cellIndex;
        }
        nextTapIndex = 0;
        if (level > TUTORIAL_MAX_LEVEL) {
            remainingChances = CHANCES_AFTER_TUTORIAL;
        } else {
            remainingChances = 0;
        }
        wrongRevealCellIndex = -1;
    }

    /** Get number at cell (1-based) or -1 if empty. */
    public int getNumberAtCell(int cellIndex) {
        if (cellIndex < 0 || cellIndex >= GRID_SIZE) return -1;
        return cellNumbers[cellIndex];
    }

    /**
     * Validate user tap. Returns TapResult (CORRECT, WRONG, LEVEL_COMPLETE after last correct).
     */
    public TapResult onCellTapped(int cellIndex) {
        if (gameOver || nextTapIndex >= tapOrder.length) {
            return TapResult.IGNORED;
        }
        wrongRevealCellIndex = -1;
        int expectedCell = tapOrder[nextTapIndex];
        if (cellIndex == expectedCell) {
            score += CORRECT_TAP_SCORE;
            nextTapIndex++;
            if (nextTapIndex >= tapOrder.length) {
                levelComplete = true;
                score += LEVEL_COMPLETE_BONUS;
                return TapResult.LEVEL_COMPLETE;
            }
            return TapResult.CORRECT;
        }
        // Wrong cell
        if (level <= TUTORIAL_MAX_LEVEL) {
            gameOver = true;
            return TapResult.WRONG;
        }
        remainingChances--;
        wrongRevealCellIndex = expectedCell;
        if (remainingChances <= 0) {
            gameOver = true;
            return TapResult.WRONG_GAME_OVER;
        }
        return TapResult.WRONG_LOST_CHANCE;
    }

    /** Call after level complete to advance to next level and start it. */
    public void advanceLevel() {
        level++;
        startLevel();
    }

    public enum TapResult {
        CORRECT,
        /** Tutorial only: wrong tap ends the game immediately. */
        WRONG,
        /** Post-tutorial: wrong tap but chances remain; UI may peek {@link #getWrongRevealCellIndex()}. */
        WRONG_LOST_CHANCE,
        /** Post-tutorial: wrong tap with no chances left; UI may peek then game over. */
        WRONG_GAME_OVER,
        LEVEL_COMPLETE,
        IGNORED
    }

    /** Performance label from score (before game over). */
    public static String getPerformanceLevel(int score) {
        if (score < 40) return "Memory Beginner";
        if (score <= 80) return "Memory Skilled";
        if (score <= 150) return "Memory Expert";
        return "Memory Master 🧠";
    }
}
