// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.wear;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/** One reading of the watch battery, from the sticky ACTION_BATTERY_CHANGED broadcast. */
final class BatteryState {

    /** 0-100, or -1 if unknown. */
    final int level;
    /** On the charger (any kind), including when it is full and no longer filling. */
    final boolean charging;
    /** On the charger and at 100% (or reported full). */
    final boolean full;

    BatteryState(int level, boolean charging, boolean full) {
        this.level = level;
        this.charging = charging;
        this.full = full;
    }

    static BatteryState read(Context context) {
        // A null receiver only returns the sticky value, which is allowed from any context.
        Intent intent = context.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return new BatteryState(-1, false, false);
        }
        int raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int level = raw >= 0 && scale > 0 ? Math.round(raw * 100f / scale) : -1;
        boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        boolean full = plugged && (level >= 100 || status == BatteryManager.BATTERY_STATUS_FULL);
        return new BatteryState(level, plugged, full);
    }
}
