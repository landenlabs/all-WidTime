package com.landenlabs.widtime.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.landenlabs.widtime.model.City;

import org.junit.Test;

public class CityQueryTest {

    private static City city(String admin1, String country) {
        return new City("Salem", admin1, country, "America/New_York", 0, 0);
    }

    @Test
    public void parse_splitsOnFirstComma() {
        CityQuery q = CityQuery.parse("  Salem ,  NH ");
        assertEquals("Salem", q.name);
        assertEquals("NH", q.qualifier);
        assertTrue(q.hasQualifier());
    }

    @Test
    public void parse_noComma() {
        CityQuery q = CityQuery.parse("Boston");
        assertEquals("Boston", q.name);
        assertFalse(q.hasQualifier());
    }

    @Test
    public void parse_null() {
        assertEquals("", CityQuery.parse(null).name);
    }

    @Test
    public void noQualifier_matchesEverything() {
        assertTrue(CityQuery.parse("Salem").matches(city("Oregon", "United States"), "US"));
    }

    @Test
    public void stateAbbreviation_matchesExactly() {
        CityQuery q = CityQuery.parse("Salem, NH");
        assertTrue(q.matches(city("New Hampshire", "United States"), "US"));
        assertFalse(q.matches(city("Massachusetts", "United States"), "US"));
    }

    @Test
    public void abbreviation_doesNotPrefixMatch() {
        // "MA" is Massachusetts, not a prefix of Maine.
        CityQuery q = CityQuery.parse("Salem, ma");
        assertTrue(q.matches(city("Massachusetts", "United States"), "US"));
        assertFalse(q.matches(city("Maine", "United States"), "US"));
    }

    @Test
    public void sharedAbbreviation_matchesBothRegions() {
        CityQuery q = CityQuery.parse("Perth, WA");
        assertTrue(q.matches(city("Western Australia", "Australia"), "AU"));
        assertTrue(q.matches(city("Washington", "United States"), "US"));
    }

    @Test
    public void fullOrPartialRegionName_matches() {
        assertTrue(CityQuery.parse("Salem, new ham").matches(city("New Hampshire", "United States"), "US"));
        assertTrue(CityQuery.parse("Quebec, Québec").matches(city("Quebec", "Canada"), "CA"));
    }

    @Test
    public void countryNameOrCode_matches() {
        assertTrue(CityQuery.parse("Salem, India").matches(city("Tamil Nadu", "India"), "IN"));
        assertTrue(CityQuery.parse("Salem, in").matches(city("Tamil Nadu", "India"), "IN"));
        assertFalse(CityQuery.parse("Salem, India").matches(city("Oregon", "United States"), "US"));
    }
}
