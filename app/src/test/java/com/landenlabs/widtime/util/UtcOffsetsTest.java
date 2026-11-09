package com.landenlabs.widtime.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;

public class UtcOffsetsTest {

    private static final Instant JANUARY = Instant.parse("2026-01-15T12:00:00Z");
    private static final Instant JULY = Instant.parse("2026-07-15T12:00:00Z");

    @Test
    public void wholeHours() {
        assertEquals("UTC+0", UtcOffsets.label(ZoneOffset.UTC));
        assertEquals("UTC+9", UtcOffsets.label(ZoneOffset.ofHours(9)));
        assertEquals("UTC−5", UtcOffsets.label(ZoneOffset.ofHours(-5)));
    }

    @Test
    public void partialHours() {
        assertEquals("UTC+5:30", UtcOffsets.label(ZoneOffset.ofHoursMinutes(5, 30)));
        assertEquals("UTC+5:45", UtcOffsets.label(ZoneOffset.ofHoursMinutes(5, 45)));
        assertEquals("UTC−3:30", UtcOffsets.label(ZoneOffset.ofHoursMinutes(-3, -30)));
        assertEquals("UTC+0:05", UtcOffsets.label(ZoneOffset.ofHoursMinutes(0, 5)));
    }

    @Test
    public void followsDaylightSaving() {
        assertEquals("UTC−5", UtcOffsets.label("America/New_York", JANUARY));
        assertEquals("UTC−4", UtcOffsets.label("America/New_York", JULY));
    }

    @Test
    public void fixedOffsetIdsAndInvalidZones() {
        assertEquals("UTC+5:30", UtcOffsets.label("+05:30", JANUARY));
        assertEquals("UTC+0", UtcOffsets.label("Z", JANUARY));
        assertEquals("", UtcOffsets.label("Not/AZone", JANUARY));
    }
}
