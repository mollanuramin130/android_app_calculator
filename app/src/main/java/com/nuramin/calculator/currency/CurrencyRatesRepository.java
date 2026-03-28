package com.nuramin.calculator.currency;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live FX: USD-normalized rates from open.er-api.com (broad currency list, no API key).
 * Persists last successful JSON for offline use.
 */
public final class CurrencyRatesRepository {

    private static final String PREFS = "currency_rates_repo_v1";
    private static final String KEY_CACHED_JSON = "cached_json";

    /** open.er-api.com — many ISO currencies per 1 USD (same shape as Frankfurter base USD). */
    private static final String API_URL =
            "https://open.er-api.com/v6/latest/USD";

    private static final ConcurrentHashMap<String, Double> USD_PER_UNIT = new ConcurrentHashMap<>();

    static {
        applyBuiltinSample();
    }

    private CurrencyRatesRepository() {}

    /** Built-in USD-per-unit values when no cache/API yet (overwritten by live JSON). */
    private static void applyBuiltinSample() {
        USD_PER_UNIT.put("USD", 1.0);
        USD_PER_UNIT.put("EUR", 1.08);
        USD_PER_UNIT.put("GBP", 1.27);
        USD_PER_UNIT.put("INR", 0.012);
        USD_PER_UNIT.put("BDT", 0.0085);
        USD_PER_UNIT.put("JPY", 1.0 / 150.0);
        USD_PER_UNIT.put("CNY", 1.0 / 7.2);
        USD_PER_UNIT.put("AUD", 0.65);
        USD_PER_UNIT.put("CAD", 0.73);
        USD_PER_UNIT.put("CHF", 1.12);
    }

    /** Merge API rate codes into {@link CurrencyRegistry} so pickers include rare ISO codes. */
    public static void notifyRegistryOfRateCodes(@NonNull Context context) {
        if (USD_PER_UNIT.isEmpty()) return;
        CurrencyRegistry.mergeCodesFromRates(context, new HashSet<>(USD_PER_UNIT.keySet()));
    }

    @NonNull
    public static Set<String> getKnownRateCodes() {
        return new HashSet<>(USD_PER_UNIT.keySet());
    }

    public static double getUsdPerUnit(@NonNull String currencyCode) {
        String c = currencyCode.trim().toUpperCase();
        if ("USD".equals(c)) {
            Double u = USD_PER_UNIT.get("USD");
            if (u != null && u > 0 && Double.isFinite(u)) return u;
            return 1.0;
        }
        Double v = USD_PER_UNIT.get(c);
        if (v != null && v > 0 && Double.isFinite(v)) return v;
        return Double.NaN;
    }

    /** Apply rates object: value = foreign units per 1 USD → USD per 1 foreign = 1/value. */
    public static void applyOpenErRatesJson(@NonNull JSONObject root) {
        JSONObject rates = root.optJSONObject("rates");
        if (rates == null || rates.length() == 0) return;
        USD_PER_UNIT.clear();
        for (Iterator<String> it = rates.keys(); it.hasNext(); ) {
            String rawKey = it.next();
            double foreignPerUsd = rates.optDouble(rawKey, 0);
            if (!Double.isFinite(foreignPerUsd) || foreignPerUsd <= 0) continue;
            String code = rawKey.toUpperCase(Locale.US);
            if ("USD".equals(code)) {
                USD_PER_UNIT.put("USD", 1.0);
            } else {
                USD_PER_UNIT.put(code, 1.0 / foreignPerUsd);
            }
        }
        if (!USD_PER_UNIT.containsKey("USD")) {
            USD_PER_UNIT.put("USD", 1.0);
        }
    }

    public static void loadCacheFromPrefs(@NonNull Context context) {
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(KEY_CACHED_JSON, null);
        if (json == null || json.isEmpty()) return;
        try {
            applyOpenErRatesJson(new JSONObject(json));
        } catch (Exception ignored) {
        }
    }

    public static void saveCacheToPrefs(@NonNull Context context, @NonNull String json) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_JSON, json)
                .apply();
    }

    /**
     * Fetches latest rates. Returns raw JSON on success, or null.
     */
    @Nullable
    public static String fetchLatestJson() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            String body = sb.toString();
            JSONObject root = new JSONObject(body);
            if (!"success".equalsIgnoreCase(root.optString("result"))) {
                return null;
            }
            applyOpenErRatesJson(root);
            return body;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
