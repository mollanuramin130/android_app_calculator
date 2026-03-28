package com.nuramin.calculator.currency;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Indexes {@code assets/currency_flags/*.svg} once. Used to hide currencies with no bundled flag.
 */
public final class CurrencyFlagAssets {

    private static final String ASSET_DIR = "currency_flags";
    private static final Object LOCK = new Object();

    private static volatile Set<String> svgKeys;
    /** True after a successful non-empty scan; false if listing failed or folder empty. */
    private static volatile boolean indexLoaded;

    private CurrencyFlagAssets() {}

    /**
     * Whether we have a non-empty index of SVG filenames (without .svg). If false, callers should
     * not filter (e.g. missing assets in dev) so the app still lists currencies.
     */
    public static boolean hasIndexedAssets(@NonNull Context context) {
        ensureIndexed(context.getApplicationContext());
        return indexLoaded && svgKeys != null && !svgKeys.isEmpty();
    }

    /** Lowercase ISO-3166 alpha-2 file key, e.g. {@code us}, without .svg */
    public static boolean hasSvgForKey(@NonNull Context context, @Nullable String lowerCaseKey) {
        if (lowerCaseKey == null || lowerCaseKey.length() != 2) {
            return false;
        }
        ensureIndexed(context.getApplicationContext());
        if (svgKeys == null || svgKeys.isEmpty()) {
            return false;
        }
        return svgKeys.contains(lowerCaseKey.toLowerCase(Locale.US));
    }

    public static boolean hasSvgForItem(@NonNull Context context, @NonNull CurrencyItem item) {
        String key = CurrencySvgFlagLoader.svgFileKey(item);
        return key != null && hasSvgForKey(context, key);
    }

    private static void ensureIndexed(@NonNull Context appContext) {
        if (svgKeys != null) {
            return;
        }
        synchronized (LOCK) {
            if (svgKeys != null) {
                return;
            }
            HashSet<String> set = new HashSet<>();
            AssetManager am = appContext.getAssets();
            try {
                String[] names = am.list(ASSET_DIR);
                if (names != null) {
                    for (String n : names) {
                        if (n != null && n.endsWith(".svg") && n.length() > 4) {
                            set.add(n.substring(0, n.length() - 4).toLowerCase(Locale.US));
                        }
                    }
                }
            } catch (IOException ignored) {
                // leave empty
            }
            indexLoaded = !set.isEmpty();
            svgKeys = Collections.unmodifiableSet(set);
        }
    }
}
