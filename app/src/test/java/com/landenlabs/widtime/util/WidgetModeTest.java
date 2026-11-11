package com.landenlabs.widtime.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WidgetModeTest {

    @Test
    public void batteryOnlyWhenNoCitiesAndOnlyLevelBar() {
        assertTrue(WidgetMode.isBatteryOnly(0, true, false, 85));
        assertTrue(WidgetMode.isBatteryOnly(0, true, false, 0));
        assertTrue(WidgetMode.isBatteryOnly(0, true, false, 100));
    }

    @Test
    public void notBatteryOnlyWithCities() {
        assertFalse(WidgetMode.isBatteryOnly(1, true, false, 85));
    }

    @Test
    public void notBatteryOnlyWithoutLevelBar() {
        assertFalse(WidgetMode.isBatteryOnly(0, false, false, 85));
    }

    @Test
    public void notBatteryOnlyWithTimeLeftBar() {
        assertFalse(WidgetMode.isBatteryOnly(0, true, true, 85));
    }

    @Test
    public void notBatteryOnlyWhenLevelUnknown() {
        assertFalse(WidgetMode.isBatteryOnly(0, true, false, -1));
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
