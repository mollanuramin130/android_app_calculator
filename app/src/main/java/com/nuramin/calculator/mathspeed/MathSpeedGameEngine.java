package com.nuramin.calculator.mathspeed;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Game logic for Math Speed Game (falling answers).
 * Generates equations, correct/wrong answers, tracks score, lives, timer.
 * No Android dependencies; can be tested and updated independently.
 */
public final class MathSpeedGameEngine {

    public static final int INITIAL_TIMER_SEC = 60;
    public static final int MAX_LIVES = 3;
    public static final int SCORE_CORRECT = 10;
    public static final int OPERAND_MIN = 1;
    public static final int OPERAND_MAX = 20;

    private static final String[] OPERATORS = {" + ", " − ", " × ", " ÷ "};

    private final Random random = new Random();

    private int score;
    private int lives;
    private int timeRemainingSec;
    private int correctResult;
    private String equationText;
    private List<Integer> optionValues; // 1 correct + 3 wrong, for spawning
    private boolean gameRunning;

    public MathSpeedGameEngine() {
        reset();
    }

    public int getScore() {
        return score;
    }

    public int getLives() {
        return lives;
    }

    public int getTimeRemainingSec() {
        return timeRemainingSec;
    }

    public String getCurrentEquationText() {
        return equationText;
    }

    public int getCorrectResult() {
        return correctResult;
    }

    /** Returns 4 values: 1 correct + 3 wrong. Used when spawning falling buttons. */
    public List<Integer> getOptionValues() {
        return optionValues == null ? new ArrayList<>() : new ArrayList<>(optionValues);
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public boolean isGameOver() {
        return !gameRunning;
    }

    /** Reset and start a new game. */
    public void reset() {
        score = 0;
        lives = MAX_LIVES;
        timeRemainingSec = INITIAL_TIMER_SEC;
        gameRunning = true;
        generateEquation();
    }

    /** Call every second. Returns true when timer hit zero (game over). */
    public boolean tickTimer() {
        if (!gameRunning) return false;
        timeRemainingSec--;
        if (timeRemainingSec < 0) timeRemainingSec = 0;
        if (timeRemainingSec <= 0) {
            gameRunning = false;
            return true;
        }
        return false;
    }

    /** Add bonus seconds for correct answer so user can reach higher score. */
    public void addTimeBonus(int seconds) {
        if (seconds > 0 && gameRunning) {
            timeRemainingSec += seconds;
        }
    }

    /**
     * User tapped an answer. Returns true if correct.
     * On correct: score += 10, generate next equation.
     * On wrong: lives--; game over when lives <= 0.
     */
    public boolean submitAnswer(int value) {
        if (!gameRunning) return false;
        if (value == correctResult) {
            score += SCORE_CORRECT;
            generateEquation();
            return true;
        }
        lives--;
        if (lives <= 0) {
            gameRunning = false;
        }
        return false;
    }

    /** Generate new equation: operands 1..20, + − × ÷ (division integer only). */
    public void generateEquation() {
        int a = OPERAND_MIN + random.nextInt(OPERAND_MAX - OPERAND_MIN + 1);
        int b = OPERAND_MIN + random.nextInt(OPERAND_MAX - OPERAND_MIN + 1);
        int opIndex = random.nextInt(4);
        int result;
        String op = OPERATORS[opIndex];
        switch (opIndex) {
            case 0:
                result = a + b;
                break;
            case 1:
                result = a - b;
                if (result < 0) {
                    int t = a;
                    a = b;
                    b = t;
                    result = a - b;
                }
                break;
            case 2:
                result = a * b;
                break;
            default: // division: integer result only
                if (b == 0) b = 1;
                result = a / b;
                if (result == 0) result = 1;
                a = b * result;
                break;
        }
        equationText = a + op + b + " = ?";
        correctResult = result;
        optionValues = buildWrongAnswers(result);
    }

    private List<Integer> buildWrongAnswers(int correct) {
        Set<Integer> used = new HashSet<>();
        used.add(correct);
        List<Integer> options = new ArrayList<>();
        options.add(correct);
        int[][] ranges = {{-5, 5}, {-7, 7}, {-9, 9}};
        for (int r = 0; r < 3; r++) {
            int lo = ranges[r][0], hi = ranges[r][1];
            for (int attempt = 0; attempt < 50; attempt++) {
                int offset = lo + random.nextInt(hi - lo + 1);
                if (offset == 0) continue;
                int wrong = correct + offset;
                if (!used.contains(wrong)) {
                    used.add(wrong);
                    options.add(wrong);
                    break;
                }
            }
        }
        while (options.size() < 4) {
            int wrong = correct + (random.nextInt(19) - 9);
            if (wrong != correct && !used.contains(wrong)) {
                used.add(wrong);
                options.add(wrong);
            }
        }
        return options;
    }
}
