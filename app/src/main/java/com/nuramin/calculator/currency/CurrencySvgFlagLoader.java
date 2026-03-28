package com.nuramin.calculator.currency;

import android.content.Context;
import android.graphics.Picture;
import android.graphics.drawable.PictureDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.nuramin.sunsetcoralcalculator.R;

import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads country flags from {@code assets/currency_flags/&lt;iso3166&gt;.svg} (4×3 SVG set).
 * Renders at the given width/height (typically 4:3) to match {@link R.dimen#currency_flag_render_width_main} /
 * {@link R.dimen#currency_flag_render_height_main} or picker variants.
 */
public final class CurrencySvgFlagLoader {

    private static final String ASSET_PREFIX = "currency_flags/";
    private static final int CACHE_MAX = 200;
    private static final Object CACHE_LOCK = new Object();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Key: svgKey + ":" + widthPx + "x" + heightPx */
    private static final android.util.LruCache<String, Picture> PICTURE_CACHE =
            new android.util.LruCache<>(CACHE_MAX);

    private CurrencySvgFlagLoader() {}

    /**
     * @param widthDimenRes  drawable width for SVG render (e.g. {@link R.dimen#currency_flag_render_width_main})
     * @param heightDimenRes drawable height for SVG render (e.g. {@link R.dimen#currency_flag_render_height_main})
     */
    public static void loadInto(
            @NonNull ImageView imageView,
            @NonNull CurrencyItem item,
            @DimenRes int widthDimenRes,
            @DimenRes int heightDimenRes) {
        Context ctx = imageView.getContext();
        int wPx = ctx.getResources().getDimensionPixelSize(widthDimenRes);
        int hPx = ctx.getResources().getDimensionPixelSize(heightDimenRes);
        String currencyCode = item.getCode();
        imageView.setTag(R.id.currency_flag_load_tag, currencyCode);

        String key = svgFileKey(item);
        if (key == null) {
            imageView.setLayerType(View.LAYER_TYPE_NONE, null);
            imageView.setImageResource(CurrencyFlagResolver.getFlagForCurrency(ctx, item));
            return;
        }

        String cacheKey = cacheKey(key, wPx, hPx);
        Picture cached;
        synchronized (CACHE_LOCK) {
            cached = PICTURE_CACHE.get(cacheKey);
        }
        if (cached != null) {
            applyPicture(imageView, currencyCode, cached, wPx, hPx);
            return;
        }

        imageView.setImageResource(R.drawable.flag_placeholder);
        Context app = ctx.getApplicationContext();
        EXECUTOR.execute(() -> {
            Picture picture = loadPicture(app, key, wPx, hPx);
            MAIN.post(() -> {
                Object tag = imageView.getTag(R.id.currency_flag_load_tag);
                if (tag == null || !currencyCode.equals(tag.toString())) {
                    return;
                }
                if (picture != null) {
                    synchronized (CACHE_LOCK) {
                        PICTURE_CACHE.put(cacheKey, picture);
                    }
                    applyPicture(imageView, currencyCode, picture, wPx, hPx);
                } else {
                    imageView.setLayerType(View.LAYER_TYPE_NONE, null);
                    imageView.setImageResource(CurrencyFlagResolver.getFlagForCurrency(ctx, item));
                }
            });
        });
    }

    @NonNull
    private static String cacheKey(@NonNull String svgKey, int wPx, int hPx) {
        return svgKey + ":" + wPx + "x" + hPx;
    }

    /** ISO-3166 key for {@code currency_flags/&lt;key&gt;.svg} (lowercase). */
    @Nullable
    public static String svgFileKey(@NonNull CurrencyItem item) {
        String fc = item.getFlagCountryCode();
        if (fc != null && fc.length() == 2) {
            return fc.toLowerCase(Locale.US);
        }
        String guessed = CurrencyRegistry.guessFlagCountryCode(item.getCode());
        if (guessed != null && guessed.length() == 2) {
            return guessed.toLowerCase(Locale.US);
        }
        return null;
    }

    @Nullable
    private static Picture loadPicture(@NonNull Context app, @NonNull String svgKey, int widthPx, int heightPx) {
        String path = ASSET_PREFIX + svgKey + ".svg";
        try (InputStream is = app.getAssets().open(path)) {
            SVG svg = SVG.getFromInputStream(is);
            svg.setDocumentWidth(widthPx);
            svg.setDocumentHeight(heightPx);
            return svg.renderToPicture();
        } catch (SVGParseException | java.io.IOException ignored) {
            return null;
        }
    }

    private static void applyPicture(
            @NonNull ImageView imageView,
            @NonNull String expectedCurrencyCode,
            @NonNull Picture picture,
            int widthPx,
            int heightPx) {
        Object tag = imageView.getTag(R.id.currency_flag_load_tag);
        if (tag == null || !expectedCurrencyCode.equals(tag.toString())) {
            return;
        }
        PictureDrawable drawable = new PictureDrawable(picture);
        drawable.setBounds(0, 0, widthPx, heightPx);
        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        imageView.setImageDrawable(drawable);
    }
}
