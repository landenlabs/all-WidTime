package com.dennislang.widtime.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Wraps {@link DateTimeFormatter} pattern compilation so the am/pm marker's case can be forced.
 * Standard java.time patterns give no way to control that: 'a' always prints whatever case the
 * locale's am/pm strings use. Here, an 'a' in the user's pattern forces the marker to lowercase
 * and an 'A' forces uppercase; both compile to the real 'a' (am-pm-of-day) field. This repurposes
 * 'A', which java.time defines as milli-of-day - a field nobody wants in a clock widget.
 */
public final class DateTimePatterns {

    private DateTimePatterns() {
    }

    public static String format(ZonedDateTime dateTime, String rawPattern, Locale locale) {
        AmPmCase amPmCase = detectAmPmCase(rawPattern);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(normalize(rawPattern), locale);
        String formatted = dateTime.format(formatter);
        if (amPmCase == AmPmCase.NONE) {
            return formatted;
        }
        String marker = dateTime.format(DateTimeFormatter.ofPattern("a", locale));
        if (marker.isEmpty()) {
            return formatted;
        }
        String replacement = amPmCase == AmPmCase.UPPER
                ? marker.toUpperCase(locale)
                : marker.toLowerCase(locale);
        return formatted.replace(marker, replacement);
    }

    /** Throws {@link IllegalArgumentException} the same way {@link #format} would if compiled. */
    public static void validate(String rawPattern, Locale locale) {
        DateTimeFormatter.ofPattern(normalize(rawPattern), locale);
    }

    private enum AmPmCase {
        NONE, UPPER, LOWER
    }

    private static AmPmCase detectAmPmCase(String pattern) {
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean inQuotes = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == 'A') {
                    hasUpper = true;
                } else if (c == 'a') {
                    hasLower = true;
                }
            }
        }
        if (hasUpper) {
            return AmPmCase.UPPER;
        }
        if (hasLower) {
            return AmPmCase.LOWER;
        }
        return AmPmCase.NONE;
    }

    private static String normalize(String pattern) {
        StringBuilder normalized = new StringBuilder(pattern.length());
        boolean inQuotes = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                inQuotes = !inQuotes;
                normalized.append(c);
            } else if (!inQuotes && c == 'A') {
                normalized.append('a');
            } else {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }
}
