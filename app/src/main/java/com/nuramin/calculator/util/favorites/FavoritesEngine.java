package com.nuramin.calculator.util.favorites;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Intelligent Favorites engine with schema inference, duplicate detection, ranking,
 * soft-delete lifecycle and safe structured responses.
 */
public final class FavoritesEngine {
    private static final String TAG = "FavoritesEngine";
    private static final String PREFS = "favorite_storage_prefs";
    private static final String KEY_V2 = "favorite_items_v2_json";
    private static final String KEY_V1 = "favorite_items_json";
    private static final int MAX_ITEMS = 500;
    private static final long STALE_MS = 60L * 24L * 60L * 60L * 1000L; // 60 days
    private static volatile boolean debugMode = false;
    private static final Object LOCK = new Object();

    public enum DuplicatePolicy { REJECT, MERGE, KEEP_BOTH }

    public static final class Request {
        public final Map<String, String> rawInput;
        public final String explicitType;
        public final DuplicatePolicy duplicatePolicy;

        public Request(Map<String, String> rawInput, String explicitType, DuplicatePolicy duplicatePolicy) {
            this.rawInput = rawInput == null ? Collections.emptyMap() : rawInput;
            this.explicitType = explicitType == null ? "" : explicitType;
            this.duplicatePolicy = duplicatePolicy == null ? DuplicatePolicy.MERGE : duplicatePolicy;
        }
    }

    public static final class ErrorInfo {
        public final String code;
        public final String message;
        public final Map<String, Object> details;

        public ErrorInfo(String code, String message, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.details = details == null ? Collections.emptyMap() : details;
        }
    }

    public static final class Response<T> {
        public final boolean success;
        public final T data;
        public final ErrorInfo error;
        public final Map<String, Object> metadata;

        private Response(boolean success, T data, ErrorInfo error, Map<String, Object> metadata) {
            this.success = success;
            this.data = data;
            this.error = error;
            this.metadata = metadata == null ? Collections.emptyMap() : metadata;
        }

        public static <T> Response<T> ok(T data, Map<String, Object> metadata) {
            return new Response<>(true, data, null, metadata);
        }

        public static <T> Response<T> fail(String code, String message, Map<String, Object> details, Map<String, Object> metadata) {
            return new Response<>(false, null, new ErrorInfo(code, message, details), metadata);
        }
    }

    public static final class FavoriteRecord {
        public String id;
        public String type;
        public JSONObject normalizedData;
        public JSONObject rawInput;
        public long createdAt;
        public long lastUsed;
        public int usageCount;
        public String fingerprint;
        public boolean isFavorite;
        public boolean isDeleted;
        public int version;
    }

    private static final class Schema {
        final String type;
        final List<String> required;
        final List<String> optional;
        final Map<String, String> aliases;

        Schema(String type, List<String> required, List<String> optional, Map<String, String> aliases) {
            this.type = type;
            this.required = required;
            this.optional = optional;
            this.aliases = aliases;
        }
    }

    /** Result of schema normalization + validation (same rules as {@link #add}). */
    private static final class PreparedAdd {
        final String type;
        final Schema schema;
        final Map<String, String> normalized;
        final Response<Void> validation;

        PreparedAdd(String type, Schema schema, Map<String, String> normalized, Response<Void> validation) {
            this.type = type;
            this.schema = schema;
            this.normalized = normalized;
            this.validation = validation;
        }
    }

    private FavoritesEngine() {}

    /**
     * Validates payload without persisting (same inference, fallback, and required fields as {@link #add}).
     */
    public static Response<Void> validateForAdd(Request request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        return prepareForAdd(request, metadata).validation;
    }

