package com.landenlabs.widtime.util;

/** Pure (Android-free) rules for charging pings, the unplug alarm, and charge-speed labels. */
public final class ChargeLogic {

    private static final long MINUTE = 60_000L;

    /** Pings 1-5 come every 2 min, pings 6-10 every 5 min, then every 10 min while charging. */
    static final int FAST_PING_COUNT = 5;
    static final int MID_PING_COUNT = 5;
    static final long FAST_PING_INTERVAL = 2 * MINUTE;
    static final long MID_PING_INTERVAL = 5 * MINUTE;
    static final long SLOW_PING_INTERVAL = 10 * MINUTE;

    /** Unplug alarm: sound for 5 s, then 5 min of silence, repeated until stopped. */
    public static final long ALARM_PLAY_MILLIS = 5_000L;
    public static final long ALARM_PAUSE_MILLIS = 5 * MINUTE;

    public enum Speed {
        SLOW("Slow"), NORMAL("Normal"), FAST("Fast"), SUPER_FAST("Super fast");

        public final String label;

        Speed(String label) {
            this.label = label;
        }
    }

    private ChargeLogic() {
    }

    /** Delay before the next ping, given how many pings this charging session has already played. */
    public static long pingDelayMillis(int pingsPlayed) {
        if (pingsPlayed < FAST_PING_COUNT) {
            return FAST_PING_INTERVAL;
        }
        if (pingsPlayed < FAST_PING_COUNT + MID_PING_COUNT) {
            return MID_PING_INTERVAL;
        }
        return SLOW_PING_INTERVAL;
    }

    /**
     * BatteryManager current properties should be in microamps, but some devices (notably older
     * Samsung) report milliamps. Values under 10,000 are taken as mA (a phone never draws 10 A).
     * Returns the absolute current in mA, or NaN when unsupported.
     */
    public static double normalizeCurrentMilliAmps(int raw) {
        if (raw == 0 || raw == Integer.MIN_VALUE) {
            return Double.NaN;
        }
        long abs = Math.abs((long) raw);
        return abs >= 10_000 ? abs / 1000.0 : abs;
    }

    /** Battery voltage extra should be mV; normalize V or uV reports to volts. NaN if unknown. */
    public static double normalizeVolts(int raw) {
        if (raw <= 0) {
            return Double.NaN;
        }
        if (raw < 100) {
            return raw;
        }
        if (raw > 100_000) {
            return raw / 1_000_000.0;
        }
        return raw / 1000.0;
    }

    /**
     * Charger capability from the battery intent's max_charging_current (uA) and
     * max_charging_voltage (uV), as AOSP SystemUI computes it. Voltage defaults to 5 V.
     */
    public static double maxChargingWatts(int maxMicroAmps, int maxMicroVolts) {
        if (maxMicroAmps <= 0) {
            return Double.NaN;
        }
        double volts = maxMicroVolts > 0 ? maxMicroVolts / 1_000_000.0 : 5.0;
        return maxMicroAmps / 1_000_000.0 * volts;
    }

    /** AOSP thresholds: under 5 W slow, 7.5 W and up fast; 20 W and up labelled super fast. */
    public static Speed speedForWatts(double watts) {
        if (Double.isNaN(watts) || watts <= 0) {
            return null;
        }
        if (watts < 5) {
            return Speed.SLOW;
        }
        if (watts < 7.5) {
            return Speed.NORMAL;
        }
        if (watts < 20) {
            return Speed.FAST;
        }
        return Speed.SUPER_FAST;
    }
}
