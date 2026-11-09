package com.landenlabs.widtime.data;

import com.landenlabs.widtime.model.City;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Bundled, offline lookup table of major world cities and their IANA time zone ids. */
public final class CityDatabase {

    private static final City[] CITIES = {
            new City("New York", "United States", "America/New_York"),
            new City("Los Angeles", "United States", "America/Los_Angeles"),
            new City("Chicago", "United States", "America/Chicago"),
            new City("Denver", "United States", "America/Denver"),
            new City("Honolulu", "United States", "Pacific/Honolulu"),
            new City("Anchorage", "United States", "America/Anchorage"),
            new City("Toronto", "Canada", "America/Toronto"),
            new City("Vancouver", "Canada", "America/Vancouver"),
            new City("Mexico City", "Mexico", "America/Mexico_City"),
            new City("Bogota", "Colombia", "America/Bogota"),
            new City("Lima", "Peru", "America/Lima"),
            new City("Santiago", "Chile", "America/Santiago"),
            new City("Caracas", "Venezuela", "America/Caracas"),
            new City("Havana", "Cuba", "America/Havana"),
            new City("Sao Paulo", "Brazil", "America/Sao_Paulo"),
            new City("Buenos Aires", "Argentina", "America/Argentina/Buenos_Aires"),
            new City("London", "United Kingdom", "Europe/London"),
            new City("Dublin", "Ireland", "Europe/Dublin"),
            new City("Lisbon", "Portugal", "Europe/Lisbon"),
            new City("Madrid", "Spain", "Europe/Madrid"),
            new City("Paris", "France", "Europe/Paris"),
            new City("Brussels", "Belgium", "Europe/Brussels"),
            new City("Amsterdam", "Netherlands", "Europe/Amsterdam"),
            new City("Berlin", "Germany", "Europe/Berlin"),
            new City("Zurich", "Switzerland", "Europe/Zurich"),
            new City("Vienna", "Austria", "Europe/Vienna"),
            new City("Rome", "Italy", "Europe/Rome"),
            new City("Warsaw", "Poland", "Europe/Warsaw"),
            new City("Stockholm", "Sweden", "Europe/Stockholm"),
            new City("Oslo", "Norway", "Europe/Oslo"),
            new City("Copenhagen", "Denmark", "Europe/Copenhagen"),
            new City("Helsinki", "Finland", "Europe/Helsinki"),
            new City("Reykjavik", "Iceland", "Atlantic/Reykjavik"),
            new City("Athens", "Greece", "Europe/Athens"),
            new City("Kyiv", "Ukraine", "Europe/Kyiv"),
            new City("Moscow", "Russia", "Europe/Moscow"),
            new City("Istanbul", "Turkey", "Europe/Istanbul"),
            new City("Cairo", "Egypt", "Africa/Cairo"),
            new City("Johannesburg", "South Africa", "Africa/Johannesburg"),
            new City("Lagos", "Nigeria", "Africa/Lagos"),
            new City("Nairobi", "Kenya", "Africa/Nairobi"),
            new City("Beirut", "Lebanon", "Asia/Beirut"),
            new City("Tel Aviv", "Israel", "Asia/Tel_Aviv"),
            new City("Baghdad", "Iraq", "Asia/Baghdad"),
            new City("Tehran", "Iran", "Asia/Tehran"),
            new City("Dubai", "United Arab Emirates", "Asia/Dubai"),
            new City("Riyadh", "Saudi Arabia", "Asia/Riyadh"),
            new City("Karachi", "Pakistan", "Asia/Karachi"),
            new City("New Delhi", "India", "Asia/Kolkata"),
            new City("Mumbai", "India", "Asia/Kolkata"),
            new City("Dhaka", "Bangladesh", "Asia/Dhaka"),
            new City("Almaty", "Kazakhstan", "Asia/Almaty"),
            new City("Bangkok", "Thailand", "Asia/Bangkok"),
            new City("Ho Chi Minh City", "Vietnam", "Asia/Ho_Chi_Minh"),
            new City("Jakarta", "Indonesia", "Asia/Jakarta"),
            new City("Singapore", "Singapore", "Asia/Singapore"),
            new City("Kuala Lumpur", "Malaysia", "Asia/Kuala_Lumpur"),
            new City("Manila", "Philippines", "Asia/Manila"),
            new City("Hong Kong", "China", "Asia/Hong_Kong"),
            new City("Shanghai", "China", "Asia/Shanghai"),
            new City("Beijing", "China", "Asia/Shanghai"),
            new City("Taipei", "Taiwan", "Asia/Taipei"),
            new City("Seoul", "South Korea", "Asia/Seoul"),
            new City("Tokyo", "Japan", "Asia/Tokyo"),
            new City("Perth", "Australia", "Australia/Perth"),
            new City("Adelaide", "Australia", "Australia/Adelaide"),
            new City("Sydney", "Australia", "Australia/Sydney"),
            new City("Melbourne", "Australia", "Australia/Melbourne"),
            new City("Brisbane", "Australia", "Australia/Brisbane"),
            new City("Auckland", "New Zealand", "Pacific/Auckland"),
    };

    private CityDatabase() {
    }

    public static List<City> all() {
        return new ArrayList<>(Arrays.asList(CITIES));
    }

    public static List<City> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return all();
        }
        String needle = query.trim().toLowerCase(Locale.US);
        List<City> results = new ArrayList<>();
        for (City city : CITIES) {
            if (city.name.toLowerCase(Locale.US).contains(needle)
                    || city.country.toLowerCase(Locale.US).contains(needle)) {
                results.add(city);
            }
        }
        return results;
    }
}
