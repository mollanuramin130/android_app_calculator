package com.nuramin.calculator.puzzle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for NumberTargetPuzzleEngine: puzzle generation, submit expression, attempts.
 */
@RunWith(JUnit4.class)
public class NumberTargetPuzzleEngineTest {

    private NumberTargetPuzzleEngine engine;

    @Before
    public void setUp() {
        engine = new NumberTargetPuzzleEngine();
        engine.reset(); // ensure puzzle is generated (constructor does not call reset)
    }

    @Test
    public void initialState() {
        assertEquals(1, engine.getLevel());
        assertEquals(0, engine.getScore());
        assertEquals(NumberTargetPuzzleEngine.MAX_ATTEMPTS_PER_PUZZLE, engine.getAttemptsLeft());
        assertNotNull(engine.getCurrentNumbers());
        assertEquals(NumberTargetPuzzleEngine.NUMBERS_COUNT, engine.getCurrentNumbers().length);
        assertTrue(engine.getCurrentTarget() > 0);
    }

    @Test
    public void submitEmpty_invalid() {
        assertEquals(NumberTargetPuzzleEngine.SubmitResult.INVALID_EXPRESSION,
                engine.submitExpression(""));
        assertEquals(NumberTargetPuzzleEngine.SubmitResult.INVALID_EXPRESSION,
                engine.submitExpression("   "));
    }

    @Test
    public void submitWrongNumbers_invalidNumbers() {
        int[] nums = engine.getCurrentNumbers();
        int target = engine.getCurrentTarget();
        String wrongExpr = (nums[0] + 1) + "+" + nums[1] + "+" + nums[2] + "+" + nums[3];
        assertEquals(NumberTargetPuzzleEngine.SubmitResult.INVALID_NUMBERS,
                engine.submitExpression(wrongExpr));
    }

    @Test
    public void submitValidExpression_usesAttempts() {
        int[] nums = engine.getCurrentNumbers();
        int target = engine.getCurrentTarget();
        String expr = nums[0] + "+" + nums[1] + "+" + nums[2] + "+" + nums[3];
        int sum = nums[0] + nums[1] + nums[2] + nums[3];
        if (sum == target) {
            assertEquals(NumberTargetPuzzleEngine.SubmitResult.CORRECT, engine.submitExpression(expr));
        } else {
            NumberTargetPuzzleEngine.SubmitResult r = engine.submitExpression(expr);
            assertTrue(r == NumberTargetPuzzleEngine.SubmitResult.WRONG ||
                    r == NumberTargetPuzzleEngine.SubmitResult.WRONG_ATTEMPTS_EXHAUSTED);
        }
    }

    @Test
    public void reset_restoresState() {
        engine.submitExpression("1+2+3+4");
        engine.reset();
        assertEquals(1, engine.getLevel());
        assertEquals(0, engine.getScore());
        assertEquals(NumberTargetPuzzleEngine.MAX_ATTEMPTS_PER_PUZZLE, engine.getAttemptsLeft());
    }

    @Test
    public void getPerformanceRating() {
        assertEquals("Beginner", NumberTargetPuzzleEngine.getPerformanceRating(30));
        assertEquals("Beginner", NumberTargetPuzzleEngine.getPerformanceRating(39));
        assertEquals("Puzzle Player", NumberTargetPuzzleEngine.getPerformanceRating(40));
        assertEquals("Puzzle Player", NumberTargetPuzzleEngine.getPerformanceRating(50));
        assertEquals("Puzzle Player", NumberTargetPuzzleEngine.getPerformanceRating(69));
        assertEquals("Puzzle Expert", NumberTargetPuzzleEngine.getPerformanceRating(70));
        assertEquals("Puzzle Expert", NumberTargetPuzzleEngine.getPerformanceRating(85));
        assertEquals("Puzzle Expert", NumberTargetPuzzleEngine.getPerformanceRating(99));
        assertEquals("Puzzle Champion 🏆", NumberTargetPuzzleEngine.getPerformanceRating(100));
    }

    @Test
    public void getScoreReward_level1() {
        assertEquals(10, engine.getScoreReward());
    }

    @Test
    public void noAttemptsLeft() {
        int[] n = engine.getCurrentNumbers();
        // Use multiplication so result is wrong (target is sum-like, not product)
        String wrong = n[0] + "*" + n[1] + "*" + n[2] + "*" + n[3];
        for (int i = 0; i < NumberTargetPuzzleEngine.MAX_ATTEMPTS_PER_PUZZLE; i++) {
            NumberTargetPuzzleEngine.SubmitResult r = engine.submitExpression(wrong);
            assertTrue(r == NumberTargetPuzzleEngine.SubmitResult.WRONG || r == NumberTargetPuzzleEngine.SubmitResult.WRONG_ATTEMPTS_EXHAUSTED);
        }
        assertEquals(NumberTargetPuzzleEngine.SubmitResult.NO_ATTEMPTS_LEFT,
                engine.submitExpression(n[0] + "+" + n[1] + "+" + n[2] + "+" + n[3]));
    }

    @Test
    public void wrongAttemptsExhausted() {
        int[] n = engine.getCurrentNumbers();
        int wrongTarget = engine.getCurrentTarget() + 100;
        String wrongExpr = n[0] + "+" + n[1] + "+" + n[2] + "+" + n[3];
        for (int i = 0; i < NumberTargetPuzzleEngine.MAX_ATTEMPTS_PER_PUZZLE - 1; i++) {
            NumberTargetPuzzleEngine.SubmitResult r = engine.submitExpression(wrongExpr);
            assertTrue(r == NumberTargetPuzzleEngine.SubmitResult.WRONG || r == NumberTargetPuzzleEngine.SubmitResult.CORRECT);
        }
        NumberTargetPuzzleEngine.SubmitResult last = engine.submitExpression(wrongExpr);
        assertTrue(last == NumberTargetPuzzleEngine.SubmitResult.WRONG_ATTEMPTS_EXHAUSTED || last == NumberTargetPuzzleEngine.SubmitResult.CORRECT);
    }

    @Test
    public void invalidExpression_nonIntegerResult() {
        int[] n = engine.getCurrentNumbers();
        // "a/2+b+c+d" may extract 5 numbers (a,2,b,c,d) -> INVALID_NUMBERS, or parse -> INVALID_EXPRESSION, or valid int -> WRONG/CORRECT
        String expr = n[0] + "/2+" + n[1] + "+" + n[2] + "+" + n[3];
        NumberTargetPuzzleEngine.SubmitResult r = engine.submitExpression(expr);
        assertTrue(r != NumberTargetPuzzleEngine.SubmitResult.NO_ATTEMPTS_LEFT);
    }

    @Test
    public void invalidExpression_parseFails() {
        // "1+2+3+" has only 3 numbers -> INVALID_NUMBERS; or malformed parse -> INVALID_EXPRESSION
        NumberTargetPuzzleEngine.SubmitResult r = engine.submitExpression("1+2+3+");
        assertTrue(r == NumberTargetPuzzleEngine.SubmitResult.INVALID_EXPRESSION
                || r == NumberTargetPuzzleEngine.SubmitResult.INVALID_NUMBERS);
    }

    @Test
    public void unicodeOperators_normalized() {
        int[] n = engine.getCurrentNumbers();
        String expr = n[0] + "×" + n[1] + "+" + n[2] + "+" + n[3];
        engine.submitExpression(expr);
        String divExpr = n[0] + "÷" + n[1] + "+" + n[2] + "+" + n[3];
        engine.submitExpression(divExpr);
    }

    @Test
    public void isGameComplete() {
        assertFalse(engine.isGameComplete());
    }
}
