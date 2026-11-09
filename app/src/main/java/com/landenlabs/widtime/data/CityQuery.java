package com.landenlabs.widtime.data;

import com.landenlabs.widtime.model.City;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Splits search text such as "Salem, NH" into the city name sent to the geocoder ("Salem") and
 * an optional qualifier ("NH") used to filter the results by state, province or country. The
 * geocoder only matches city names, so "Salem, NH" sent as-is finds nothing useful.
 */
public final class CityQuery {

    /** City name part; never null. */
    public final String name;
    /** Text after the first comma, or empty. */
    public final String qualifier;

    private CityQuery(String name, String qualifier) {
        this.name = name;
        this.qualifier = qualifier;
    }

    public static CityQuery parse(String text) {
        if (text == null) {
            return new CityQuery("", "");
        }
        String trimmed = text.trim();
        int comma = trimmed.indexOf(',');
        if (comma < 0) {
            return new CityQuery(trimmed, "");
        }
        return new CityQuery(trimmed.substring(0, comma).trim(),
                trimmed.substring(comma + 1).trim());
    }

    public boolean hasQualifier() {
        return !qualifier.isEmpty();
    }

    /**
     * True when there is no qualifier, or the qualifier matches the city's region or country.
     * Matching ignores case and accents and accepts prefixes ("New Ham"), US state, Canadian
     * province and Australian state abbreviations ("NH", "BC", "NSW"), and country codes ("US").
     */
    public boolean matches(City city, String countryCode) {
        if (!hasQualifier()) {
            return true;
        }
        String q = fold(qualifier);
        String admin = fold(city.admin1);
        if (countryCode != null && fold(countryCode).equals(q)) {
            return true;
        }
        List<String> regions = REGION_ABBREVIATIONS.get(q);
        if (regions != null) {
            // A known abbreviation is matched exactly, so "MA" does not also match "Maine".
            return regions.contains(admin);
        }
        return admin.startsWith(q) || fold(city.country).startsWith(q);
    }

    /** Results that pass {@link #matches}; {@code countryCodes} is parallel to {@code cities}. */
    public List<City> filter(List<City> cities, List<String> countryCodes) {
        List<City> out = new ArrayList<>();
        for (int i = 0; i < cities.size(); i++) {
            if (matches(cities.get(i), countryCodes.get(i))) {
                out.add(cities.get(i));
            }
        }
        return out;
    }

    /** Lower case, accents removed, so "québec" matches "Quebec". */
    static String fold(String s) {
        if (s == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(s.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    /** Abbreviation to region names; a list because "WA" is Washington and Western Australia. */
    private static final Map<String, List<String>> REGION_ABBREVIATIONS = new HashMap<>();

    private static void region(String abbreviation, String name) {
        REGION_ABBREVIATIONS.computeIfAbsent(fold(abbreviation), k -> new ArrayList<>(2))
                .add(fold(name));
    }

    static {
        // United States
        region("AL", "Alabama");
        region("AK", "Alaska");
        region("AZ", "Arizona");
        region("AR", "Arkansas");
        region("CA", "California");
        region("CO", "Colorado");
        region("CT", "Connecticut");
        region("DE", "Delaware");
        region("DC", "District of Columbia");
        region("FL", "Florida");
        region("GA", "Georgia");
        region("HI", "Hawaii");
        region("ID", "Idaho");
        region("IL", "Illinois");
        region("IN", "Indiana");
        region("IA", "Iowa");
        region("KS", "Kansas");
        region("KY", "Kentucky");
        region("LA", "Louisiana");
        region("ME", "Maine");
        region("MD", "Maryland");
        region("MA", "Massachusetts");
        region("MI", "Michigan");
        region("MN", "Minnesota");
        region("MS", "Mississippi");
        region("MO", "Missouri");
        region("MT", "Montana");
        region("NE", "Nebraska");
        region("NV", "Nevada");
        region("NH", "New Hampshire");
        region("NJ", "New Jersey");
        region("NM", "New Mexico");
        region("NY", "New York");
        region("NC", "North Carolina");
        region("ND", "North Dakota");
        region("OH", "Ohio");
        region("OK", "Oklahoma");
        region("OR", "Oregon");
        region("PA", "Pennsylvania");
        region("PR", "Puerto Rico");
        region("RI", "Rhode Island");
        region("SC", "South Carolina");
        region("SD", "South Dakota");
        region("TN", "Tennessee");
        region("TX", "Texas");
        region("UT", "Utah");
        region("VT", "Vermont");
        region("VA", "Virginia");
        region("WA", "Washington");
        region("WV", "West Virginia");
        region("WI", "Wisconsin");
        region("WY", "Wyoming");
        // Canada
        region("AB", "Alberta");
        region("BC", "British Columbia");
        region("MB", "Manitoba");
        region("NB", "New Brunswick");
        region("NL", "Newfoundland and Labrador");
        region("NS", "Nova Scotia");
        region("NT", "Northwest Territories");
        region("NU", "Nunavut");
        region("ON", "Ontario");
        region("PE", "Prince Edward Island");
        region("QC", "Quebec");
        region("SK", "Saskatchewan");
        region("YT", "Yukon");
        // Australia
        region("ACT", "Australian Capital Territory");
        region("NSW", "New South Wales");
        region("QLD", "Queensland");
        region("SA", "South Australia");
        region("TAS", "Tasmania");
        region("VIC", "Victoria");
        region("WA", "Western Australia");
    }
}
