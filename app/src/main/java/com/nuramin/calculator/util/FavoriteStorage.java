package com.nuramin.calculator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.nuramin.calculator.favorites.FavoriteRowMeta;
import com.nuramin.calculator.favorites.FavoriteRowMetadataStore;
import com.nuramin.calculator.util.favorites.FavoritesEngine;
import com.nuramin.sunsetcoralcalculator.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Simple persistent storage for user favourites/starred reminders. */
public final class FavoriteStorage {
    private static final String PIN_PREFS = "favorite_pin_prefs";

    public static final class FavoriteItem {
        public final String id;
        public final String title;
        public final String note;
        public final String screen;
        public final long timestamp;
        public final boolean pinned;
        public final int priorityLevel;
        public final int customWeight;
        public final boolean autoDeleteEnabled;
        public final long autoDeleteExpireAtMs;
        public final long pinnedAtMs;

        public FavoriteItem(String id, String title, String note, String screen, long timestamp, boolean pinned,
                            int priorityLevel, int customWeight, boolean autoDeleteEnabled,
                            long autoDeleteExpireAtMs, long pinnedAtMs) {
            this.id = id;
            this.title = title;
            this.note = note;
            this.screen = screen;
            this.timestamp = timestamp;
            this.pinned = pinned;
            this.priorityLevel = priorityLevel;
            this.customWeight = customWeight;
            this.autoDeleteEnabled = autoDeleteEnabled;
            this.autoDeleteExpireAtMs = autoDeleteExpireAtMs;
            this.pinnedAtMs = pinnedAtMs;
        }

        /** Higher sorts first within same pin group. */
        public int sortPriorityValue() {
            switch (priorityLevel) {
                case FavoriteRowMeta.PRIORITY_LOW:
                    return 1;
                case FavoriteRowMeta.PRIORITY_MEDIUM:
                    return 2;
                case FavoriteRowMeta.PRIORITY_HIGH:
                    return 3;
                case FavoriteRowMeta.PRIORITY_CUSTOM:
                    return Math.max(1, Math.min(100, customWeight));
                default:
                    return 0;
            }
        }
    }

    private FavoriteStorage() {}

