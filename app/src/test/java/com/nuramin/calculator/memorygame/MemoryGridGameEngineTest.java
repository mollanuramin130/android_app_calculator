package com.nuramin.calculator.memorygame;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for MemoryGridGameEngine: level, score, tap validation.
 */
@RunWith(JUnit4.class)
public class MemoryGridGameEngineTest {

    private MemoryGridGameEngine engine;

    @Before
    public void setUp() {
        engine = new MemoryGridGameEngine();
    }

    @Test
    public void reset_initialState() {
        engine.reset();
        assertEquals(1, engine.getLevel());
        assertEquals(0, engine.getScore());
        assertFalse(engine.isGameOver());
        assertFalse(engine.isLevelComplete());
    }

    @Test
    public void startLevel_fillsCells() {
        engine.reset();
        engine.startLevel();
        assertEquals(3, engine.getNumbersCount()); // level 1 = 3 numbers
    }

    @Test
    public void getNumbersCount_validRange() {
        engine.reset();
        engine.startLevel();
        int count = engine.getNumbersCount();
        assertTrue(count >= 3 && count <= 10);
    }

    @Test
    public void getNumberAtCell() {
        engine.reset();
        engine.startLevel();
        assertEquals(-1, engine.getNumberAtCell(-1));
        assertEquals(-1, engine.getNumberAtCell(MemoryGridGameEngine.GRID_SIZE));
        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            int n = engine.getNumberAtCell(i);
            assertTrue(n == -1 || (n >= 1 && n <= engine.getNumbersCount()));
        }
    }

    @Test
    public void onCellTapped_correctThenLevelComplete() {
        engine.reset();
        engine.startLevel();
        int[] order = new int[engine.getNumbersCount()];
        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            int n = engine.getNumberAtCell(i);
            if (n >= 1 && n <= order.length) order[n - 1] = i;
        }
        for (int i = 0; i < order.length; i++) {
            MemoryGridGameEngine.TapResult r = engine.onCellTapped(order[i]);
            if (i < order.length - 1) {
                assertEquals(MemoryGridGameEngine.TapResult.CORRECT, r);
            } else {
                assertEquals(MemoryGridGameEngine.TapResult.LEVEL_COMPLETE, r);
                assertTrue(engine.isLevelComplete());
            }
        }
    }

    @Test
    public void onCellTapped_wrong_gameOver() {
        engine.reset();
        engine.startLevel();
        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            if (engine.getNumberAtCell(i) != 1) {
                assertEquals(MemoryGridGameEngine.TapResult.WRONG, engine.onCellTapped(i));
                assertTrue(engine.isGameOver());
                assertEquals(MemoryGridGameEngine.TapResult.IGNORED, engine.onCellTapped(i));
                return;
            }
        }
    }

    @Test
    public void advanceLevel_incrementsLevel() {
        engine.reset();
        engine.startLevel();
        int[] order = new int[engine.getNumbersCount()];
        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            int n = engine.getNumberAtCell(i);
            if (n >= 1 && n <= order.length) order[n - 1] = i;
        }
        for (int idx : order) engine.onCellTapped(idx);
        engine.advanceLevel();
        assertEquals(2, engine.getLevel());
        assertEquals(4, engine.getNumbersCount());
    }

    @Test
    public void getDisplayTimeMs_levels() {
        engine.reset();
        assertEquals(3000L, engine.getDisplayTimeMs());
        engine.startLevel();
        int[] order = new int[3];
        for (int i = 0; i < MemoryGridGameEngine.GRID_SIZE; i++) {
            int n = engine.getNumberAtCell(i);
            if (n >= 1 && n <= 3) order[n - 1] = i;
        }
        for (int idx : order) engine.onCellTapped(idx);
        engine.advanceLevel();
        assertEquals(3000L, engine.getDisplayTimeMs());
    }

    @Test
    public void getPerformanceLevel() {
        assertEquals("Memory Beginner", MemoryGridGameEngine.getPerformanceLevel(0));
        assertEquals("Memory Beginner", MemoryGridGameEngine.getPerformanceLevel(39));
        assertEquals("Memory Skilled", MemoryGridGameEngine.getPerformanceLevel(40));
        assertEquals("Memory Skilled", MemoryGridGameEngine.getPerformanceLevel(80));
        assertEquals("Memory Expert", MemoryGridGameEngine.getPerformanceLevel(81));
        assertEquals("Memory Expert", MemoryGridGameEngine.getPerformanceLevel(150));
        assertEquals("Memory Master 🧠", MemoryGridGameEngine.getPerformanceLevel(151));
    }
}
