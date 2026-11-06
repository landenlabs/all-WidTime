package com.dennislang.widtime.model;

/** A known city entry from the bundled lookup database. */
public class City {

    public final String name;
    public final String country;
    public final String zoneId;

    public City(String name, String country, String zoneId) {
        this.name = name;
        this.country = country;
        this.zoneId = zoneId;
    }

    public String getDisplayName() {
        return name + ", " + country;
    }
}
