package com.landenlabs.widtime.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChargeLogicTest {

    private static final long MIN = 60_000L;

    @Test
    public void pingScheduleMatchesSpec() {
        // Times (minutes after plug-in) at which pings 1..13 play.
        long t = 0;
        long[] expected = {2, 4, 6, 8, 10, 15, 20, 25, 30, 35, 45, 55, 65};
        for (int played = 0; played < expected.length; played++) {
            t += ChargeLogic.pingDelayMillis(played);
            assertEquals("ping " + (played + 1), expected[played] * MIN, t);
        }
    }

    @Test
    public void alarmTiming() {
        assertEquals(5_000L, ChargeLogic.ALARM_PLAY_MILLIS);
        assertEquals(5 * MIN, ChargeLogic.ALARM_PAUSE_MILLIS);
    }

    @Test
    public void currentUnits() {
        assertEquals(1850.0, ChargeLogic.normalizeCurrentMilliAmps(1_850_000), 1e-9); // uA
        assertEquals(1850.0, ChargeLogic.normalizeCurrentMilliAmps(-1_850_000), 1e-9); // negative sign convention
        assertEquals(1850.0, ChargeLogic.normalizeCurrentMilliAmps(1850), 1e-9); // device reports mA
        assertTrue(Double.isNaN(ChargeLogic.normalizeCurrentMilliAmps(0)));
        assertTrue(Double.isNaN(ChargeLogic.normalizeCurrentMilliAmps(Integer.MIN_VALUE)));
    }

    @Test
    public void voltageUnits() {
        assertEquals(4.2, ChargeLogic.normalizeVolts(4200), 1e-9);
        assertEquals(4.2, ChargeLogic.normalizeVolts(4_200_000), 1e-9);
        assertEquals(4.0, ChargeLogic.normalizeVolts(4), 1e-9);
        assertTrue(Double.isNaN(ChargeLogic.normalizeVolts(-1)));
    }

    @Test
    public void speedLabels() {
        assertEquals(2.5, ChargeLogic.maxChargingWatts(500_000, 0), 1e-9); // 0.5 A at default 5 V
        assertEquals(ChargeLogic.Speed.SLOW, ChargeLogic.speedForWatts(2.5));
        assertEquals(ChargeLogic.Speed.NORMAL, ChargeLogic.speedForWatts(5));
        assertEquals(ChargeLogic.Speed.FAST, ChargeLogic.speedForWatts(18)); // 9 V 2 A
        assertEquals(ChargeLogic.Speed.SUPER_FAST, ChargeLogic.speedForWatts(25));
        assertNull(ChargeLogic.speedForWatts(Double.NaN));
        assertTrue(Double.isNaN(ChargeLogic.maxChargingWatts(-1, -1)));
    }

    @Test
    public void usbDataLabel() {
        assertNull(ChargeLogic.usbDataLabel(false, true, java.util.Arrays.asList("adb")));
        assertEquals("Power only · no data", ChargeLogic.usbDataLabel(true, false, null));
        assertEquals("Power + data", ChargeLogic.usbDataLabel(true, true, java.util.Collections.emptyList()));
        assertEquals("Power + data · ADB, MTP",
                ChargeLogic.usbDataLabel(true, true, java.util.Arrays.asList("adb", "mtp")));
    }

    /** Feeds a charge's level readings through the rule; returns the announced levels. */
    private static java.util.List<Integer> spoken(int step, int... levels) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        int mark = -1;
        for (int level : levels) {
            if (ChargeLogic.shouldSpeak(mark, level, step)) {
                out.add(level);
            }
            mark = ChargeLogic.nextSpeakMark(mark, level, step);
        }
        return out;
    }

    @Test
    public void speakCrossingStep() {
        assertEquals(java.util.Arrays.asList(10), spoken(10, 9, 10));
    }

    @Test
    public void speakBigJumpOnceAtActualLevel() {
        assertEquals(java.util.Arrays.asList(22), spoken(10, 9, 22));
    }

    @Test
    public void speakSameStepIsQuietThenNextStep() {
        assertEquals(java.util.Arrays.asList(30), spoken(10, 22, 25, 29, 30));
    }

    @Test
    public void speakFirstReadingOnlyArms() {
        assertEquals(java.util.Arrays.asList(50), spoken(10, 47, 48, 49, 50));
    }

    @Test
    public void speakDipDoesNotRepeat() {
        assertEquals(java.util.Arrays.asList(50, 60), spoken(10, 49, 50, 49, 48, 50, 59, 60));
    }

    @Test
    public void speakFullAndOtherSteps() {
        assertEquals(java.util.Arrays.asList(100), spoken(10, 98, 99, 100));
        assertEquals(java.util.Arrays.asList(25, 50), spoken(25, 24, 25, 49, 50));
        assertEquals(java.util.Arrays.asList(5, 10), spoken(5, 4, 5, 9, 10));
    }

    @Test
    public void speakOffNeverSpeaks() {
        assertTrue(spoken(0, 9, 10, 50, 100).isEmpty());
    }
}
