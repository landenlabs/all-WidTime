package com.landenlabs.widtime.model;

/**
 * A city configured on a specific widget instance, with its own text style. {@code zoneId} is any
 * string accepted by {@link java.time.ZoneId#of(String)} - either an IANA zone id (e.g.
 * "Asia/Tokyo") for a looked-up city, or a fixed offset id (e.g. "+05:30") for a manually
 * entered location. Hidden entries ({@code visible == false}) stay in the list but are not drawn
 * on the widget.
 */
public class CityEntry {

    public String label;
    public String zoneId;
    public int textColor;
    public float textSizeSp;
    public String pattern;
    public boolean visible;

    public CityEntry(String label, String zoneId, int textColor, float textSizeSp, String pattern, boolean visible) {
        this.label = label;
        this.zoneId = zoneId;
        this.textColor = textColor;
        this.textSizeSp = textSizeSp;
        this.pattern = pattern;
        this.visible = visible;
    }

    /** A new visible city that copies the text style of {@code styleFrom}. */
    public static CityEntry withStyleOf(String label, String zoneId, CityEntry styleFrom) {
        return new CityEntry(label, zoneId, styleFrom.textColor, styleFrom.textSizeSp, styleFrom.pattern, true);
    }
}
