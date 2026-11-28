// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.util;

/** Pure (Android-free) rules for which layout the widget shows. */
public final class WidgetMode {

    /** How far the battery-only bar's fill is blended toward white while charging. */
    public static final float CHARGING_WHITEN = 0.4f;

    private WidgetMode() {
    }

    /** Which large bars fill the widget in place of the clocks; NONE keeps the normal layout. */
    public enum BigBars {
        NONE, BATTERY, TEMPERATURE, BOTH;

        public boolean showBattery() {
            return this == BATTERY || this == BOTH;
        }

        public boolean showTemperature() {
            return this == TEMPERATURE || this == BOTH;
        }
    }

    /**
     * Big-bar mode: no cities shown and no time-left bar, with the level bar and/or the
     * temperature bar turned on and known. One bar fills the widget; two split its height.
     * The time-left and temperature bars are exclusive (temperature wins, see WidgetPrefs).
     */
    public static BigBars bigBars(int visibleCityCount, boolean showBatteryBar, boolean showTimeLeftBar,
                                  boolean showTempBar, int levelPercent, double temperatureC) {
        if (visibleCityCount > 0 || (showTimeLeftBar && !showTempBar)) {
            return BigBars.NONE;
        }
        boolean battery = showBatteryBar && levelPercent >= 0;
        boolean temp = showTempBar && !Double.isNaN(temperatureC);
        if (battery && temp) {
            return BigBars.BOTH;
        }
        if (battery) {
            return BigBars.BATTERY;
        }
        return temp ? BigBars.TEMPERATURE : BigBars.NONE;
    }

    /** Blends {@code argb} toward white by {@code fraction} (0 = unchanged, 1 = white); keeps alpha. */
    public static int blendTowardWhite(int argb, float fraction) {
        float f = Math.max(0f, Math.min(1f, fraction));
        int a = (argb >>> 24) & 0xFF;
        int r = blend((argb >> 16) & 0xFF, f);
        int g = blend((argb >> 8) & 0xFF, f);
        int b = blend(argb & 0xFF, f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blend(int channel, float f) {
        return Math.round(channel + (255 - channel) * f);
    }
}
