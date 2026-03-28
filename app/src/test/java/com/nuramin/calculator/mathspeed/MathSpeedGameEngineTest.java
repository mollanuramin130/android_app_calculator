package com.nuramin.calculator.mathspeed;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for MathSpeedGameEngine: timer, lives, score, answer submission.
 */
@RunWith(JUnit4.class)
public class MathSpeedGameEngineTest {

    private MathSpeedGameEngine engine;

    @Before
    public void setUp() {
        engine = new MathSpeedGameEngine();
    }

    @Test
    public void initialState() {
        assertEquals(0, engine.getScore());
        assertEquals(MathSpeedGameEngine.MAX_LIVES, engine.getLives());
        assertEquals(MathSpeedGameEngine.INITIAL_TIMER_SEC, engine.getTimeRemainingSec());
        assertTrue(engine.isGameRunning());
        assertNotNull(engine.getCurrentEquationText());
        assertNotNull(engine.getOptionValues());
        assertEquals(4, engine.getOptionValues().size());
    }

    @Test
    public void correctAnswer_increasesScore() {
        int correct = engine.getCorrectResult();
        boolean result = engine.submitAnswer(correct);
        assertTrue(result);
        assertEquals(MathSpeedGameEngine.SCORE_CORRECT, engine.getScore());
        assertTrue(engine.isGameRunning());
    }

    @Test
    public void wrongAnswer_decreasesLives() {
        int correct = engine.getCorrectResult();
        int wrong = correct + 999;
        boolean result = engine.submitAnswer(wrong);
        assertFalse(result);
        assertEquals(MathSpeedGameEngine.MAX_LIVES - 1, engine.getLives());
    }

    @Test
    public void threeWrongAnswers_gameOver() {
        for (int i = 0; i < MathSpeedGameEngine.MAX_LIVES; i++) {
            List<Integer> options = engine.getOptionValues();
            int wrong = options.get(0).equals(engine.getCorrectResult()) ? options.get(1) : options.get(0);
            engine.submitAnswer(wrong);
        }
        assertFalse(engine.isGameRunning());
        assertTrue(engine.isGameOver());
    }

    @Test
    public void tickTimer_decreasesTime() {
        int before = engine.getTimeRemainingSec();
        engine.tickTimer();
        assertEquals(before - 1, engine.getTimeRemainingSec());
    }

    @Test
    public void addTimeBonus_increasesTime() {
        engine.tickTimer();
        int before = engine.getTimeRemainingSec();
        engine.addTimeBonus(5);
        assertEquals(before + 5, engine.getTimeRemainingSec());
    }

    @Test
    public void reset_restoresState() {
        engine.submitAnswer(engine.getCorrectResult());
        engine.tickTimer();
        engine.reset();
        assertEquals(0, engine.getScore());
        assertEquals(MathSpeedGameEngine.MAX_LIVES, engine.getLives());
        assertEquals(MathSpeedGameEngine.INITIAL_TIMER_SEC, engine.getTimeRemainingSec());
        assertTrue(engine.isGameRunning());
    }

    @Test
    public void submitAfterGameOver_returnsFalse() {
        for (int i = 0; i < MathSpeedGameEngine.MAX_LIVES; i++) {
            List<Integer> opts = engine.getOptionValues();
            int w = opts.get(0).equals(engine.getCorrectResult()) ? opts.get(1) : opts.get(0);
            engine.submitAnswer(w);
        }
        assertFalse(engine.submitAnswer(engine.getCorrectResult()));
    }

    @Test
    public void tickTimer_toZero_gameOver() {
        for (int i = 0; i < MathSpeedGameEngine.INITIAL_TIMER_SEC; i++) {
            boolean gameOver = engine.tickTimer();
            if (gameOver) {
                assertTrue(engine.isGameOver());
                assertFalse(engine.isGameRunning());
                assertFalse(engine.tickTimer());
                return;
            }
        }
        assertTrue(engine.isGameOver());
    }

    @Test
    public void addTimeBonus_whenNotRunning_noChange() {
        for (int i = 0; i < MathSpeedGameEngine.MAX_LIVES; i++) {
            List<Integer> opts = engine.getOptionValues();
            int w = opts.get(0).equals(engine.getCorrectResult()) ? opts.get(1) : opts.get(0);
            engine.submitAnswer(w);
        }
        int timeBefore = engine.getTimeRemainingSec();
        engine.addTimeBonus(10);
        assertEquals(timeBefore, engine.getTimeRemainingSec());
    }

    @Test
    public void addTimeBonus_zeroOrNegative_noChange() {
        int before = engine.getTimeRemainingSec();
        engine.addTimeBonus(0);
        engine.addTimeBonus(-5);
        assertEquals(before, engine.getTimeRemainingSec());
    }

    @Test
    public void generateEquation_producesValidEquation() {
        for (int i = 0; i < 20; i++) {
            engine.generateEquation();
            String text = engine.getCurrentEquationText();
            assertNotNull(text);
            assertTrue(text.contains("?"));
            // Subtraction can yield 0 (e.g. 5 − 5); that case has no ÷
            assertTrue(engine.getCorrectResult() != 0 || text.contains("÷") || text.contains("−"));
            List<Integer> opts = engine.getOptionValues();
            assertTrue(opts.contains(engine.getCorrectResult()));
        }
    }
}