    public static synchronized void add(Context context, String title, String note, String screen) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("title", safe(title, "Favourite"));
        payload.put("note", safe(note, ""));
        payload.put("screen", safe(screen, ""));
        String type = inferTypeFromScreen(screen);
        FavoritesEngine.Request request = new FavoritesEngine.Request(payload, type, FavoritesEngine.DuplicatePolicy.MERGE);
        FavoritesEngine.add(context, request);
    }

    public static synchronized void remove(Context context, String id) {
        if (id == null || id.trim().isEmpty()) return;
        FavoritesEngine.unfavorite(context, id);
        clearPinned(context, id);
    }

    public static synchronized void deletePermanent(Context context, String id) {
        if (id == null || id.trim().isEmpty()) return;
        FavoritesEngine.deletePermanent(context, id);
        clearPinned(context, id);
    }

    public static synchronized void processExpiredAutoDeletes(Context context) {
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, 2000, false);
        if (!response.success || response.data == null) return;
        long now = System.currentTimeMillis();
        for (FavoritesEngine.FavoriteRecord r : response.data) {
            FavoriteRowMeta m = FavoriteRowMetadataStore.get(context, r.id);
            if (!m.autoDeleteEnabled || m.autoDeleteExpireAtMs <= 0) continue;
            if (m.pinned) continue;
            if (m.shouldBlockAutoDelete()) continue;
            if (now >= m.autoDeleteExpireAtMs) {
                FavoritesEngine.deletePermanent(context, r.id);
                FavoriteRowMetadataStore.remove(context, r.id);
            }
        }
    }

    public static synchronized List<FavoriteItem> getAll(Context context) {
        FavoriteRowMetadataStore.migrateLegacyPins(context);
        processExpiredAutoDeletes(context);
        List<FavoriteItem> out = new ArrayList<>();
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, 1000, false);
        if (!response.success || response.data == null) return Collections.unmodifiableList(out);
        for (FavoritesEngine.FavoriteRecord r : response.data) {
            out.add(buildFavoriteItem(context, r));
        }
        Collections.sort(out, FavoriteStorage::compareFavoriteRows);
        if (out.size() > 200) out = new ArrayList<>(out.subList(0, 200));
        return Collections.unmodifiableList(out);
    }

    private static FavoriteItem buildFavoriteItem(Context context, FavoritesEngine.FavoriteRecord r) {
        String title = r.normalizedData != null ? r.normalizedData.optString("title", "") : "";
        String note = r.normalizedData != null ? r.normalizedData.optString("note", "") : "";
        String screen = r.normalizedData != null ? r.normalizedData.optString("screen", "") : "";
        if (TextUtils.isEmpty(title) && r.rawInput != null) title = r.rawInput.optString("title", "");
        if (TextUtils.isEmpty(note) && r.rawInput != null) note = r.rawInput.optString("note", "");
        if (TextUtils.isEmpty(screen) && r.rawInput != null) screen = r.rawInput.optString("screen", "");
        if (TextUtils.isEmpty(title)) title = "Favourite";
        FavoriteRowMeta m = FavoriteRowMetadataStore.get(context, r.id);
        return new FavoriteItem(r.id, title, note, screen, r.createdAt, m.pinned,
                m.priorityLevel, m.customWeight, m.autoDeleteEnabled, m.autoDeleteExpireAtMs, m.pinnedAtMs);
    }

    private static int compareFavoriteRows(FavoriteItem a, FavoriteItem b) {
        if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
        int pw = Integer.compare(b.sortPriorityValue(), a.sortPriorityValue());
        if (pw != 0) return pw;
        if (a.pinned && b.pinned) {
            int cmp = Long.compare(b.pinnedAtMs, a.pinnedAtMs);
            if (cmp != 0) return cmp;
        }
        return Long.compare(b.timestamp, a.timestamp);
    }

    public static synchronized List<FavoriteItem> search(Context context, String query, String type) {
        FavoriteRowMetadataStore.migrateLegacyPins(context);
        List<FavoriteItem> out = new ArrayList<>();
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, type, query, 100, false);
        if (!response.success || response.data == null) return Collections.unmodifiableList(out);
        for (FavoritesEngine.FavoriteRecord r : response.data) {
            out.add(buildFavoriteItem(context, r));
        }
        Collections.sort(out, FavoriteStorage::compareFavoriteRows);
        return Collections.unmodifiableList(out);
    }

    public static synchronized void markUsed(Context context, String id) {
        if (id == null || id.trim().isEmpty()) return;
        FavoritesEngine.touchUsage(context, id);
    }

    private static String inferTypeFromScreen(String screen) {
        String s = screen == null ? "" : screen.toLowerCase();
        if (s.contains("emi")) return "emi";
        if (s.contains("tax")) return "tax";
        if (s.contains("date")) return "date";
        if (s.contains("unit")) return "unit_converter";
        if (s.contains("interest")) return "interest";
        return "calculator";
    }

    private static String safe(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        return v.isEmpty() ? fallback : v;
    }

    public static FavoritesEngine.Response<FavoritesEngine.FavoriteRecord> addAdvanced(
            Context context,
            Map<String, String> rawInput,
            String explicitType,
            FavoritesEngine.DuplicatePolicy duplicatePolicy
    ) {
        FavoritesEngine.Request request = new FavoritesEngine.Request(rawInput, explicitType, duplicatePolicy);
        return FavoritesEngine.add(context, request);
    }

    /** Same inference and required-field rules as {@link #addAdvanced}, without persisting. */
    public static FavoritesEngine.Response<Void> validateForAdd(Map<String, String> payload, String explicitType) {
        FavoritesEngine.Request request = new FavoritesEngine.Request(payload, explicitType, FavoritesEngine.DuplicatePolicy.MERGE);
        return FavoritesEngine.validateForAdd(request);
    }

    /** User-facing message for a failed {@link #validateForAdd} result. */
    public static String formatValidationMessage(Context context, FavoritesEngine.Response<Void> response) {
        if (context == null) return "";
        if (response != null && response.success) return "";
        if (response == null || response.error == null) {
            return context.getString(R.string.favorites_validation_generic);
        }
        if ("MISSING_REQUIRED".equals(response.error.code) && response.error.details != null) {
            Object raw = response.error.details.get("missing_fields");
            if (raw instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> missing = (List<String>) raw;
                if (missing != null && !missing.isEmpty()) {
                    return context.getString(R.string.favorites_validation_missing_list, joinMissingFieldLabels(context, missing));
                }
            }
        }
        String msg = response.error.message;
        return msg == null ? context.getString(R.string.favorites_validation_generic) : msg;
    }

    private static String joinMissingFieldLabels(Context context, List<String> keys) {
        StringBuilder sb = new StringBuilder();
        String sep = context.getString(R.string.favorites_validation_list_separator);
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(humanizeMissingFieldKey(context, keys.get(i)));
        }
        return sb.toString();
    }

    private static String humanizeMissingFieldKey(Context context, String key) {
        if (TextUtils.isEmpty(key)) return "";
        switch (key) {
            case "title":
                return context.getString(R.string.favorites_field_title);
            case "principal":
                return context.getString(R.string.favorites_field_principal);
            case "rate":
                return context.getString(R.string.favorites_field_rate);
            case "tenure":
                return context.getString(R.string.favorites_field_tenure);
            case "time":
                return context.getString(R.string.favorites_field_time);
            case "amount":
                return context.getString(R.string.favorites_field_amount);
            case "date_1":
                return context.getString(R.string.favorites_field_date_1);
            case "date_2":
                return context.getString(R.string.favorites_field_date_2);
            case "value":
                return context.getString(R.string.favorites_field_value);
            case "from_unit":
                return context.getString(R.string.favorites_field_from_unit);
            case "to_unit":
                return context.getString(R.string.favorites_field_to_unit);
            case "note":
                return context.getString(R.string.favorites_field_note);
            default:
                return key.replace('_', ' ');
        }
    }

    public static FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> staleSuggestions(Context context) {
        return FavoritesEngine.staleSuggestions(context);
    }

    public static void setDebugMode(boolean enabled) {
        FavoritesEngine.setDebugMode(enabled);
    }

    public static synchronized void removePermanently(Context context, String id) {
        deletePermanent(context, id);
    }

    public static synchronized void restoreByPayload(Context context, Map<String, String> payload, String explicitType) {
        FavoritesEngine.Request request = new FavoritesEngine.Request(payload, explicitType, FavoritesEngine.DuplicatePolicy.MERGE);
        FavoritesEngine.add(context, request);
    }

    public static synchronized void clearAllForTesting(Context context) {
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, 10000, true);
        if (!response.success || response.data == null) return;
        for (FavoritesEngine.FavoriteRecord record : response.data) {
            FavoritesEngine.deletePermanent(context, record.id);
        }
    }

    public static synchronized List<String> debugListFingerprints(Context context) {
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, 1000, true);
        List<String> out = new ArrayList<>();
        if (!response.success || response.data == null) return out;
        for (FavoritesEngine.FavoriteRecord record : response.data) {
            if (!TextUtils.isEmpty(record.fingerprint)) out.add(record.fingerprint);
        }
        return out;
    }

    public static synchronized boolean hasExactDuplicate(Context context, String fingerprint) {
        if (TextUtils.isEmpty(fingerprint)) return false;
        List<String> fps = debugListFingerprints(context);
        for (String fp : fps) {
            if (fingerprint.equals(fp)) return true;
        }
        return false;
    }

    public static synchronized List<FavoriteItem> recent(Context context, int limit) {
        FavoriteRowMetadataStore.migrateLegacyPins(context);
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, limit, false);
        List<FavoriteItem> out = new ArrayList<>();
        if (!response.success || response.data == null) return out;
        for (FavoritesEngine.FavoriteRecord r : response.data) {
            out.add(buildFavoriteItem(context, r));
        }
        Collections.sort(out, FavoriteStorage::compareFavoriteRows);
        return out;
    }

    public static synchronized boolean isPinned(Context context, String id) {
        if (context == null || TextUtils.isEmpty(id)) return false;
        return FavoriteRowMetadataStore.get(context, id).pinned;
    }

    public static synchronized void setPinned(Context context, String id, boolean pinned) {
        if (context == null || TextUtils.isEmpty(id)) return;
        FavoriteRowMeta m = FavoriteRowMetadataStore.get(context, id);
        m.pinned = pinned;
        if (pinned) m.pinnedAtMs = System.currentTimeMillis();
        FavoriteRowMetadataStore.save(context, id, m);
        SharedPreferences prefs = context.getSharedPreferences(PIN_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(id, pinned).apply();
    }

    public static synchronized boolean togglePinned(Context context, String id) {
        FavoriteRowMeta m = FavoriteRowMetadataStore.get(context, id);
        m.pinned = !m.pinned;
        if (m.pinned) m.pinnedAtMs = System.currentTimeMillis();
        FavoriteRowMetadataStore.save(context, id, m);
        SharedPreferences prefs = context.getSharedPreferences(PIN_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(id, m.pinned).apply();
        return m.pinned;
    }

    public static synchronized void setPriority(Context context, String id, int priorityLevel, int customWeight) {
        if (context == null || TextUtils.isEmpty(id)) return;
        FavoriteRowMeta m = FavoriteRowMetadataStore.get(context, id);
        m.priorityLevel = priorityLevel;
        m.customWeight = customWeight;
        if (m.shouldBlockAutoDelete() && m.autoDeleteEnabled) {
            m.autoDeleteEnabled = false;
            m.autoDeleteExpireAtMs = 0L;
        }
        FavoriteRowMetadataStore.save(context, id, m);
    }

    public static synchronized void setAutoDelete(Context context, String id, boolean enabled, long expireAtMs) {
        if (context == null || TextUtils.isEmpty(id)) return;
        FavoriteRowMeta m = FavoriteRowMetadataStore.get(context, id);
        if (enabled && m.shouldBlockAutoDelete()) {
            return;
        }
        m.autoDeleteEnabled = enabled;
        m.autoDeleteExpireAtMs = enabled ? expireAtMs : 0L;
        FavoriteRowMetadataStore.save(context, id, m);
    }

    public static synchronized boolean updateTitle(Context context, String id, String newTitle) {
        return FavoritesEngine.updateTitle(context, id, newTitle).success;
    }

    private static void clearPinned(Context context, String id) {
        if (context == null || TextUtils.isEmpty(id)) return;
        FavoriteRowMetadataStore.remove(context, id);
    }

    public static synchronized boolean isHealthy(Context context) {
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, 1, true);
        return response.success;
    }

    public static synchronized FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> raw(Context context) {
        return FavoritesEngine.list(context, null, null, 1000, true);
    }

    public static synchronized void remove(Context context, FavoriteItem item) {
        if (item == null) return;
        remove(context, item.id);
    }

    public static synchronized FavoritesEngine.Response<Boolean> permanentDeleteWithResponse(Context context, String id) {
        return FavoritesEngine.deletePermanent(context, id);
    }

    public static synchronized FavoritesEngine.Response<Boolean> softDeleteWithResponse(Context context, String id) {
        return FavoritesEngine.unfavorite(context, id);
    }

    public static synchronized FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> query(
            Context context,
            String type,
            String text,
            int limit
    ) {
        return FavoritesEngine.list(context, type, text, limit, false);
    }

    public static synchronized FavoritesEngine.Response<Boolean> touch(Context context, String id) {
        return FavoritesEngine.touchUsage(context, id);
    }

    public static synchronized Map<String, Object> debugHealthReport(Context context) {
        Map<String, Object> report = new LinkedHashMap<>();
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> all = FavoritesEngine.list(context, null, null, 1000, true);
        report.put("success", all.success);
        report.put("total_items", all.data == null ? 0 : all.data.size());
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> stale = FavoritesEngine.staleSuggestions(context);
        report.put("stale_items", stale.data == null ? 0 : stale.data.size());
        return report;
    }

    public static synchronized String explainDuplicateDecision(Context context, Map<String, String> payload, String type) {
        FavoritesEngine.Request request = new FavoritesEngine.Request(payload, type, FavoritesEngine.DuplicatePolicy.REJECT);
        FavoritesEngine.Response<FavoritesEngine.FavoriteRecord> response = FavoritesEngine.add(context, request);
        if (response.success) {
            FavoritesEngine.unfavorite(context, response.data.id);
            FavoritesEngine.deletePermanent(context, response.data.id);
            return "No duplicate found";
        }
        if (response.error == null) return "Unknown";
        return response.error.code + ": " + response.error.message;
    }

    public static synchronized List<FavoriteItem> getAllSorted(Context context) {
        return getAll(context);
    }

    public static synchronized int count(Context context) {
        return getAll(context).size();
    }

    public static synchronized List<String> types(Context context) {
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, 1000, true);
        List<String> out = new ArrayList<>();
        if (!response.success || response.data == null) return out;
        for (FavoritesEngine.FavoriteRecord r : response.data) {
            if (!out.contains(r.type)) out.add(r.type);
        }
        return out;
    }

    public static synchronized List<FavoriteItem> filterByType(Context context, String type) {
        return search(context, null, type);
    }

    public static synchronized void markAllUsed(Context context) {
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, 1000, false);
        if (!response.success || response.data == null) return;
        for (FavoritesEngine.FavoriteRecord r : response.data) {
            FavoritesEngine.touchUsage(context, r.id);
        }
    }

    public static synchronized void restoreSoftDeleted(Context context) {
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, 1000, true);
        if (!response.success || response.data == null) return;
        for (FavoritesEngine.FavoriteRecord r : response.data) {
            if (r.isDeleted) {
                Map<String, String> payload = new LinkedHashMap<>();
                payload.put("title", r.rawInput.optString("title", r.normalizedData.optString("title", "")));
                payload.put("note", r.rawInput.optString("note", r.normalizedData.optString("note", "")));
                payload.put("screen", r.rawInput.optString("screen", r.normalizedData.optString("screen", "")));
                FavoritesEngine.add(context, new FavoritesEngine.Request(payload, r.type, FavoritesEngine.DuplicatePolicy.MERGE));
            }
        }
    }

    public static synchronized void optimize(Context context) {
        // Trigger a read/write cycle so pruning and migration stay consistent.
        FavoritesEngine.Response<List<FavoritesEngine.FavoriteRecord>> response =
                FavoritesEngine.list(context, null, null, 1000, true);
        if (!response.success || response.data == null) return;
        for (FavoritesEngine.FavoriteRecord record : response.data) {
            if (record.usageCount < 0) {
                FavoritesEngine.touchUsage(context, record.id);
            }
        }
    }
}
