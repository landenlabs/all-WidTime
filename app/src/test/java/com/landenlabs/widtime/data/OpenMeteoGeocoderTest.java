package com.landenlabs.widtime.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.landenlabs.widtime.model.City;

import org.junit.Test;

import java.util.List;

public class OpenMeteoGeocoderTest {

    /** Trimmed from a real response for name=Salem. */
    private static final String SALEM_JSON = "{\"results\":["
            + "{\"name\":\"Salem\",\"latitude\":44.9429,\"longitude\":-123.0351,\"country_code\":\"US\","
            + "\"timezone\":\"America/Los_Angeles\",\"country\":\"United States\",\"admin1\":\"Oregon\"},"
            + "{\"name\":\"Salem\",\"latitude\":42.51977,\"longitude\":-70.89552,\"country_code\":\"US\","
            + "\"timezone\":\"America/New_York\",\"country\":\"United States\",\"admin1\":\"Massachusetts\"},"
            + "{\"name\":\"Salem\",\"latitude\":42.78842,\"longitude\":-71.20089,\"country_code\":\"US\","
            + "\"timezone\":\"America/New_York\",\"country\":\"United States\",\"admin1\":\"New Hampshire\"},"
            + "{\"name\":\"Nowhere\",\"latitude\":1,\"longitude\":2,\"country_code\":\"XX\"},"
            + "{\"name\":\"Bad\",\"latitude\":1,\"longitude\":2,\"timezone\":\"Not/AZone\"}"
            + "],\"generationtime_ms\":1.2}";

    @Test
    public void parse_readsFieldsAndSkipsMissingOrBadZones() throws Exception {
        List<City> all = OpenMeteoGeocoder.parse(SALEM_JSON, CityQuery.parse("Salem"));
        assertEquals(3, all.size());
        City oregon = all.get(0);
        assertEquals("Salem", oregon.name);
        assertEquals("Oregon", oregon.admin1);
        assertEquals("United States", oregon.country);
        assertEquals("America/Los_Angeles", oregon.zoneId);
        assertEquals(44.9429, oregon.latitude, 1e-9);
        assertEquals(-123.0351, oregon.longitude, 1e-9);
        assertEquals("Salem, Oregon, United States", oregon.getDisplayName());
    }

    @Test
    public void parse_appliesStateFilter() throws Exception {
        List<City> nh = OpenMeteoGeocoder.parse(SALEM_JSON, CityQuery.parse("Salem, NH"));
        assertEquals(1, nh.size());
        assertEquals("New Hampshire", nh.get(0).admin1);
        assertEquals(42.78842, nh.get(0).latitude, 1e-9);
    }

    @Test
    public void parse_noResultsKey_isEmpty() throws Exception {
        assertTrue(OpenMeteoGeocoder.parse("{\"generationtime_ms\":0.5}", CityQuery.parse("zzzz")).isEmpty());
    }

    @Test
    public void buildUrl_sendsOnlyNameAndUsesMaxCountWithQualifier() {
        String url = OpenMeteoGeocoder.buildUrl(CityQuery.parse("San José, CR"), "es");
        assertEquals("https://geocoding-api.open-meteo.com/v1/search?name=San+Jos%C3%A9"
                + "&count=100&language=es&format=json", url);
        assertTrue(OpenMeteoGeocoder.buildUrl(CityQuery.parse("Boston"), "").contains("&count=20&language=en&"));
    }

    @Test
    public void displayName_bundledCityHasNoRegion() {
        assertEquals("Tokyo, Japan", new City("Tokyo", "Japan", "Asia/Tokyo").getDisplayName());
    }
}
