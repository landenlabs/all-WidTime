package com.landenlabs.widtime.util;

/** Pure (Android-free) rules for which layout the widget shows. */
public final class WidgetMode {

    /** How far the battery-only bar's fill is blended toward white while charging. */
    public static final float CHARGING_WHITEN = 0.4f;

    private WidgetMode() {
    }

    /**
     * Battery-only mode: no cities shown, only the level bar turned on (not the time-left bar),
     * and a known battery level. The widget is then one large battery bar, whether charging or not.
     */
    public static boolean isBatteryOnly(int visibleCityCount, boolean showBatteryBar,
                                        boolean showTimeLeftBar, int levelPercent) {
        return visibleCityCount == 0 && showBatteryBar && !showTimeLeftBar && levelPercent >= 0;
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
