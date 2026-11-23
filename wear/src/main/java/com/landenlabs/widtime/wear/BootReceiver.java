// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * After a restart or an app update. The jobs are persisted, but alarms are not, and a watch
 * that restarts on its charger has to start the poll itself.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        PendingResult result = goAsync();
        new Thread(() -> {
            try {
                ChargeWatch.check(context, true);
            } finally {
                result.finish();
            }
        }, "watch-boot").start();
    }
}
