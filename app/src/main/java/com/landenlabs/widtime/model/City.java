package com.landenlabs.widtime.model;

/**
 * A city search result: from the bundled lookup table, or from the online geocoder (which also
 * supplies the state/province and coordinates).
 */
public class City {

    public final String name;
    /** State, province or region; empty when unknown (bundled cities). */
    public final String admin1;
    public final String country;
    public final String zoneId;
    /** Degrees; {@link Double#NaN} when unknown (bundled cities). */
    public final double latitude;
    public final double longitude;

    public City(String name, String country, String zoneId) {
        this(name, "", country, zoneId, Double.NaN, Double.NaN);
    }

    public City(String name, String admin1, String country, String zoneId,
                double latitude, double longitude) {
        this.name = name;
        this.admin1 = admin1 == null ? "" : admin1;
        this.country = country == null ? "" : country;
        this.zoneId = zoneId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** e.g. "Salem, New Hampshire, United States" or "Tokyo, Japan". */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder(name);
        // Skip a region that only repeats the city name, e.g. "Singapore, Singapore".
        if (!admin1.isEmpty() && !admin1.equalsIgnoreCase(name)) {
            sb.append(", ").append(admin1);
        }
        if (!country.isEmpty()) {
            sb.append(", ").append(country);
        }
        return sb.toString();
    }
}
