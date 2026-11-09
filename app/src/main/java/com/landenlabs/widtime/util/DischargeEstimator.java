package com.landenlabs.widtime.util;

import java.util.List;

/**
 * Pure (Android-free) math for the battery bars: bar colors and the discharge-rate estimate
 * built from (timeMillis, levelPercent) samples recorded while the device is unplugged.
 */
public final class DischargeEstimator {

    public static final int COLOR_GREEN = 0xFF4CAF50;
    public static final int COLOR_YELLOW = 0xFFFFC107;
    public static final int COLOR_RED = 0xFFF44336;

    /** Full length of the time-left bar. */
    public static final double FULL_SCALE_HOURS = 24.0;

    /** Samples older than this are dropped so the rate follows recent usage. */
    public static final long WINDOW_MILLIS = 6L * 60 * 60 * 1000;

    /** Minimum observed span before a measured rate is trusted. */
    static final int MIN_DROP_PERCENT = 2;
    static final long MIN_SPAN_MILLIS = 10L * 60 * 1000;

    private DischargeEstimator() {
    }

    /** Green above 60%, yellow from 40% to 60%, red below 40%. */
    public static int colorForPercent(double percent) {
        if (percent > 60) {
            return COLOR_GREEN;
        }
        if (percent >= 40) {
            return COLOR_YELLOW;
        }
        return COLOR_RED;
    }

    /** Percent (0..100) of the 24 h scale that {@code hoursLeft} fills. */
    public static double percentOfFullScale(double hoursLeft) {
        return clamp(hoursLeft / FULL_SCALE_HOURS * 100.0, 0, 100);
    }

    /**
     * Appends a discharging sample, keeping only level changes, resetting when the level went up
     * (charged while we weren't looking), and pruning samples outside {@link #WINDOW_MILLIS}.
     * Always keeps at least two samples so a slow overnight drain still yields a rate.
     */
    public static void addSample(List<long[]> samples, long timeMillis, int level) {
        if (!samples.isEmpty()) {
            long[] last = samples.get(samples.size() - 1);
            if (level > last[1] || timeMillis < last[0]) {
                samples.clear();
            } else if (level == last[1]) {
                return;
            }
        }
        samples.add(new long[]{timeMillis, level});
        while (samples.size() > 2 && samples.get(0)[0] < timeMillis - WINDOW_MILLIS) {
            samples.remove(0);
        }
    }

    /**
     * Discharge rate in percent per hour from the first to the last sample, or {@code NaN} when
     * there is not yet enough drop/time to trust it.
     */
    public static double ratePercentPerHour(List<long[]> samples) {
        if (samples.size() < 2) {
            return Double.NaN;
        }
        long[] first = samples.get(0);
        long[] last = samples.get(samples.size() - 1);
        long drop = first[1] - last[1];
        long span = last[0] - first[0];
        if (drop < MIN_DROP_PERCENT || span < MIN_SPAN_MILLIS) {
            return Double.NaN;
        }
        return drop / (span / 3_600_000.0);
    }

    /** Hours until empty at {@code ratePercentPerHour}, or {@code NaN} if the rate is unusable. */
    public static double hoursLeft(int level, double ratePercentPerHour) {
        if (Double.isNaN(ratePercentPerHour) || ratePercentPerHour <= 0) {
            return Double.NaN;
        }
        return level / ratePercentPerHour;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
