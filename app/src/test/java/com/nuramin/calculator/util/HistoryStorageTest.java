package com.nuramin.calculator.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.nuramin.calculator.model.HistoryEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for HistoryStorage using an in-memory SharedPreferences (no Robolectric).
 */
@RunWith(JUnit4.class)
public class HistoryStorageTest {

    private HistoryStorage storage;
    private InMemorySharedPreferences prefs;

    @Before
    public void setUp() {
        prefs = new InMemorySharedPreferences();
        Context context = new MockContext(prefs);
        storage = new HistoryStorage(context);
        storage.clear();
    }

    @Test
    public void loadOldHistory_empty() {
        List<HistoryEntry> list = storage.loadOldHistory();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void appendEntry_and_loadOldHistory() {
        storage.appendEntry("1+2", "3");
        List<HistoryEntry> list = storage.loadOldHistory();
        assertEquals(1, list.size());
        assertEquals("1+2", list.get(0).getExpression());
        assertEquals("3", list.get(0).getResult());
        assertNotNull(list.get(0).getTimestamp());
    }

    @Test
    public void clear() {
        storage.appendEntry("1+2", "3");
        storage.clear();
        assertTrue(storage.loadOldHistory().isEmpty());
    }

    @Test
    public void saveAll() {
        List<HistoryEntry> entries = List.of(
            new HistoryEntry("a", "1"),
            new HistoryEntry("b", "2", 999L)
        );
        storage.saveAll(entries);
        List<HistoryEntry> loaded = storage.loadOldHistory();
        assertEquals(2, loaded.size());
        assertEquals("a", loaded.get(0).getExpression());
        assertEquals("1", loaded.get(0).getResult());
        assertEquals("b", loaded.get(1).getExpression());
        assertEquals(999L, (long) loaded.get(1).getTimestamp());
    }

    /** In-memory SharedPreferences for unit tests (no Android/Robolectric). */
    private static final class InMemorySharedPreferences implements SharedPreferences {
        private final Map<String, String> map = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(map);
        }

        @Override
        public String getString(String key, String defValue) {
            return map.getOrDefault(key, defValue);
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            return defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            return defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            return defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return defValue;
        }

        @Override
        public boolean contains(String key) {
            return map.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, String> pending = new HashMap<>(map);

                @Override
                public Editor putString(String key, String value) {
                    pending.put(key, value);
                    return this;
                }

                @Override
                public Editor putStringSet(String key, Set<String> values) {
                    return this;
                }

                @Override
                public Editor putInt(String key, int value) {
                    return this;
                }

                @Override
                public Editor putLong(String key, long value) {
                    return this;
                }

                @Override
                public Editor putFloat(String key, float value) {
                    return this;
                }

                @Override
                public Editor putBoolean(String key, boolean value) {
                    return this;
                }

                @Override
                public Editor remove(String key) {
                    pending.remove(key);
                    return this;
                }

                @Override
                public Editor clear() {
                    pending.clear();
                    return this;
                }

                @Override
                public boolean commit() {
                    map.clear();
                    map.putAll(pending);
                    return true;
                }

                @Override
                public void apply() {
                    map.clear();
                    map.putAll(pending);
                }
            };
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    }

    private static final class MockContext extends android.content.ContextWrapper {
        private final SharedPreferences prefs;

        MockContext(SharedPreferences prefs) {
            super(null);
            this.prefs = prefs;
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return prefs;
        }
    }
}