    private static PreparedAdd prepareForAdd(Request request, Map<String, Object> metadata) {
        String type = inferType(request.explicitType, request.rawInput);
        metadata.put("inferred_type", type);
        Schema schema = schemaFor(type);
        Map<String, String> normalized = normalizeAndMap(request.rawInput, schema, metadata);
        Response<Void> validation = validate(schema, normalized);
        if (!validation.success) {
            String fallbackTitle = normalized.get("title");
            String fallbackNote = normalized.get("note");
            if (!TextUtils.isEmpty(fallbackTitle) || !TextUtils.isEmpty(fallbackNote)) {
                type = "calculator";
                schema = schemaFor(type);
                metadata.put("fallback_type", type);
                Map<String, String> fallbackInput = new LinkedHashMap<>(request.rawInput);
                if (TextUtils.isEmpty(fallbackInput.get("title")) && !TextUtils.isEmpty(fallbackTitle)) {
                    fallbackInput.put("title", fallbackTitle);
                }
                if (TextUtils.isEmpty(fallbackInput.get("note")) && !TextUtils.isEmpty(fallbackNote)) {
                    fallbackInput.put("note", fallbackNote);
                }
                normalized = normalizeAndMap(fallbackInput, schema, metadata);
                validation = validate(schema, normalized);
            }
        }
        return new PreparedAdd(type, schema, normalized, validation);
    }

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    public static Response<FavoriteRecord> add(Context context, Request request) {
        synchronized (LOCK) {
            try {
                long now = System.currentTimeMillis();
                List<FavoriteRecord> list = load(context);
                Map<String, Object> metadata = new LinkedHashMap<>();
                PreparedAdd prep = prepareForAdd(request, metadata);
                if (!prep.validation.success) {
                    return Response.fail(
                            prep.validation.error.code,
                            prep.validation.error.message,
                            prep.validation.error.details,
                            metadata);
                }
                String type = prep.type;
                Schema schema = prep.schema;
                Map<String, String> normalized = prep.normalized;

                String fingerprint = fingerprintFor(type, normalized, schema.required);
                metadata.put("fingerprint", fingerprint);

                FavoriteRecord exact = findByFingerprint(list, fingerprint);
                if (exact != null) {
                    metadata.put("duplicate", true);
                    metadata.put("existing_id", exact.id);
                    if (exact.isDeleted) {
                        exact.isDeleted = false;
                        exact.isFavorite = true;
                        exact.lastUsed = now;
                        exact.usageCount = Math.max(1, exact.usageCount + 1);
                        mergeNormalized(exact.normalizedData, normalized);
                        save(context, list);
                        return Response.ok(exact, metadata);
                    }
                    if (request.duplicatePolicy == DuplicatePolicy.REJECT) {
                        return Response.fail("DUPLICATE", "Duplicate favorite detected", metadata, metadata);
                    }
                    if (request.duplicatePolicy == DuplicatePolicy.MERGE) {
                        mergeInto(exact, request.rawInput, normalized, now);
                        save(context, list);
                        return Response.ok(exact, metadata);
                    }
                } else {
                    FavoriteRecord fuzzy = findByFuzzySimilarity(list, type, normalized);
                    if (fuzzy != null && request.duplicatePolicy == DuplicatePolicy.REJECT) {
                        metadata.put("duplicate", true);
                        metadata.put("existing_id", fuzzy.id);
                        return Response.fail("DUPLICATE_SIMILAR", "Similar favorite already exists", metadata, metadata);
                    }
                    if (fuzzy != null && request.duplicatePolicy == DuplicatePolicy.MERGE) {
                        mergeInto(fuzzy, request.rawInput, normalized, now);
                        save(context, list);
                        metadata.put("merged_into", fuzzy.id);
                        return Response.ok(fuzzy, metadata);
                    }
                }

                FavoriteRecord record = new FavoriteRecord();
                record.id = "fav_" + now + "_" + (int) (Math.random() * 1000);
                record.type = type;
                record.normalizedData = new JSONObject(normalized);
                record.rawInput = new JSONObject(request.rawInput);
                record.createdAt = now;
                record.lastUsed = now;
                record.usageCount = 1;
                record.fingerprint = fingerprint;
                record.isFavorite = true;
                record.isDeleted = false;
                record.version = 2;
                list.add(record);
                prune(list);
                save(context, list);
                return Response.ok(record, metadata);
            } catch (Exception e) {
                return Response.fail("INTERNAL_ERROR", "Failed to add favorite", mapOf("exception", e.getMessage()), null);
            }
        }
    }

