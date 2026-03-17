package com.nuramin.sunsetcoralcalculator.ai.cloud;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

import com.nuramin.sunsetcoralcalculator.R;

/**
 * Update Manager using Play Core Library.
 * Checks for update availability and shows "New version available" popup with Update Now button.
 * Can be used on app launch or from settings. Does not replace existing UpdateHelper; use as needed.
 */
public final class UpdateManager {

    /**
     * Check for update on app launch. If update available, shows a dialog.
     * Call from Activity (e.g. MainActivity or AISmartActivity).
     */
    public static void checkUpdateOnLaunch(@NonNull Activity activity) {
        if (!NetworkUtil.isConnected(activity)) return;
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);
        Task<AppUpdateInfo> task = appUpdateManager.getAppUpdateInfo();
        task.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                showUpdateDialog(activity, appUpdateManager, appUpdateInfo);
            }
        });
    }

    private static void showUpdateDialog(Activity activity, AppUpdateManager appUpdateManager, AppUpdateInfo appUpdateInfo) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.update_dialog_title)
                .setMessage(activity.getString(R.string.update_dialog_message))
                .setPositiveButton(R.string.update_btn_now, (dialog, which) -> {
                    try {
                        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE, activity, 0);
                    } catch (Exception ignored) { }
                })
                .setNegativeButton(R.string.update_btn_later, null)
                .show();
    }
}
