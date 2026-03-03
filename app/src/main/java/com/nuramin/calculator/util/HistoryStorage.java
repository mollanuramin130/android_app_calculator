package com.nuramin.calculator.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.nuramin.calculator.model.HistoryEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists "Old History" so it survives app restart. Clear on "Clear history".
 */
public final class HistoryStorage {

    private static final String PREFS_NAME = "calculator_history";
    private static final String KEY_OLD_HISTORY = "old_history";
    private static final int MAX_OLD_ITEMS = 200;

    private final SharedPreferences prefs;

    public HistoryStorage(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<HistoryEntry> loadOldHistory() {
        String json = prefs.getString(KEY_OLD_HISTORY, "[]");
        List<HistoryEntry> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String expr = o.optString("expr", "");
                String result = o.optString("result", "");
                long time = o.optLong("time", 0L);
                list.add(new HistoryEntry(expr, result, time));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void appendEntry(String expression, String result) {
        List<HistoryEntry> list = loadOldHistory();
        list.add(0, new HistoryEntry(expression, result, System.currentTimeMillis()));
        while (list.size() > MAX_OLD_ITEMS) {
            list.remove(list.size() - 1);
        }
        save(list);
    }

    public void clear() {
        prefs.edit().remove(KEY_OLD_HISTORY).apply();
    }

    /** Replace persisted old history with the given list (e.g. after removing one entry). */
    public void saveAll(List<HistoryEntry> list) {
        save(list);
    }

    private void save(List<HistoryEntry> list) {
        JSONArray arr = new JSONArray();
        for (HistoryEntry e : list) {
            JSONObject o = new JSONObject();
            try {
                o.put("expr", e.getExpression());
                o.put("result", e.getResult());
                o.put("time", e.getTimestamp() != null ? e.getTimestamp() : 0L);
                arr.put(o);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_OLD_HISTORY, arr.toString()).apply();
    }
}