    public static Response<Boolean> unfavorite(Context context, String id) {
        synchronized (LOCK) {
            try {
                List<FavoriteRecord> list = load(context);
                FavoriteRecord found = findById(list, id);
                if (found == null) return Response.fail("NOT_FOUND", "Favorite not found", mapOf("id", id), null);
                found.isFavorite = false;
                found.isDeleted = true;
                save(context, list);
                return Response.ok(true, null);
            } catch (Exception e) {
                return Response.fail("INTERNAL_ERROR", "Failed to remove favorite", mapOf("exception", e.getMessage()), null);
            }
        }
    }

    public static Response<Boolean> deletePermanent(Context context, String id) {
        synchronized (LOCK) {
            try {
                List<FavoriteRecord> list = load(context);
                List<FavoriteRecord> next = new ArrayList<>();
                boolean removed = false;
                for (FavoriteRecord r : list) {
                    if (TextUtils.equals(r.id, id)) {
                        removed = true;
                    } else {
                        next.add(r);
                    }
                }
                save(context, next);
                return Response.ok(removed, null);
            } catch (Exception e) {
                return Response.fail("INTERNAL_ERROR", "Failed to delete favorite", mapOf("exception", e.getMessage()), null);
            }
        }
    }

    public static Response<List<FavoriteRecord>> list(Context context, String typeFilter, String query, int limit, boolean includeDeleted) {
        synchronized (LOCK) {
            try {
                List<FavoriteRecord> all = load(context);
                long now = System.currentTimeMillis();
                List<FavoriteRecord> out = new ArrayList<>();
                for (FavoriteRecord r : all) {
                    if (!includeDeleted && (r.isDeleted || !r.isFavorite)) continue;
                    if (!TextUtils.isEmpty(typeFilter) && !typeFilter.equalsIgnoreCase(r.type)) continue;
                    if (!matchesQuery(r, query)) continue;
                    out.add(r);
                }
                Collections.sort(out, (a, b) -> Double.compare(score(b, typeFilter, query, now), score(a, typeFilter, query, now)));
                if (limit > 0 && out.size() > limit) out = new ArrayList<>(out.subList(0, limit));
                return Response.ok(out, mapOf("count", out.size()));
            } catch (Exception e) {
                return Response.fail("INTERNAL_ERROR", "Failed to list favorites", mapOf("exception", e.getMessage()), null);
            }
        }
    }

    public static Response<Boolean> touchUsage(Context context, String id) {
        synchronized (LOCK) {
            try {
                List<FavoriteRecord> list = load(context);
                FavoriteRecord found = findById(list, id);
                if (found == null) return Response.ok(false, null);
                found.lastUsed = System.currentTimeMillis();
                found.usageCount += 1;
                save(context, list);
                return Response.ok(true, null);
            } catch (Exception e) {
                return Response.fail("INTERNAL_ERROR", "Failed to update usage", mapOf("exception", e.getMessage()), null);
            }
        }
    }

