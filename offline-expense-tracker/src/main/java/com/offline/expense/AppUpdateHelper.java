package com.offline.expense;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.IntentSender;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

/**
 * Wraps Play Core's in-app update API (flexible flow): checks Play Store for a newer version on
 * launch, downloads it in the background with the user's one-time consent, then prompts to
 * restart once the download finishes. Without this, Play Store updates the app silently with no
 * in-app prompt at all.
 */
final class AppUpdateHelper {
    private static final int REQUEST_CODE = 4001;

    private final Activity activity;
    private final AppUpdateManager appUpdateManager;
    private final InstallStateUpdatedListener installStateListener = state -> {
        if (state.installStatus() == InstallStatus.DOWNLOADED) promptRestart();
    };

    AppUpdateHelper(Activity activity) {
        this.activity = activity;
        this.appUpdateManager = AppUpdateManagerFactory.create(activity);
        appUpdateManager.registerListener(installStateListener);
    }

    void checkForUpdate() {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(info -> {
            boolean flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE);
            if (!flexibleAllowed) return;
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    || info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startFlexibleUpdate(info);
            }
        });
    }

    /** Call from onResume: if a download already finished while the app was backgrounded, re-prompt. */
    void resumeIfDownloaded() {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(info -> {
            if (info.installStatus() == InstallStatus.DOWNLOADED) promptRestart();
        });
    }

    private void startFlexibleUpdate(AppUpdateInfo info) {
        try {
            appUpdateManager.startUpdateFlowForResult(info, AppUpdateType.FLEXIBLE, activity, REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            // Nothing to recover here — the user just won't be prompted again until next launch.
        }
    }

    private void promptRestart() {
        new AlertDialog.Builder(activity)
                .setTitle("Update ready")
                .setMessage("A new version finished downloading. Restart now to finish installing it?")
                .setPositiveButton("Restart", (dialog, which) -> appUpdateManager.completeUpdate())
                .setNegativeButton("Later", null)
                .show();
    }

    void unregister() {
        appUpdateManager.unregisterListener(installStateListener);
    }
}
