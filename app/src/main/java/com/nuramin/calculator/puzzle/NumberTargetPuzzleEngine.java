package com.nuramin.calculator.puzzle;

import com.nuramin.calculator.util.ExprParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Game logic for Number Target Puzzle: generate solvable puzzles, validate expression,
 * level/difficulty, attempts, score. Modular: no Android dependencies.
 */
public final class NumberTargetPuzzleEngine {

    public static final int TARGET_SCORE = 100;
    public static final int MAX_ATTEMPTS_PER_PUZZLE = 3;
    public static final int NUMBERS_COUNT = 4;
    public static final int PUZZLES_PER_LEVEL = 3;

    private final Random random = new Random();

    private int level = 1;
    private int score = 0;
    private int puzzlesSolvedThisLevel = 0;
    private int attemptsLeft = MAX_ATTEMPTS_PER_PUZZLE;

    private int[] currentNumbers = new int[NUMBERS_COUNT];
    private int currentTarget;

    public int getLevel() {
        return level;
    }

    public int getScore() {
        return score;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }

    public int[] getCurrentNumbers() {
        return currentNumbers.clone();
    }

    public int getCurrentTarget() {
        return currentTarget;
    }

    /** Score reward for current level (10, 12, or 15). */
    public int getScoreReward() {
        if (level <= 1) return 10;
        if (level == 2) return 12;
        return 15;
    }

    /** Reset game state (score 0, level 1). */
    public void reset() {
        level = 1;
        score = 0;
        puzzlesSolvedThisLevel = 0;
        attemptsLeft = MAX_ATTEMPTS_PER_PUZZLE;
        generatePuzzle();
    }

    /**
     * Generate a new puzzle: 4 numbers and a target that can be produced from them.
     * Uses level to determine number range and target range.
     */
    public void generatePuzzle() {
        int numMin, numMax, targetMin, targetMax;
        if (level <= 1) {
            numMin = 1;
            numMax = 10;
            targetMin = 10;
            targetMax = 30;
        } else if (level == 2) {
            numMin = 1;
            numMax = 15;
            targetMin = 30;
            targetMax = 80;
        } else {
            numMin = 2;
            numMax = 20;
            targetMin = 80;
            targetMax = 150;
        }

        // Generate a solvable puzzle: pick 4 numbers, then compute target from an expression
        for (int tries = 0; tries < 50; tries++) {
            for (int i = 0; i < NUMBERS_COUNT; i++) {
                currentNumbers[i] = numMin + random.nextInt(numMax - numMin + 1);
            }
            // Build target from (a op b) op (c op d) or similar to guarantee solvability
            int a = currentNumbers[0], b = currentNumbers[1], c = currentNumbers[2], d = currentNumbers[3];
            int[] targets = {
                    a + b + c + d, a + b + c - d, a + b - c + d, a + b - c - d,
                    a + b * c, a * b + c, a * b + c * d, (a + b) * c,
                    a * b - c, a * b / (c != 0 ? c : 1), a * b + c - d
            };
            for (int t : targets) {
                if (t >= targetMin && t <= targetMax && t > 0) {
                    currentTarget = t;
                    attemptsLeft = MAX_ATTEMPTS_PER_PUZZLE;
                    return;
                }
            }
        }
        // Fallback: force target from expression so puzzle is solvable
        int a = currentNumbers[0], b = currentNumbers[1], c = currentNumbers[2], d = currentNumbers[3];
        int t = (a + b) * c - d;
        if (t < targetMin || t > targetMax) t = a * b + c + d;
        if (t < targetMin || t > targetMax) t = a + b + c * d;
        currentTarget = Math.max(targetMin, Math.min(targetMax, t));
        if (currentTarget <= 0) currentTarget = a + b + c + d;
        attemptsLeft = MAX_ATTEMPTS_PER_PUZZLE;
    }

    /**
     * Validate and evaluate expression. Returns SubmitResult.
     * Expression must use each of the 4 numbers exactly once; only + − × ÷ allowed.
     */
    public SubmitResult submitExpression(String expression) {
        if (attemptsLeft <= 0) {
            return SubmitResult.NO_ATTEMPTS_LEFT;
        }
        String normalized = expression.trim().replace('×', '*').replace('÷', '/').replace('−', '-');
        if (normalized.isEmpty()) {
            return SubmitResult.INVALID_EXPRESSION;
        }

        // Check that each number is used exactly once (extract numbers from expression)
        List<Integer> usedNumbers = extractNumbersFromExpression(expression);
        if (usedNumbers == null || usedNumbers.size() != NUMBERS_COUNT) {
            return SubmitResult.INVALID_NUMBERS;
        }
        boolean[] used = new boolean[NUMBERS_COUNT];
        for (int n : usedNumbers) {
            boolean found = false;
            for (int i = 0; i < NUMBERS_COUNT; i++) {
                if (!used[i] && currentNumbers[i] == n) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return SubmitResult.INVALID_NUMBERS;
        }

        double result;
        try {
            result = new ExprParser(normalized).parse();
        } catch (Exception e) {
            return SubmitResult.INVALID_EXPRESSION;
        }

        if (Double.isNaN(result) || Double.isInfinite(result) || result != Math.floor(result)) {
            return SubmitResult.INVALID_EXPRESSION;
        }
        int intResult = (int) Math.round(result);

        attemptsLeft--;
        if (intResult == currentTarget) {
            score += getScoreReward();
            puzzlesSolvedThisLevel++;
            if (puzzlesSolvedThisLevel >= PUZZLES_PER_LEVEL) {
                level++;
                puzzlesSolvedThisLevel = 0;
            }
            return SubmitResult.CORRECT;
        } else {
            if (attemptsLeft <= 0) {
                return SubmitResult.WRONG_ATTEMPTS_EXHAUSTED;
            }
            return SubmitResult.WRONG;
        }
    }

    /** Extract integers from expression string (numbers separated by operators/spaces). */
    private List<Integer> extractNumbersFromExpression(String expr) {
        List<Integer> list = new ArrayList<>();
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c)) {
                num.append(c);
            } else if (c == '+' || c == '−' || c == '×' || c == '÷' || c == ' ' || c == '-' && num.length() == 0) {
                if (num.length() > 0) {
                    try {
                        list.add(Integer.parseInt(num.toString()));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                    num.setLength(0);
                }
                if (c == '-' && (i == 0 || !Character.isDigit(expr.charAt(i - 1)))) {
                    num.append('-');
                }
            } else {
                if (num.length() > 0) {
                    try {
                        list.add(Integer.parseInt(num.toString()));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                    num.setLength(0);
                }
            }
        }
        if (num.length() > 0) {
            try {
                list.add(Integer.parseInt(num.toString()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return list;
    }

    public boolean isGameComplete() {
        return score >= TARGET_SCORE;
    }

    public enum SubmitResult {
        CORRECT,
        WRONG,
        WRONG_ATTEMPTS_EXHAUSTED,
        INVALID_EXPRESSION,
        INVALID_NUMBERS,
        NO_ATTEMPTS_LEFT
    }

    public static String getPerformanceRating(int score) {
        if (score < 40) return "Beginner";
        if (score < 70) return "Puzzle Player";
        if (score < 100) return "Puzzle Expert";
        return "Puzzle Champion 🏆";
    }
}
