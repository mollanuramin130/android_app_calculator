package com.nuramin.calculator;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;

/**
 * Handles in-app updates: check availability, show update dialog, flexible/immediate flow,
 * download progress, restart prompt, and fallback to Play Store.
 */
public final class UpdateHelper {

    private static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.nuramin.sunsetcoralcalculator";

    private final AppCompatActivity activity;
    private final AppUpdateManager appUpdateManager;
    private final View snackbarAnchor;

    @Nullable
    private ActivityResultLauncher<IntentSenderRequest> updateResultLauncher;
    @Nullable
    private InstallStateUpdatedListener installStateListener;
    /** Show "Update Available" popup only once per app session. */
    private boolean updatePopupShownThisSession;

    public UpdateHelper(AppCompatActivity activity, View snackbarAnchor) {
        this.activity = activity;
        this.snackbarAnchor = snackbarAnchor != null ? snackbarAnchor : activity.getWindow().getDecorView();
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
    }

    /**
     * Set the activity result launcher for the update flow. Must be called from the activity
     * after registering with registerForActivityResult(StartIntentSenderForResult(), ...).
     */
    public void setUpdateResultLauncher(@Nullable ActivityResultLauncher<IntentSenderRequest> launcher) {
        this.updateResultLauncher = launcher;
    }

    /**
     * Check for update on app start. If update is available and FLEXIBLE, show "Update Available" popup
     * once per session. Does not block; runs async.
     */
    public void checkForUpdateOnStart() {
        if (!isNetworkAvailable()) {
            showToast(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_no_internet));
            return;
        }
        Task<AppUpdateInfo> task = appUpdateManager.getAppUpdateInfo();
        task.addOnSuccessListener(this::onUpdateInfoSuccessForStart);
        task.addOnFailureListener(e -> {
            // Play Core failed; fallback to Play Store link if user tries manual check
            if (updateResultLauncher == null) return;
        });
    }

    /**
     * Manual "Check for Updates" from menu. Shows popup if update available, or "Your app is up to date" toast.
     */
    public void checkForUpdateManual() {
        if (!isNetworkAvailable()) {
            showToast(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_no_internet));
            return;
        }
        Task<AppUpdateInfo> task = appUpdateManager.getAppUpdateInfo();
        task.addOnSuccessListener(this::onUpdateInfoSuccessForManual);
        task.addOnFailureListener(e -> openPlayStoreFallback());
    }

    private void onUpdateInfoSuccessForStart(AppUpdateInfo appUpdateInfo) {
        if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return;
        // Prefer FLEXIBLE so we don't interrupt calculator usage
        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            if (!updatePopupShownThisSession) {
                updatePopupShownThisSession = true;
                showUpdateAvailableDialog(appUpdateInfo, AppUpdateType.FLEXIBLE);
            }
        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            // Critical path: allow immediate if flexible not allowed
            if (!updatePopupShownThisSession) {
                updatePopupShownThisSession = true;
                startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE);
            }
        }
    }

    private void onUpdateInfoSuccessForManual(AppUpdateInfo appUpdateInfo) {
        if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            showToast(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_up_to_date));
            return;
        }
        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            showUpdateAvailableDialog(appUpdateInfo, AppUpdateType.FLEXIBLE);
        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE);
        } else {
            showToast(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_up_to_date));
        }
    }

    /** Show "New Update Available" Material-style dialog; Update Now starts flow, Later dismisses. */
    private void showUpdateAvailableDialog(AppUpdateInfo appUpdateInfo, int updateType) {
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_dialog_title))
                .setMessage(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_dialog_message))
                .setPositiveButton(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_btn_now), (dialog, which) -> {
                    dialog.dismiss();
                    startUpdateFlow(appUpdateInfo, updateType);
                })
                .setNegativeButton(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_btn_later), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    /**
     * Start the update flow. For FLEXIBLE, registers install state listener and shows progress/snackbar.
     * For IMMEDIATE, blocks until update completes.
     */
    private void startUpdateFlow(AppUpdateInfo appUpdateInfo, int updateType) {
        if (updateResultLauncher == null) {
            openPlayStoreFallback();
            return;
        }
        AppUpdateOptions options = AppUpdateOptions.newBuilder(updateType).build();
        if (updateType == AppUpdateType.FLEXIBLE) {
            registerInstallStateListener();
        }
        appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                updateResultLauncher,
                options
        );
    }

    private void registerInstallStateListener() {
        if (installStateListener != null) return;
        installStateListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                long total = state.totalBytesToDownload();
                long downloaded = state.bytesDownloaded();
                if (total > 0) {
                    int pct = (int) (100 * downloaded / total);
                    showSnackbar(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_downloading_progress, pct), false);
                } else {
                    showSnackbar(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_downloading), false);
                }
            } else if (state.installStatus() == InstallStatus.DOWNLOADED) {
                unregisterInstallStateListener();
                showRestartSnackbar();
            }
        };
        appUpdateManager.registerListener(installStateListener);
    }

    private void unregisterInstallStateListener() {
        if (installStateListener != null) {
            appUpdateManager.unregisterListener(installStateListener);
            installStateListener = null;
        }
    }

    /** Snackbar: "Update ready! Restart app to apply." with [Restart] -> completeUpdate(). */
    private void showRestartSnackbar() {
        Snackbar snackbar = Snackbar.make(
                snackbarAnchor,
                activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_restart_message),
                Snackbar.LENGTH_INDEFINITE
        );
        snackbar.setAction(activity.getString(com.nuramin.sunsetcoralcalculator.R.string.update_btn_restart), v -> {
            snackbar.dismiss();
            appUpdateManager.completeUpdate();
        });
        snackbar.show();
    }

    private void showSnackbar(CharSequence message, boolean shortDuration) {
        Snackbar.make(snackbarAnchor, message, shortDuration ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG).show();
    }

    private void showToast(CharSequence message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Call from Activity.onResume(): if an update was in progress and was paused, resume it.
     */
    public void onResume() {
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        showRestartSnackbar();
                    }
                    // If update is in progress, Play Core may resume automatically; we keep listener if we had one
                });
    }

    /**
     * Call from Activity.onDestroy() or when no longer needed to unregister the install listener.
     */
    public void onDestroy() {
        unregisterInstallStateListener();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    /** Fallback: open Play Store app page if Play Core API fails. */
    public void openPlayStoreFallback() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL));
            intent.setPackage("com.android.vending");
            activity.startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL));
            activity.startActivity(fallback);
        }
    }
}
