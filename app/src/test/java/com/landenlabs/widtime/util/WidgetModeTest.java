// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.landenlabs.widtime.util.WidgetMode.BigBars;

import org.junit.Test;

public class WidgetModeTest {

    private static final double NO_TEMP = Double.NaN;

    @Test
    public void batteryWhenNoCitiesAndOnlyLevelBar() {
        assertEquals(BigBars.BATTERY, WidgetMode.bigBars(0, true, false, false, 85, 30));
        assertEquals(BigBars.BATTERY, WidgetMode.bigBars(0, true, false, false, 0, 30));
        assertEquals(BigBars.BATTERY, WidgetMode.bigBars(0, true, false, false, 100, NO_TEMP));
    }

    @Test
    public void temperatureWhenNoCitiesAndOnlyTempBar() {
        assertEquals(BigBars.TEMPERATURE, WidgetMode.bigBars(0, false, false, true, 85, 30));
        // Unknown level doesn't matter for the temperature bar.
        assertEquals(BigBars.TEMPERATURE, WidgetMode.bigBars(0, false, false, true, -1, 30));
    }

    @Test
    public void bothWhenNoCitiesAndLevelAndTempBars() {
        assertEquals(BigBars.BOTH, WidgetMode.bigBars(0, true, false, true, 85, 30));
    }

    @Test
    public void tempWinsOverTimeLeft() {
        assertEquals(BigBars.BOTH, WidgetMode.bigBars(0, true, true, true, 85, 30));
        assertEquals(BigBars.TEMPERATURE, WidgetMode.bigBars(0, false, true, true, 85, 30));
    }

    @Test
    public void unknownValuesDropTheirBar() {
        assertEquals(BigBars.TEMPERATURE, WidgetMode.bigBars(0, true, false, true, -1, 30));
        assertEquals(BigBars.BATTERY, WidgetMode.bigBars(0, true, false, true, 85, NO_TEMP));
        assertEquals(BigBars.NONE, WidgetMode.bigBars(0, true, false, true, -1, NO_TEMP));
        assertEquals(BigBars.NONE, WidgetMode.bigBars(0, true, false, false, -1, 30));
    }

    @Test
    public void noneWithCities() {
        assertEquals(BigBars.NONE, WidgetMode.bigBars(1, true, false, false, 85, 30));
        assertEquals(BigBars.NONE, WidgetMode.bigBars(1, false, false, true, 85, 30));
        assertEquals(BigBars.NONE, WidgetMode.bigBars(2, true, false, true, 85, 30));
    }

    @Test
    public void noneWithoutAnyBar() {
        assertEquals(BigBars.NONE, WidgetMode.bigBars(0, false, false, false, 85, 30));
    }

    @Test
    public void noneWithTimeLeftBar() {
        assertEquals(BigBars.NONE, WidgetMode.bigBars(0, true, true, false, 85, 30));
        assertEquals(BigBars.NONE, WidgetMode.bigBars(0, false, true, false, 85, 30));
    }

    @Test
    public void bigBarsFlags() {
        assertTrue(BigBars.BOTH.showBattery());
        assertTrue(BigBars.BOTH.showTemperature());
        assertTrue(BigBars.BATTERY.showBattery());
        assertFalse(BigBars.BATTERY.showTemperature());
        assertFalse(BigBars.TEMPERATURE.showBattery());
        assertTrue(BigBars.TEMPERATURE.showTemperature());
        assertFalse(BigBars.NONE.showBattery());
        assertFalse(BigBars.NONE.showTemperature());
    }

    @Test
    public void blendTowardWhite() {
        assertEquals(0xFF4CAF50, WidgetMode.blendTowardWhite(0xFF4CAF50, 0f));
        assertEquals(0xFFFFFFFF, WidgetMode.blendTowardWhite(0xFF4CAF50, 1f));
        // 127.5 rounds to 128 (0x80).
        assertEquals(0xFF808080, WidgetMode.blendTowardWhite(0xFF000000, 0.5f));
        // Alpha is kept.
        assertEquals(0x80FFFFFF, WidgetMode.blendTowardWhite(0x80000000, 1f));
        // 40% of the way from 0x4C (76) to 255 is 147.6 -> 148 (0x94).
        assertEquals(0x94, (WidgetMode.blendTowardWhite(0xFF4CAF50, 0.4f) >> 16) & 0xFF);
    }
}
