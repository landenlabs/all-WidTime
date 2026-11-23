// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.util;

/** Rules for the watch battery reading sent by the watch app (no Android dependencies, for tests). */
public final class WatchLogic {

    /**
     * On the charger the watch reports at least every 10 minutes, so a reading older than this
     * means it lost touch (out of range, or the app was stopped) and "charging" may be wrong.
     */
    public static final long FRESH_CHARGING_MILLIS = 15 * 60_000L;
    /** On battery it reports about every 30 minutes, later if Android holds the job back. */
    public static final long FRESH_BATTERY_MILLIS = 3 * 60 * 60_000L;

    private WatchLogic() {
    }

    /**
     * Whether a reading is recent enough to show. The age comes from the watch's clock, which
     * follows the phone's; a reading slightly "from the future" counts as new.
     */
    public static boolean isFresh(boolean charging, long ageMillis) {
        long age = Math.max(ageMillis, 0);
        return age <= (charging ? FRESH_CHARGING_MILLIS : FRESH_BATTERY_MILLIS);
    }

    /** One "watch fully charged" notice per charge: the first full reading while on the charger. */
    public static boolean shouldNotifyFull(boolean notifiedThisCharge, boolean charging, boolean full) {
        return charging && full && !notifiedThisCharge;
    }

    /**
     * Taking the watch off the charger re-arms the notice. A full reading marks it used even when
     * the notice is off, so turning it on mid-charge doesn't announce a charge that's long done.
     */
    public static boolean nextFullNotified(boolean notifiedThisCharge, boolean charging, boolean full) {
        return charging && (notifiedThisCharge || full);
    }
}
