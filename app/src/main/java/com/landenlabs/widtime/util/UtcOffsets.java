package com.landenlabs.widtime.util;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Pure (Android-free) short UTC offset labels such as "UTC−5", "UTC+0" or "UTC+5:30". */
public final class UtcOffsets {

    private static final char MINUS = '−';

    private UtcOffsets() {
    }

    /** Offset of {@code zoneId} at {@code instant} (so it follows daylight saving), or "" if the zone is invalid. */
    public static String label(String zoneId, Instant instant) {
        try {
            return label(ZoneId.of(zoneId).getRules().getOffset(instant));
        } catch (DateTimeException e) {
            return "";
        }
    }

    public static String label(ZoneOffset offset) {
        int total = offset.getTotalSeconds();
        int abs = Math.abs(total);
        int hours = abs / 3600;
        int minutes = (abs % 3600) / 60;
        StringBuilder sb = new StringBuilder("UTC").append(total < 0 ? MINUS : '+').append(hours);
        if (minutes != 0) {
            sb.append(':').append(minutes < 10 ? "0" : "").append(minutes);
        }
        return sb.toString();
    }
}
