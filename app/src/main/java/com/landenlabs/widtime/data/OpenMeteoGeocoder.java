package com.landenlabs.widtime.data;

import com.landenlabs.widtime.model.City;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Online city lookup using the free Open-Meteo Geocoding API (GeoNames data, CC BY 4.0, no API
 * key). One request returns each city's region, country, coordinates and IANA time zone.
 * {@link #search} blocks; call it off the main thread.
 *
 * @see <a href="https://open-meteo.com/en/docs/geocoding-api">Geocoding API docs</a>
 */
public final class OpenMeteoGeocoder {

    private static final String ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search";
    private static final int TIMEOUT_MS = 8000;
    /** Results are ranked by population; without a qualifier the top few are what people want. */
    private static final int COUNT_PLAIN = 20;
    /** "Salem, NH" is filtered here, so fetch the API maximum to reach smaller towns. */
    private static final int COUNT_QUALIFIED = 100;
    /** The API needs at least two characters. */
    public static final int MIN_QUERY_LENGTH = 2;

    private OpenMeteoGeocoder() {
    }

    /**
     * @param text     user text, e.g. "Boston" or "Salem, NH"
     * @param language two-letter language code for place names, e.g. "en"
     * @return matching cities, most populous first; empty if the name is too short
     */
    public static List<City> search(String text, String language) throws IOException {
        CityQuery query = CityQuery.parse(text);
        if (query.name.length() < MIN_QUERY_LENGTH) {
            return new ArrayList<>();
        }
        String json = get(buildUrl(query, language));
        try {
            return parse(json, query);
        } catch (JSONException e) {
            throw new IOException("Unexpected geocoder response", e);
        }
    }

    static String buildUrl(CityQuery query, String language) {
        int count = query.hasQualifier() ? COUNT_QUALIFIED : COUNT_PLAIN;
        String lang = (language == null || language.isEmpty()) ? "en" : language;
        return ENDPOINT
                + "?name=" + encode(query.name)
                + "&count=" + count
                + "&language=" + encode(lang)
                + "&format=json";
    }

    /** URLEncoder.encode(String, Charset) needs API 33; minSdk is 26. */
    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);  // UTF-8 is always supported.
        }
    }

    /** Parses a response body and applies the query's state/country filter. */
    static List<City> parse(String json, CityQuery query) throws JSONException {
        List<City> cities = new ArrayList<>();
        List<String> countryCodes = new ArrayList<>();
        JSONArray results = new JSONObject(json).optJSONArray("results");
        if (results == null) {
            return cities;  // No matches: the API omits "results".
        }
        for (int i = 0; i < results.length(); i++) {
            JSONObject r = results.optJSONObject(i);
            if (r == null) {
                continue;
            }
            String name = r.optString("name", "");
            String zone = r.optString("timezone", "");
            if (name.isEmpty() || !isValidZone(zone)) {
                continue;  // A city without a usable zone can't drive a clock.
            }
            cities.add(new City(name,
                    r.optString("admin1", ""),
                    r.optString("country", r.optString("country_code", "")),
                    zone,
                    r.optDouble("latitude", Double.NaN),
                    r.optDouble("longitude", Double.NaN)));
            countryCodes.add(r.optString("country_code", ""));
        }
        return query.filter(cities, countryCodes);
    }

    private static boolean isValidZone(String zone) {
        if (zone.isEmpty()) {
            return false;
        }
        try {
            ZoneId.of(zone);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static String get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Geocoder HTTP " + status);
            }
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            conn.disconnect();
        }
    }
}
