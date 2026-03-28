package com.nuramin.calculator.currency;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads ISO 4217 metadata from {@code assets/currencies_iso4217.json}, merges JVM
 * {@link Currency#getAvailableCurrencies()}, and optional API-only codes from rates.
 */
public final class CurrencyRegistry {

    private static final String ASSET = "currencies_iso4217.json";
    private static final Object LOCK = new Object();

    private static volatile List<CurrencyItem> cachedList;
    private static volatile Map<String, CurrencyItem> byCode;

    private CurrencyRegistry() {}

    @NonNull
    public static List<CurrencyItem> getAll(@NonNull Context context) {
        synchronized (LOCK) {
            ensureLoaded(context.getApplicationContext());
            return Collections.unmodifiableList(new ArrayList<>(cachedList));
        }
    }

    /**
     * Only currencies with a matching file under {@code assets/currency_flags/&lt;cc&gt;.svg}.
     * If the asset folder could not be indexed, returns the same list as {@link #getAll} (safe fallback).
     */
    @NonNull
    public static List<CurrencyItem> getAllWithSvgFlags(@NonNull Context context) {
        synchronized (LOCK) {
            ensureLoaded(context.getApplicationContext());
            Context app = context.getApplicationContext();
            if (!CurrencyFlagAssets.hasIndexedAssets(app)) {
                return Collections.unmodifiableList(new ArrayList<>(cachedList));
            }
            ArrayList<CurrencyItem> out = new ArrayList<>();
            for (CurrencyItem c : cachedList) {
                if (CurrencyFlagAssets.hasSvgForItem(app, c)) {
                    out.add(c);
                }
            }
            return Collections.unmodifiableList(out);
        }
    }

    @Nullable
    public static CurrencyItem find(@NonNull Context context, @NonNull String code) {
        if (TextUtils.isEmpty(code)) return null;
        synchronized (LOCK) {
            ensureLoaded(context.getApplicationContext());
            return byCode.get(code.toUpperCase(Locale.US));
        }
    }

    /** Add currencies present in API but missing from the registry (dynamic codes). */
    public static void mergeCodesFromRates(@NonNull Context context, @Nullable Set<String> codes) {
        if (codes == null || codes.isEmpty()) return;
        synchronized (LOCK) {
            ensureLoaded(context.getApplicationContext());
            boolean changed = false;
            for (String raw : codes) {
                if (raw == null) continue;
                String c = raw.toUpperCase(Locale.US);
                if (byCode.containsKey(c)) continue;
                CurrencyItem item = buildFromJvmOrStub(c);
                byCode.put(c, item);
                changed = true;
            }
            if (changed) {
                rebuildSortedList();
            }
        }
    }

    /** Clear cached list (e.g. locale change or after merging API-only codes). */
    public static void invalidate() {
        synchronized (LOCK) {
            cachedList = null;
            byCode = null;
        }
    }

    private static void ensureLoaded(Context appContext) {
        if (cachedList != null && byCode != null) return;
        Map<String, CurrencyItem> map = new HashMap<>();
        readJsonInto(appContext, map);
        mergeJvmCurrencies(map);
        byCode = map;
        rebuildSortedList();
    }

    private static void rebuildSortedList() {
        ArrayList<CurrencyItem> list = new ArrayList<>(byCode.values());
        Collections.sort(list, (a, b) -> a.getCode().compareToIgnoreCase(b.getCode()));
        cachedList = list;
    }

    private static void readJsonInto(Context ctx, Map<String, CurrencyItem> map) {
        try (InputStream is = ctx.getAssets().open(ASSET);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String code = o.optString("code", "").trim().toUpperCase(Locale.US);
                if (code.length() != 3) continue;
                String name = o.optString("name", code).trim();
                String country = o.optString("country", "").trim();
                String flag = o.optString("flag", "").trim();
                if (flag.length() == 2) {
                    flag = flag.toUpperCase(Locale.US);
                } else {
                    flag = guessFlagCountry(code);
                }
                map.put(code, new CurrencyItem(code, name, country, flag));
            }
        } catch (Exception ignored) {
            // asset missing or invalid — rely on JVM merge only
        }
    }

    private static void mergeJvmCurrencies(Map<String, CurrencyItem> map) {
        Set<Currency> set = Currency.getAvailableCurrencies();
        for (Currency c : set) {
            String code = c.getCurrencyCode();
            if (map.containsKey(code)) continue;
            map.put(code, buildFromJvmOrStub(code));
        }
    }

    @NonNull
    private static CurrencyItem buildFromJvmOrStub(@NonNull String code) {
        try {
            Currency c = Currency.getInstance(code);
            String name = c.getDisplayName(Locale.ENGLISH);
            return new CurrencyItem(code, name, "", guessFlagCountry(code));
        } catch (Exception e) {
            return new CurrencyItem(code, code, "", null);
        }
    }

    /**
     * ISO-3166 alpha-2 for flag assets (same heuristics as internal {@link #guessFlagCountry}).
     * Used when {@link CurrencyItem#getFlagCountryCode()} is missing.
     */
    @Nullable
    public static String guessFlagCountryCode(@NonNull String iso4217) {
        return guessFlagCountry(iso4217);
    }

    /** Heuristic ISO-3166 hints for common ISO-4217 codes when not in JSON. */
    @Nullable
    private static String guessFlagCountry(String code) {
        switch (code) {
            case "USD": return "US";
            case "EUR": return "EU";
            case "GBP": return "GB";
            case "JPY": return "JP";
            case "CNY": return "CN";
            case "INR": return "IN";
            case "BDT": return "BD";
            case "AUD": return "AU";
            case "CAD": return "CA";
            case "CHF": return "CH";
            case "SEK": return "SE";
            case "NOK": return "NO";
            case "DKK": return "DK";
            case "PLN": return "PL";
            case "NZD": return "NZ";
            case "SGD": return "SG";
            case "HKD": return "HK";
            case "KRW": return "KR";
            case "MXN": return "MX";
            case "ZAR": return "ZA";
            case "TRY": return "TR";
            case "RUB": return "RU";
            case "BRL": return "BR";
            case "AED": return "AE";
            case "SAR": return "SA";
            case "ILS": return "IL";
            case "THB": return "TH";
            case "IDR": return "ID";
            case "MYR": return "MY";
            case "PHP": return "PH";
            case "PKR": return "PK";
            case "EGP": return "EG";
            case "NGN": return "NG";
            case "ARS": return "AR";
            case "CLP": return "CL";
            case "COP": return "CO";
            default:
                return null;
        }
    }
}
