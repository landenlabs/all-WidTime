// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.landenlabs.widtime.R;

/**
 * Offers a Google Play "flexible" in-app update: Play's own dialog asks to update, the download
 * runs in the background, and a Snackbar offers to restart into the new version once it's ready.
 *
 * <p>Only works for installs from Google Play; for sideloaded or debug builds the check quietly
 * fails. A version the user said no to is not offered again; a newer one is.
 *
 * <p>Create it in {@code onCreate} (it registers an activity result launcher) and call
 * {@link #check()} when the screen opens.
 */
final class PlayUpdateChecker implements DefaultLifecycleObserver {

    private static final String PREFS = "play_update";
    private static final String KEY_DECLINED_VERSION = "declinedVersionCode";

    private final AppCompatActivity activity;
    private final AppUpdateManager manager;
    private final ActivityResultLauncher<IntentSenderRequest> updateLauncher;
    private final InstallStateUpdatedListener installListener = state -> {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showRestart();
        }
    };

    /** Version code offered in Play's dialog, remembered if the user declines it. */
    private int offeredVersion;

    PlayUpdateChecker(@NonNull AppCompatActivity activity) {
        this.activity = activity;
        this.manager = AppUpdateManagerFactory.create(activity);
        this.updateLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_CANCELED) {
                        prefs().edit().putInt(KEY_DECLINED_VERSION, offeredVersion).apply();
                    }
                });
        activity.getLifecycle().addObserver(this);
    }

    /** Asks Play whether a newer version exists and, if so, shows Play's update dialog. */
    void check() {
        manager.getAppUpdateInfo().addOnSuccessListener(activity, info -> {
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showRestart();  // Downloaded on an earlier visit but not yet installed.
            } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    && info.availableVersionCode() != prefs().getInt(KEY_DECLINED_VERSION, 0)) {
                startUpdate(info);
            }
        });
        // No failure listener: not installed from Play, no Play Store, or offline. Nothing to do.
    }

    private void startUpdate(AppUpdateInfo info) {
        offeredVersion = info.availableVersionCode();
        manager.registerListener(installListener);
        manager.startUpdateFlowForResult(info, updateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build());
    }

    private void showRestart() {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        Snackbar.make(activity.findViewById(android.R.id.content),
                        R.string.update_downloaded, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.update_restart, v -> manager.completeUpdate())
                .show();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        manager.unregisterListener(installListener);
    }

    private SharedPreferences prefs() {
        return activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
