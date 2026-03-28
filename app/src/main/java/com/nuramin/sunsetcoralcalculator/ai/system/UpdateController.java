package com.nuramin.sunsetcoralcalculator.ai.system;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.nuramin.sunsetcoralcalculator.R;

/**
 * In-app updates via Play Core — <strong>flexible only</strong> (no immediate, no Play Store redirect).
 * <p>
 * No {@link AppCompatActivity} is stored; every entry point receives the current activity so
 * recreation is handled correctly.
 */
public final class UpdateController {

    private static final String PREFS = "update_controller_v1";
    private static final String KEY_LAST_API_CHECK_MS = "last_api_check_ms";
    private static final String KEY_LAST_SNOOZE_MS = "last_snooze_ms";
    private static final long INTERVAL_MS = 24L * 60L * 60L * 1000L;

    /** Process-scoped; survives activity recreation when using application context. */
    @Nullable
    private AppUpdateManager appUpdateManager;

    @Nullable
    private ActivityResultLauncher<IntentSenderRequest> updateResultLauncher;
    @Nullable
    private InstallStateUpdatedListener installStateListener;
    /** Avoid stacking multiple bottom sheets in one session. */
    private boolean bottomSheetShownThisSession;
    /** User chose Update Now — do not treat dismiss as snooze. */
    private boolean userChoseUpdateThisSheet;

    public UpdateController() {}

    public void setUpdateResultLauncher(@Nullable ActivityResultLauncher<IntentSenderRequest> launcher) {
        this.updateResultLauncher = launcher;
    }

    private AppUpdateManager getOrCreateManager(AppCompatActivity activity) {
        if (appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(activity.getApplicationContext());
        }
        return appUpdateManager;
    }

    private static View snackbarAnchor(AppCompatActivity activity) {
        View v = activity.findViewById(R.id.drawer_layout);
        if (v != null) return v;
        return activity.findViewById(android.R.id.content);
    }

