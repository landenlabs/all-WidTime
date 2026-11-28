// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

public class BatteryTempTest {

    @Test
    public void fromTenths() {
        assertEquals(31.2, BatteryTemp.fromTenths(312), 1e-9);
        assertEquals(-5.0, BatteryTemp.fromTenths(-50), 1e-9);
        assertTrue(Double.isNaN(BatteryTemp.fromTenths(Integer.MIN_VALUE)));
        assertTrue(Double.isNaN(BatteryTemp.fromTenths(-500)));
        assertTrue(Double.isNaN(BatteryTemp.fromTenths(2000)));
    }

    @Test
    public void percentOfScale() {
        assertEquals(0, BatteryTemp.percentOfScale(10), 1e-9);
        assertEquals(0, BatteryTemp.percentOfScale(BatteryTemp.COLD_C), 1e-9);
        assertEquals(50, BatteryTemp.percentOfScale(40), 1e-9);
        assertEquals(100, BatteryTemp.percentOfScale(BatteryTemp.CRITICAL_C), 1e-9);
        assertEquals(100, BatteryTemp.percentOfScale(70), 1e-9);
        assertEquals(0, BatteryTemp.percentOfScale(Double.NaN), 1e-9);
    }

    @Test
    public void colors() {
        assertEquals(DischargeEstimator.COLOR_GREEN, BatteryTemp.colorFor(30));
        assertEquals(DischargeEstimator.COLOR_GREEN, BatteryTemp.colorFor(39.9));
        assertEquals(DischargeEstimator.COLOR_YELLOW, BatteryTemp.colorFor(40));
        assertEquals(DischargeEstimator.COLOR_YELLOW, BatteryTemp.colorFor(44.9));
        assertEquals(DischargeEstimator.COLOR_RED, BatteryTemp.colorFor(45));
        assertEquals(DischargeEstimator.COLOR_GREEN, BatteryTemp.colorFor(Double.NaN));
    }

    @Test
    public void units() {
        assertTrue(BatteryTemp.usesFahrenheit(Locale.US));
        assertFalse(BatteryTemp.usesFahrenheit(Locale.UK));
        assertFalse(BatteryTemp.usesFahrenheit(Locale.GERMANY));
    }

    @Test
    public void format() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertEquals("38.5°C", BatteryTemp.format(38.5, false));
            assertEquals("101.3°F", BatteryTemp.format(38.5, true));
            assertEquals("32.0°F", BatteryTemp.format(0, true));
        } finally {
            Locale.setDefault(saved);
        }
    }
}
