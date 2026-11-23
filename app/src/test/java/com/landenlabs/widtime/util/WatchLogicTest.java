// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WatchLogicTest {

    private static final long MIN = 60_000L;

    @Test
    public void chargingReadingGoesStaleAfter15Minutes() {
        assertTrue(WatchLogic.isFresh(true, 0));
        assertTrue(WatchLogic.isFresh(true, 15 * MIN));
        assertFalse(WatchLogic.isFresh(true, 15 * MIN + 1));
    }

    @Test
    public void batteryReadingGoesStaleAfter3Hours() {
        assertTrue(WatchLogic.isFresh(false, 3 * 60 * MIN));
        assertFalse(WatchLogic.isFresh(false, 3 * 60 * MIN + 1));
    }

    @Test
    public void readingFromSlightlyAheadClockIsFresh() {
        assertTrue(WatchLogic.isFresh(true, -2 * MIN));
    }

    @Test
    public void notifiesOnceWhenFullOnCharger() {
        assertTrue(WatchLogic.shouldNotifyFull(false, true, true));
        assertFalse(WatchLogic.shouldNotifyFull(true, true, true));
    }

    @Test
    public void noNoticeBeforeFullOrOffCharger() {
        assertFalse(WatchLogic.shouldNotifyFull(false, true, false));
        assertFalse(WatchLogic.shouldNotifyFull(false, false, false));
    }

    @Test
    public void fullMarksNoticeUsedUntilUnplugged() {
        boolean notified = WatchLogic.nextFullNotified(false, true, false);
        assertFalse(notified);
        notified = WatchLogic.nextFullNotified(notified, true, true);
        assertTrue(notified);
        // Still on the charger, reported again (e.g. heartbeat): stays used.
        notified = WatchLogic.nextFullNotified(notified, true, true);
        assertTrue(notified);
        assertFalse(WatchLogic.shouldNotifyFull(notified, true, true));
        // Off the charger: re-armed for the next charge.
        notified = WatchLogic.nextFullNotified(notified, false, false);
        assertFalse(notified);
        assertTrue(WatchLogic.shouldNotifyFull(notified, true, true));
    }
}
