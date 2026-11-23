// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.watch;

/**
 * What the phone and watch apps exchange over the Wearable Data Layer. The watch app (:wear) has
 * a copy (com.landenlabs.widtime.wear.WatchProtocol); keep the two the same.
 */
final class WatchProtocol {

    /** Data item with the watch's latest battery reading (watch -> phone). */
    static final String PATH_BATTERY = "/widtime/watch_battery";
    /** Message asking the watch to send a reading now (phone -> watch). */
    static final String PATH_REFRESH = "/widtime/watch_refresh";
    /** Declared by the watch app, so the phone can tell whether a watch has it installed. */
    static final String CAPABILITY = "widtime_watch_battery";

    static final String KEY_LEVEL = "level";
    /** On the charger (wired, wireless or dock), whether or not the battery is still filling. */
    static final String KEY_CHARGING = "charging";
    static final String KEY_FULL = "full";
    static final String KEY_TIME = "time";

    private WatchProtocol() {
    }
}
