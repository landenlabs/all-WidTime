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
}