    /** Updates display title in normalized + raw JSON (fingerprint unchanged — rename only). */
    public static Response<Boolean> updateTitle(Context context, String id, String newTitle) {
        synchronized (LOCK) {
            try {
                if (TextUtils.isEmpty(id)) return Response.fail("INVALID_ID", "Missing id", mapOf("id", id), null);
                String safe = newTitle == null ? "" : newTitle.trim();
                if (safe.isEmpty()) return Response.fail("INVALID_TITLE", "Title empty", null, null);
                List<FavoriteRecord> list = load(context);
                FavoriteRecord found = findById(list, id);
                if (found == null) return Response.fail("NOT_FOUND", "Favorite not found", mapOf("id", id), null);
                if (found.normalizedData == null) found.normalizedData = new JSONObject();
                found.normalizedData.put("title", safe);
                if (found.rawInput == null) found.rawInput = new JSONObject();
                found.rawInput.put("title", safe);
                save(context, list);
                return Response.ok(true, null);
            } catch (Exception e) {
                return Response.fail("INTERNAL_ERROR", "Failed to update title", mapOf("exception", e.getMessage()), null);
            }
        }
    }

    public static Response<List<FavoriteRecord>> staleSuggestions(Context context) {
        synchronized (LOCK) {
            try {
                List<FavoriteRecord> list = load(context);
                long now = System.currentTimeMillis();
                List<FavoriteRecord> stale = new ArrayList<>();
                for (FavoriteRecord r : list) {
                    if (r.isDeleted || !r.isFavorite) continue;
                    if (now - r.lastUsed > STALE_MS && r.usageCount <= 1) stale.add(r);
                }
                Collections.sort(stale, Comparator.comparingLong(a -> a.lastUsed));
                return Response.ok(stale, mapOf("stale_count", stale.size()));
            } catch (Exception e) {
                return Response.fail("INTERNAL_ERROR", "Failed stale analysis", mapOf("exception", e.getMessage()), null);
            }
        }
    }

    private static void mergeInto(FavoriteRecord target, Map<String, String> rawInput, Map<String, String> normalized, long now) {
        mergeNormalized(target.normalizedData, normalized);
        target.rawInput = new JSONObject(rawInput);
        target.lastUsed = now;
        target.usageCount += 1;
        target.isFavorite = true;
        target.isDeleted = false;
    }

    private static void mergeNormalized(JSONObject current, Map<String, String> incoming) {
        if (current == null || incoming == null) return;
        try {
            for (Map.Entry<String, String> e : incoming.entrySet()) {
                if (!TextUtils.isEmpty(e.getValue())) current.put(e.getKey(), e.getValue());
            }
        } catch (Exception ignored) {
        }
    }

    private static FavoriteRecord findById(List<FavoriteRecord> list, String id) {
        for (FavoriteRecord r : list) {
            if (TextUtils.equals(r.id, id)) return r;
        }
        return null;
    }

    private static FavoriteRecord findByFingerprint(List<FavoriteRecord> list, String fp) {
        for (FavoriteRecord r : list) {
            if (TextUtils.equals(r.fingerprint, fp)) return r;
        }
        return null;
    }

    private static FavoriteRecord findByFuzzySimilarity(List<FavoriteRecord> list, String type, Map<String, String> normalized) {
        String incoming = canonical(normalized);
        FavoriteRecord best = null;
        double bestScore = 0.0;
        for (FavoriteRecord r : list) {
            if (r.isDeleted || !r.isFavorite) continue;
            if (!TextUtils.equals(type, r.type)) continue;
            double s = jaccard(incoming, canonical(toMap(r.normalizedData)));
            if (s > bestScore) {
                bestScore = s;
                best = r;
            }
        }
        if (debugMode) Log.d(TAG, "fuzzy bestScore=" + bestScore);
        return bestScore >= sensitivityFor(type) ? best : null;
    }

    private static double sensitivityFor(String type) {
        if ("calculator".equals(type)) return 0.90;
        if ("date".equals(type)) return 0.88;
        return 0.86;
    }

    private static boolean matchesQuery(FavoriteRecord r, String query) {
        if (TextUtils.isEmpty(query)) return true;
        String q = normalizeToken(query);
        String bag = normalizeToken(opt(r.normalizedData, "title")) + " "
                + normalizeToken(opt(r.normalizedData, "note")) + " "
                + normalizeToken(opt(r.normalizedData, "screen")) + " "
                + normalizeToken(r.type);
        if (bag.contains(q)) return true;
        return jaccard(q, bag) >= 0.45;
    }

