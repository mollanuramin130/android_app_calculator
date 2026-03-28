package com.nuramin.sunsetcoralcalculator.ai.system;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.nuramin.sunsetcoralcalculator.R;

/**
 * Central place for sharing the app and resolving the public Play Store listing URL.
 * <p>
 * The store listing URL used for sharing and fallbacks is defined only here (and via
 * {@link #getPlayStoreListingUrl(Context)} for PDF footers etc.).
 */
public final class ShareController {

    /** Application id on Google Play (matches {@code applicationId}). */
    public static final String PLAY_STORE_PACKAGE_ID = "com.nuramin.sunsetcoralcalculator";

    private ShareController() {}

    @NonNull
    private static String buildShareText(@NonNull Context context) {
        return "📱 " + context.getString(R.string.app_name) + "\n\n"
                + "All-in-one calculator for EMI, Interest, Age, GST, Discount and more.\n\n"
                + "Try it now:\n"
                + "https://play.google.com/store/apps/details?id=" + PLAY_STORE_PACKAGE_ID;
    }

    /**
     * Public HTTPS URL for this app on Google Play. Prefer this over duplicating links elsewhere.
     */
    @NonNull
    public static String getPlayStoreListingUrl(@NonNull Context context) {
        return "https://play.google.com/store/apps/details?id=" + context.getPackageName();
    }

    /**
     * Opens {@link Intent#ACTION_SEND} chooser with the polished marketing message.
     *
     * @param chooserTitle use {@link R.string#share_via_chooser} or a translated equivalent
     */
    public static void shareApp(@NonNull AppCompatActivity activity, @NonNull String chooserTitle) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, buildShareText(activity));
            activity.startActivity(Intent.createChooser(shareIntent, chooserTitle));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.share_app_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens the Play Store app page (market://) with https fallback. Used when review API fails
     * or for “write a review” flows — not for the share message body.
     */
    public static void openPlayStoreListing(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                return;
            }
        } catch (Exception ignored) {
            // fall through to https
        }
        try {
            Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(getPlayStoreListingUrl(context)));
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallback);
        } catch (Exception ignored) {
            // No browser / store
        }
    }
}
