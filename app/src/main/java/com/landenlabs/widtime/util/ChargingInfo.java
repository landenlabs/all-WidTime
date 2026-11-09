package com.landenlabs.widtime.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/** Snapshot of the charger state: plugged, source, level, measured current, and speed label. */
public final class ChargingInfo {

    // Not public API constants, but present in the battery intent on most devices (AOSP uses them).
    private static final String EXTRA_MAX_CHARGING_CURRENT = "max_charging_current";
    private static final String EXTRA_MAX_CHARGING_VOLTAGE = "max_charging_voltage";

    public final boolean plugged;
    public final boolean full;
    /** 0..100, or -1 if unknown. */
    public final int levelPercent;
    /** "AC", "USB", "Wireless", "Dock", or null. */
    public final String source;
    /** Absolute measured battery current in mA, or NaN when the device doesn't report it. */
    public final double currentMilliAmps;
    /** Null when neither charger capability nor measured power is available. */
    public final ChargeLogic.Speed speed;

    private ChargingInfo(boolean plugged, boolean full, int levelPercent, String source,
                         double currentMilliAmps, ChargeLogic.Speed speed) {
        this.plugged = plugged;
        this.full = full;
        this.levelPercent = levelPercent;
        this.source = source;
        this.currentMilliAmps = currentMilliAmps;
        this.speed = speed;
    }

    public static ChargingInfo read(Context context) {
        Context app = context.getApplicationContext();
        Intent battery = app.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return new ChargingInfo(false, false, -1, null, Double.NaN, null);
        }
        int plugType = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean plugged = plugType != 0;
        int rawLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int level = rawLevel >= 0 && scale > 0 ? Math.round(rawLevel * 100f / scale) : -1;
        boolean full = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_FULL;

        double currentMa = Double.NaN;
        BatteryManager bm = (BatteryManager) app.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            currentMa = ChargeLogic.normalizeCurrentMilliAmps(
                    bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
        }

        ChargeLogic.Speed speed = null;
        if (plugged) {
            double watts = ChargeLogic.maxChargingWatts(
                    battery.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, -1),
                    battery.getIntExtra(EXTRA_MAX_CHARGING_VOLTAGE, -1));
            if (Double.isNaN(watts)) {
                double volts = ChargeLogic.normalizeVolts(battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1));
                watts = currentMa / 1000.0 * volts;
            }
            speed = ChargeLogic.speedForWatts(watts);
        }
        return new ChargingInfo(plugged, full, level, sourceLabel(plugType), currentMa, speed);
    }

    private static String sourceLabel(int plugType) {
        switch (plugType) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "AC";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "USB";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return "Wireless";
            case 8: // BATTERY_PLUGGED_DOCK, API 33
                return "Dock";
            default:
                return null;
        }
    }
}
