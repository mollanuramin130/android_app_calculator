package com.nuramin.calculator.memorygame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Memory Number Grid. Level, score, difficulty, validation.
 * Modular: no Android dependencies; can be unit-tested or reused.
 */
public final class MemoryGridGameEngine {

    public static final int GRID_SIZE = 16; // 4x4
    public static final int CORRECT_TAP_SCORE = 5;
    public static final int LEVEL_COMPLETE_BONUS = 20;

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

    /** Number of numbers to show for current level (3..10). */
    public int getNumbersCount() {
        if (level <= 1) return 3;
        if (level == 2) return 4;
        if (level == 3) return 5;
        if (level == 4) return 6;
        if (level == 5) return 7;
        if (level == 6) return 8;
        if (level == 7) return 9;
        return 10; // 8+
    }

    /** Display time in milliseconds for current level. */
    public long getDisplayTimeMs() {
        if (level <= 1) return 3000;
        if (level == 2) return 3000;
        if (level == 3) return 2500;
        if (level == 4) return 2000;
        if (level == 5) return 1800;
        if (level == 6) return 1600;
        if (level == 7) return 1400;
        return 1200; // 8+
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
        } else {
            gameOver = true;
            return TapResult.WRONG;
        }
    }

    /** Call after level complete to advance to next level and start it. */
    public void advanceLevel() {
        level++;
        startLevel();
    }

    public enum TapResult {
        CORRECT,
        WRONG,
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
