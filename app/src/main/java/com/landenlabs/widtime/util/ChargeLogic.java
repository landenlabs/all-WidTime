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

    /** "Speak battery %" choices; 0 = off (the default). */
    public static final int[] SPEAK_STEPS = {0, 5, 10, 20, 25};

    /**
     * Whether to announce {@code level} while charging. {@code mark} is the highest
     * {@code level / step} reached this charge, or -1 before the first reading (which only sets it,
     * so plugging in at 47% stays quiet until 50%). Drops never speak and never lower the mark.
     */
    public static boolean shouldSpeak(int mark, int level, int step) {
        return step > 0 && mark >= 0 && level >= 0 && level / step > mark;
    }

    /** The mark after seeing {@code level}: raised to its step, never lowered. */
    public static int nextSpeakMark(int mark, int level, int step) {
        if (step <= 0 || level < 0) {
            return mark;
        }
        return Math.max(mark, level / step);
    }

    /** USB gadget functions (UsbManager USB_STATE boolean extras) worth showing, in display order. */
    public static final String[] USB_FUNCTIONS = {"adb", "mtp", "ptp", "rndis", "ncm", "midi", "uvc", "accessory"};

    /**
     * Whether a wired charger also carries USB data. {@code connected} is UsbManager's USB_STATE
     * "connected" extra: true only once a host has enumerated the phone over the data lines, so a
     * charge-only cable or a wall charger reads false. Returns null for wireless / unplugged.
     * e.g. "Power + data · ADB, MTP", "Power + data", "Power only · no data".
     */
    public static String usbDataLabel(boolean wired, boolean connected, java.util.List<String> activeFunctions) {
        if (!wired) {
            return null;
        }
        if (!connected) {
            return "Power only · no data";
        }
        if (activeFunctions == null || activeFunctions.isEmpty()) {
            return "Power + data";
        }
        StringBuilder sb = new StringBuilder("Power + data · ");
        for (int i = 0; i < activeFunctions.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(activeFunctions.get(i).toUpperCase(java.util.Locale.ROOT));
        }
        return sb.toString();
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
