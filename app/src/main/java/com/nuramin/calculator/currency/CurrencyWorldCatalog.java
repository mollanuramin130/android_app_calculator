package com.nuramin.calculator.currency;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Backward-compatible entry point: delegates to {@link CurrencyRegistry} (ISO JSON + JVM + API merge).
 */
public final class CurrencyWorldCatalog {

    private CurrencyWorldCatalog() {}

    @NonNull
    public static List<CurrencyItem> getItems(@NonNull Context context) {
        return CurrencyRegistry.getAllWithSvgFlags(context);
    }

    /** Index of first item with code, or 0. */
    public static int indexOfCode(@NonNull List<CurrencyItem> items, @NonNull String code) {
        String u = code.toUpperCase(Locale.US);
        for (int i = 0; i < items.size(); i++) {
            if (u.equals(items.get(i).getCode())) return i;
        }
        return 0;
    }

    @Nullable
    public static CurrencyItem itemAt(@NonNull List<CurrencyItem> items, int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    /** Call after locale change if you need to reload asset-backed metadata. */
    public static synchronized void clearCache() {
        CurrencyRegistry.invalidate();
    }
}
