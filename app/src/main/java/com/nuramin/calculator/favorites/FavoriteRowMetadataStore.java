package com.nuramin.calculator.favorites;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.Map;

/** Persists per-row UI state: pin, priority, auto-delete expiry. */
public final class FavoriteRowMetadataStore {
    private static final String PREFS = "favorite_row_ui_prefs";
    private static final String KEY_META = "meta_json";

    private FavoriteRowMetadataStore() {}

    public static synchronized FavoriteRowMeta get(Context context, String id) {
        if (context == null || TextUtils.isEmpty(id)) return new FavoriteRowMeta();
        JSONObject root = loadRoot(context);
        JSONObject o = root.optJSONObject(id);
        FavoriteRowMeta m = new FavoriteRowMeta();
        if (o != null) {
            m.pinned = o.optBoolean("pinned", false);
            m.pinnedAtMs = o.optLong("pinned_at", 0L);
            m.priorityLevel = o.optInt("priority", FavoriteRowMeta.PRIORITY_MEDIUM);
            m.customWeight = o.optInt("custom_weight", 50);
            m.autoDeleteEnabled = o.optBoolean("auto_delete", false);
            m.autoDeleteExpireAtMs = o.optLong("auto_expire_at", 0L);
        }
        // Legacy: PIN_PREFS boolean only
        if (o == null) {
            android.content.SharedPreferences pinLegacy =
                    context.getSharedPreferences("favorite_pin_prefs", Context.MODE_PRIVATE);
            if (pinLegacy.getBoolean(id, false)) {
                m.pinned = true;
                m.pinnedAtMs = System.currentTimeMillis();
            }
        }
        return m;
    }

    public static synchronized void save(Context context, String id, FavoriteRowMeta meta) {
        if (context == null || TextUtils.isEmpty(id) || meta == null) return;
        try {
            JSONObject root = loadRoot(context);
            JSONObject o = new JSONObject();
            o.put("pinned", meta.pinned);
            o.put("pinned_at", meta.pinnedAtMs);
            o.put("priority", meta.priorityLevel);
            o.put("custom_weight", meta.customWeight);
            o.put("auto_delete", meta.autoDeleteEnabled);
            o.put("auto_expire_at", meta.autoDeleteExpireAtMs);
            root.put(id, o);
            persist(context, root);
        } catch (Exception ignored) {
        }
    }

    public static synchronized void remove(Context context, String id) {
        if (context == null || TextUtils.isEmpty(id)) return;
        try {
            JSONObject root = loadRoot(context);
            root.remove(id);
            persist(context, root);
        } catch (Exception ignored) {
        }
        SharedPreferences legacy = context.getSharedPreferences("favorite_pin_prefs", Context.MODE_PRIVATE);
        legacy.edit().remove(id).apply();
    }

    private static JSONObject loadRoot(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_META, "{}");
        try {
            return new JSONObject(raw);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static void persist(Context context, JSONObject root) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_META, root.toString()).apply();
    }

    /** One-time migration from legacy pin-only prefs into full meta. */
    public static synchronized void migrateLegacyPins(Context context) {
        SharedPreferences legacy = context.getSharedPreferences("favorite_pin_prefs", Context.MODE_PRIVATE);
        Map<String, ?> all = legacy.getAll();
        if (all == null || all.isEmpty()) return;
        JSONObject root = loadRoot(context);
        boolean changed = false;
        for (Map.Entry<String, ?> e : all.entrySet()) {
            String id = e.getKey();
            if (TextUtils.isEmpty(id)) continue;
            if (root.has(id)) continue;
            Object v = e.getValue();
            if (!(v instanceof Boolean) || !((Boolean) v)) continue;
            FavoriteRowMeta m = new FavoriteRowMeta();
            m.pinned = true;
            m.pinnedAtMs = System.currentTimeMillis();
            try {
                JSONObject o = new JSONObject();
                o.put("pinned", m.pinned);
                o.put("pinned_at", m.pinnedAtMs);
                o.put("priority", m.priorityLevel);
                o.put("custom_weight", m.customWeight);
                o.put("auto_delete", m.autoDeleteEnabled);
                o.put("auto_expire_at", m.autoDeleteExpireAtMs);
                root.put(id, o);
                changed = true;
            } catch (Exception ignored) {
            }
        }
        if (changed) persist(context, root);
    }
}
