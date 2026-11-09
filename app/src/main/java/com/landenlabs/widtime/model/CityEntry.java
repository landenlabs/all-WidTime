package com.landenlabs.widtime.model;

/**
 * A city configured on a specific widget instance. {@code zoneId} is any string accepted by
 * {@link java.time.ZoneId#of(String)} - either an IANA zone id (e.g. "Asia/Tokyo") for a looked-up
 * city, or a fixed offset id (e.g. "+05:30") for a manually entered location.
 */
public class CityEntry {

    public String label;
    public String zoneId;

    public CityEntry(String label, String zoneId) {
        this.label = label;
        this.zoneId = zoneId;
    }
}