    /**
     * Call from {@link AppCompatActivity#onResume()}: checks pending downloaded flexible update,
     * then runs throttled automatic update check (bottom sheet when eligible).
     */
    public void checkForUpdate(AppCompatActivity activity) {
        AppUpdateManager mgr = getOrCreateManager(activity);
        mgr.getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (!canShowUi(activity)) return;
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        showRestartSnackbar(activity);
                        return;
                    }
                    checkForUpdateAutomatic(activity, mgr);
                })
                .addOnFailureListener(e -> {
                    if (!canShowUi(activity)) return;
                    checkForUpdateAutomatic(activity, mgr);
                });
    }

    private void checkForUpdateAutomatic(AppCompatActivity activity, AppUpdateManager mgr) {
        if (!isNetworkAvailable(activity)) return;
        if (shouldSkipDueToSnooze(activity)) return;
        if (shouldSkipDueToApiThrottle(activity)) return;

        Task<AppUpdateInfo> task = mgr.getAppUpdateInfo();
        task.addOnSuccessListener(info -> {
            if (!canShowUi(activity)) return;
            markApiCheckNow(activity);
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return;
            if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) return;
            showUpdateBottomSheet(activity, mgr, info, false);
        });
        task.addOnFailureListener(e -> { /* Missing Play services / sideload */ });
    }

    /**
     * Overflow “Check for updates”: queries Play; shows bottom sheet only if flexible is allowed.
     */
    public void checkForUpdateManual(AppCompatActivity activity) {
        if (!isNetworkAvailable(activity)) {
            toast(activity, R.string.update_no_internet);
            return;
        }
        AppUpdateManager mgr = getOrCreateManager(activity);
        Task<AppUpdateInfo> task = mgr.getAppUpdateInfo();
        task.addOnSuccessListener(info -> {
            if (!canShowUi(activity)) return;
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                toast(activity, R.string.update_up_to_date);
                return;
            }
            if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                userChoseUpdateThisSheet = false;
                showUpdateBottomSheet(activity, mgr, info, true);
            } else {
                toast(activity, R.string.update_up_to_date);
            }
        });
        task.addOnFailureListener(e -> toast(activity, R.string.update_no_internet));
    }

    private void showUpdateBottomSheet(AppCompatActivity activity, AppUpdateManager mgr,
            AppUpdateInfo appUpdateInfo, boolean forceShow) {
        if (!canShowUi(activity)) return;
        if (!forceShow && bottomSheetShownThisSession) return;
        bottomSheetShownThisSession = true;
        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        View root = LayoutInflater.from(activity).inflate(R.layout.update_bottom_sheet, null);
        sheet.setContentView(root);
        userChoseUpdateThisSheet = false;

        root.findViewById(R.id.update_sheet_btn_later).setOnClickListener(v -> {
            sheet.dismiss();
            markSnoozeNow(activity);
        });
        root.findViewById(R.id.update_sheet_btn_now).setOnClickListener(v -> {
            userChoseUpdateThisSheet = true;
            sheet.dismiss();
            startFlexibleUpdateFlow(activity, mgr, appUpdateInfo);
        });
        sheet.setOnDismissListener(dialog -> {
            if (!userChoseUpdateThisSheet) {
                markSnoozeNow(activity);
            }
        });
        sheet.show();
    }

    private void startFlexibleUpdateFlow(AppCompatActivity activity, AppUpdateManager mgr,
            AppUpdateInfo appUpdateInfo) {
        if (!canShowUi(activity)) return;
        if (updateResultLauncher == null) {
            toast(activity, R.string.update_up_to_date);
            return;
        }
        registerInstallStateListener(activity, mgr);
        AppUpdateOptions options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build();
        mgr.startUpdateFlowForResult(appUpdateInfo, updateResultLauncher, options);
    }

    private void registerInstallStateListener(AppCompatActivity activity, AppUpdateManager mgr) {
        unregisterInstallStateListener(mgr);
        final WeakReference<AppCompatActivity> activityRef = new WeakReference<>(activity);
        installStateListener = state -> {
            AppCompatActivity act = activityRef.get();
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                unregisterInstallStateListener(mgr);
            }
            if (!canShowUi(act)) return;
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                long total = state.totalBytesToDownload();
                long downloaded = state.bytesDownloaded();
                if (total > 0) {
                    int pct = (int) (100 * downloaded / total);
                    showSnackbar(act, act.getString(R.string.update_downloading_progress, pct), false);
                } else {
                    showSnackbar(act, act.getString(R.string.update_downloading), false);
                }
            } else if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartSnackbar(act);
            }
        };
        mgr.registerListener(installStateListener);
    }

    private void unregisterInstallStateListener(AppUpdateManager mgr) {
        if (installStateListener != null) {
            mgr.unregisterListener(installStateListener);
            installStateListener = null;
        }
    }

    private void showRestartSnackbar(AppCompatActivity activity) {
        if (!canShowUi(activity)) return;
        AppUpdateManager mgr = getOrCreateManager(activity);
        Snackbar snackbar = Snackbar.make(
                snackbarAnchor(activity),
                activity.getString(R.string.update_restart_message),
                Snackbar.LENGTH_INDEFINITE
        );
        snackbar.setAction(R.string.update_btn_restart, v -> {
            snackbar.dismiss();
            try {
                mgr.completeUpdate();
            } catch (Exception ignored) {
                toast(activity, R.string.update_up_to_date);
            }
        });
        snackbar.show();
    }

    private void showSnackbar(AppCompatActivity activity, CharSequence message, boolean shortDuration) {
        if (!canShowUi(activity)) return;
        Snackbar.make(snackbarAnchor(activity), message,
                shortDuration ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG).show();
    }

    private void toast(AppCompatActivity activity, int resId) {
        if (!canShowUi(activity)) return;
        Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show();
    }

    private static boolean canShowUi(AppCompatActivity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    public void onDestroy() {
        if (appUpdateManager != null) {
            unregisterInstallStateListener(appUpdateManager);
        }
    }

    private boolean isNetworkAvailable(AppCompatActivity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }

    private SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private boolean shouldSkipDueToSnooze(AppCompatActivity activity) {
        long lastSnooze = prefs(activity).getLong(KEY_LAST_SNOOZE_MS, 0L);
        return System.currentTimeMillis() - lastSnooze < INTERVAL_MS;
    }

    private boolean shouldSkipDueToApiThrottle(AppCompatActivity activity) {
        long last = prefs(activity).getLong(KEY_LAST_API_CHECK_MS, 0L);
        return System.currentTimeMillis() - last < INTERVAL_MS;
    }

    private void markApiCheckNow(AppCompatActivity activity) {
        prefs(activity).edit().putLong(KEY_LAST_API_CHECK_MS, System.currentTimeMillis()).apply();
    }

    private void markSnoozeNow(AppCompatActivity activity) {
        prefs(activity).edit().putLong(KEY_LAST_SNOOZE_MS, System.currentTimeMillis()).apply();
    }
}
