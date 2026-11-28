// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.util;

import java.util.Locale;

/**
 * Pure (Android-free) scale for the battery temperature bar.
 *
 * <p>The bar is empty at {@link #COLD_C} (a phone at room temperature) and full at
 * {@link #CRITICAL_C}, the usual Li-ion maximum operating temperature. Android itself shuts the
 * phone down a little higher, at the battery's {@code config_shutdownBatteryTemperature}
 * (68 °C by default), so a full bar is a warning before that happens.
 */
public final class BatteryTemp {

    /** Empty bar. */
    public static final double COLD_C = 20.0;
    /** Full bar. */
    public static final double CRITICAL_C = 60.0;
    /** Yellow from here: warm (heavy use, fast charging). */
    public static final double WARM_C = 40.0;
    /** Red from here: Li-ion cells should not be charged above 45 °C; many phones stop charging. */
    public static final double HOT_C = 45.0;

    /** Readings outside this range are treated as a missing or broken sensor. */
    private static final double MIN_VALID_C = -40.0;
    private static final double MAX_VALID_C = 150.0;

    private BatteryTemp() {
    }

    /**
     * Celsius from {@code BatteryManager.EXTRA_TEMPERATURE} (tenths of a degree), or {@code NaN}
     * when the extra is missing ({@code Integer.MIN_VALUE}) or out of range.
     */
    public static double fromTenths(int tenths) {
        if (tenths == Integer.MIN_VALUE) {
            return Double.NaN;
        }
        double c = tenths / 10.0;
        return c >= MIN_VALID_C && c <= MAX_VALID_C ? c : Double.NaN;
    }

    /** Percent (0..100) of the bar filled: 0 at or below COLD_C, 100 at or above CRITICAL_C. */
    public static double percentOfScale(double celsius) {
        if (Double.isNaN(celsius)) {
            return 0;
        }
        double pct = (celsius - COLD_C) / (CRITICAL_C - COLD_C) * 100.0;
        return Math.max(0, Math.min(100, pct));
    }

    /** Green below 40 °C, yellow 40–45 °C, red at 45 °C and above (unknown: green, bar is empty). */
    public static int colorFor(double celsius) {
        if (Double.isNaN(celsius) || celsius < WARM_C) {
            return DischargeEstimator.COLOR_GREEN;
        }
        if (celsius < HOT_C) {
            return DischargeEstimator.COLOR_YELLOW;
        }
        return DischargeEstimator.COLOR_RED;
    }

    /** Countries that use Fahrenheit for everyday temperatures. */
    public static boolean usesFahrenheit(Locale locale) {
        switch (locale.getCountry()) {
            case "US":
            case "LR":
            case "MM":
            case "BS":
            case "BZ":
            case "KY":
            case "PW":
                return true;
            default:
                return false;
        }
    }

    /** e.g. "38.5°C" or "101.3°F", one decimal. */
    public static String format(double celsius, boolean fahrenheit) {
        double value = fahrenheit ? celsius * 9.0 / 5.0 + 32.0 : celsius;
        return String.format(Locale.getDefault(), "%.1f°%s", value, fahrenheit ? "F" : "C");
    }
}