    private static double score(FavoriteRecord r, String contextType, String query, long now) {
        double recency = 1.0 / (1.0 + ((double) (now - r.lastUsed) / (24d * 60d * 60d * 1000d)));
        double freq = Math.min(1.0, r.usageCount / 15.0);
        double context = (!TextUtils.isEmpty(contextType) && contextType.equalsIgnoreCase(r.type)) ? 1.0 : 0.2;
        double queryScore = TextUtils.isEmpty(query) ? 0.5 : (matchesQuery(r, query) ? 1.0 : 0.0);
        return recency * 0.35 + freq * 0.35 + context * 0.15 + queryScore * 0.15;
    }

    private static Response<Void> validate(Schema schema, Map<String, String> normalized) {
        List<String> missing = new ArrayList<>();
        for (String req : schema.required) {
            String v = normalized.get(req);
            if (TextUtils.isEmpty(v)) missing.add(req);
        }
        if (!missing.isEmpty()) {
            return Response.fail("MISSING_REQUIRED", "Missing required fields", mapOf("missing_fields", missing), null);
        }
        return Response.ok(null, null);
    }

    private static Map<String, String> normalizeAndMap(Map<String, String> rawInput, Schema schema, Map<String, Object> metadata) {
        Map<String, String> mapped = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : rawInput.entrySet()) {
            String rawKey = normalizeToken(e.getKey());
            String mappedKey = schema.aliases.get(rawKey);
            if (TextUtils.isEmpty(mappedKey)) mappedKey = rawKey;
            String value = normalizeValue(mappedKey, e.getValue());
            if (!TextUtils.isEmpty(value)) mapped.put(mappedKey, value);
        }
        for (String req : schema.required) {
            if (!mapped.containsKey(req)) {
                String suggestion = defaultFor(req, schema.type, mapped);
                if (!TextUtils.isEmpty(suggestion)) mapped.put(req, suggestion);
            }
        }
        metadata.put("mapped_fields", new ArrayList<>(mapped.keySet()));
        return mapped;
    }

    private static String defaultFor(String field, String type, Map<String, String> mapped) {
        if ("title".equals(field) && !TextUtils.isEmpty(mapped.get("screen"))) return mapped.get("screen");
        if ("screen".equals(field) && !TextUtils.isEmpty(type)) return type.toUpperCase(Locale.US);
        return "";
    }

    private static String normalizeValue(String key, String input) {
        if (input == null) return "";
        String v = input.trim();
        if (v.isEmpty()) return "";
        if ("title".equals(key) || "note".equals(key) || "screen".equals(key)) {
            return v.replaceAll("\\s+", " ");
        }
        String cleaned = v.toLowerCase(Locale.US).replace(",", "").trim();
        cleaned = cleaned.replaceAll("[₹$€£¥¤]|Rs\\.", "").trim();
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }

    private static String inferType(String explicitType, Map<String, String> rawInput) {
        if (!TextUtils.isEmpty(explicitType)) return normalizeToken(explicitType);
        String bag = "";
        for (Map.Entry<String, String> e : rawInput.entrySet()) {
            bag += " " + normalizeToken(e.getKey()) + " " + normalizeToken(e.getValue());
        }
        if (bag.contains("emi") || bag.contains("tenure") || bag.contains("principal")) return "emi";
        if (bag.contains("tax") || bag.contains("gst")) return "tax";
        if (bag.contains("date") || bag.contains("dob")) return "date";
        if (bag.contains("unit") || bag.contains("from") && bag.contains("to")) return "unit_converter";
        if (bag.contains("interest")) return "interest";
        return "calculator";
    }

    private static Schema schemaFor(String type) {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("name", "title");
        aliases.put("heading", "title");
        aliases.put("desc", "note");
        aliases.put("description", "note");
        aliases.put("mode", "screen");
        aliases.put("type", "screen");
        aliases.put("calculation", "note");
        aliases.put("result", "result");
        aliases.put("date1", "date_1");
        aliases.put("date2", "date_2");
        aliases.put("date 1", "date_1");
        aliases.put("date 2", "date_2");
        aliases.put("from", "from_unit");
        aliases.put("to", "to_unit");
        aliases.put("fromunit", "from_unit");
        aliases.put("from unit", "from_unit");
        aliases.put("tounit", "to_unit");
        aliases.put("to unit", "to_unit");
        aliases.put("tax rate", "rate");
        aliases.put("amount", "value");
        aliases.put("value", "value");
        aliases.put("tax rate", "rate");
        aliases.put("tax_rate", "rate");
        aliases.put("rate %", "rate");
        aliases.put("rate percentage", "rate");
        aliases.put("principal amount", "principal");
        aliases.put("tenure period", "tenure");

        if ("emi".equals(type)) {
            return new Schema(type,
                    listOf("title", "principal", "rate", "tenure"),
                    listOf("note", "screen", "result"),
                    aliases);
        }
        if ("interest".equals(type)) {
            return new Schema(type,
                    listOf("title", "principal", "rate", "time"),
                    listOf("note", "screen", "result", "type", "time unit"),
                    aliases);
        }
        if ("tax".equals(type)) {
            return new Schema(type,
                    listOf("title", "amount", "rate"),
                    listOf("note", "screen", "result"),
                    aliases);
        }
        if ("date".equals(type)) {
            return new Schema(type,
                    listOf("title", "date_1"),
                    listOf("date_2", "mode", "note", "screen", "result"),
                    aliases);
        }
        if ("unit_converter".equals(type)) {
            return new Schema(type,
                    listOf("title", "value", "from_unit", "to_unit"),
                    listOf("note", "screen", "result"),
                    aliases);
        }
        return new Schema(type,
                listOf("title"),
                listOf("note", "screen", "result"),
                aliases);
    }

    private static String fingerprintFor(String type, Map<String, String> normalized, List<String> criticalFields) {
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String k : criticalFields) ordered.put(k, normalizeToken(normalized.get(k)));
        String payload = type + "|" + canonical(ordered);
        return sha256(payload);
    }

    private static String canonical(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "";
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            String v = map.get(k);
            if (TextUtils.isEmpty(v)) continue;
            if (sb.length() > 0) sb.append('|');
            sb.append(k).append('=').append(normalizeToken(v));
        }
        return sb.toString();
    }

    private static double jaccard(String a, String b) {
        Set<String> sa = new HashSet<>(tokenize(a));
        Set<String> sb = new HashSet<>(tokenize(b));
        if (sa.isEmpty() && sb.isEmpty()) return 1.0;
        Set<String> inter = new HashSet<>(sa);
        inter.retainAll(sb);
        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);
        return union.isEmpty() ? 0.0 : ((double) inter.size() / (double) union.size());
    }

    private static List<String> tokenize(String s) {
        if (TextUtils.isEmpty(s)) return Collections.emptyList();
        String[] arr = normalizeToken(s).split("\\s+");
        List<String> out = new ArrayList<>();
        for (String t : arr) if (!TextUtils.isEmpty(t)) out.add(t);
        return out;
    }

    private static String normalizeToken(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9_\\s.\\-/]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format(Locale.US, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private static List<FavoriteRecord> load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_V2, "");
        if (TextUtils.isEmpty(raw)) {
            List<FavoriteRecord> migrated = migrateFromV1(prefs.getString(KEY_V1, "[]"));
            if (!migrated.isEmpty()) save(context, migrated);
            return migrated;
        }
        List<FavoriteRecord> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                FavoriteRecord r = new FavoriteRecord();
                r.id = o.optString("id", "");
                r.type = o.optString("type", "calculator");
                r.normalizedData = o.optJSONObject("normalized_data");
                if (r.normalizedData == null) r.normalizedData = new JSONObject();
                r.rawInput = o.optJSONObject("raw_input");
                if (r.rawInput == null) r.rawInput = new JSONObject();
                r.createdAt = o.optLong("created_at", 0L);
                r.lastUsed = o.optLong("last_used", r.createdAt);
                r.usageCount = o.optInt("usage_count", 1);
                r.fingerprint = o.optString("fingerprint", "");
                r.isFavorite = o.optBoolean("is_favorite", true);
                r.isDeleted = o.optBoolean("is_deleted", false);
                r.version = o.optInt("version", 2);
                out.add(r);
            }
        } catch (Exception e) {
            Log.w(TAG, "Favorites JSON parse failed; using empty list until next save", e);
        }
        return out;
    }

    private static List<FavoriteRecord> migrateFromV1(String oldRaw) {
        List<FavoriteRecord> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(oldRaw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                FavoriteRecord r = new FavoriteRecord();
                r.id = o.optString("id", "");
                String title = o.optString("title", "");
                String note = o.optString("note", "");
                String screen = o.optString("screen", "");
                Map<String, String> legacyInput = new LinkedHashMap<>();
                legacyInput.put("title", title);
                legacyInput.put("note", note);
                legacyInput.put("screen", screen);
                r.type = inferType(screen, legacyInput);
                Map<String, String> normalized = new LinkedHashMap<>();
                normalized.put("title", normalizeValue("title", title));
                normalized.put("note", normalizeValue("note", note));
                normalized.put("screen", normalizeValue("screen", screen));
                r.normalizedData = new JSONObject(normalized);
                r.rawInput = new JSONObject(mapOf("title", title, "note", note, "screen", screen));
                r.createdAt = o.optLong("timestamp", System.currentTimeMillis());
                r.lastUsed = r.createdAt;
                r.usageCount = 1;
                r.fingerprint = fingerprintFor(r.type, normalized, schemaFor(r.type).required);
                r.isFavorite = true;
                r.isDeleted = false;
                r.version = 2;
                out.add(r);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void save(Context context, List<FavoriteRecord> list) {
        JSONArray arr = new JSONArray();
        try {
            for (FavoriteRecord r : list) {
                JSONObject o = new JSONObject();
                o.put("id", r.id);
                o.put("type", r.type);
                o.put("normalized_data", r.normalizedData == null ? new JSONObject() : r.normalizedData);
                o.put("raw_input", r.rawInput == null ? new JSONObject() : r.rawInput);
                o.put("created_at", r.createdAt);
                o.put("last_used", r.lastUsed);
                o.put("usage_count", r.usageCount);
                o.put("fingerprint", r.fingerprint);
                o.put("is_favorite", r.isFavorite);
                o.put("is_deleted", r.isDeleted);
                o.put("version", r.version <= 0 ? 2 : r.version);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_V2, arr.toString()).apply();
    }

    private static void prune(List<FavoriteRecord> list) {
        if (list.size() <= MAX_ITEMS) return;
        Collections.sort(list, Comparator.comparingLong(a -> a.lastUsed));
        while (list.size() > MAX_ITEMS) list.remove(0);
    }

    private static String opt(JSONObject o, String key) {
        return o == null ? "" : o.optString(key, "");
    }

    private static List<String> listOf(String... values) {
        List<String> out = new ArrayList<>();
        if (values == null) return out;
        Collections.addAll(out, values);
        return out;
    }

    private static Map<String, String> toMap(JSONObject o) {
        Map<String, String> out = new LinkedHashMap<>();
        if (o == null) return out;
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            out.put(k, o.optString(k, ""));
        }
        return out;
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (kv == null) return out;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return out;
    }
}
