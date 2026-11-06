package com.dennislang.widtime.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.dennislang.widtime.model.CityEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Per-widget-instance settings storage: cities shown, text color/size/format pattern, background, and battery bars. */
public final class WidgetPrefs {

    private static final String PREFS_PREFIX = "widget_";
    private static final String KEY_CITIES = "cities";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_PATTERN = "pattern";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_SHOW_BATTERY_BAR = "show_battery_bar";
    private static final String KEY_SHOW_TIME_LEFT_BAR = "show_time_left_bar";

    public static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;
    public static final float DEFAULT_TEXT_SIZE_SP = 16f;
    public static final String DEFAULT_PATTERN = "EEE d MMM, HH:mm";
    public static final int DEFAULT_BACKGROUND_COLOR = 0xCC202020;

    private WidgetPrefs() {
    }

    private static SharedPreferences prefs(Context context, int widgetId) {
        return context.getSharedPreferences(PREFS_PREFIX + widgetId, Context.MODE_PRIVATE);
    }

    public static List<CityEntry> getCities(Context context, int widgetId) {
        List<CityEntry> entries = new ArrayList<>();
        String json = prefs(context, widgetId).getString(KEY_CITIES, null);
        if (json == null) {
            return entries;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                entries.add(new CityEntry(obj.getString("label"), obj.getString("zoneId")));
            }
        } catch (JSONException e) {
            // Corrupt prefs; surface an empty list rather than crashing the widget host.
        }
        return entries;
    }

    public static void setCities(Context context, int widgetId, List<CityEntry> entries) {
        JSONArray array = new JSONArray();
        try {
            for (CityEntry entry : entries) {
                JSONObject obj = new JSONObject();
                obj.put("label", entry.label);
                obj.put("zoneId", entry.zoneId);
                array.put(obj);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs(context, widgetId).edit().putString(KEY_CITIES, array.toString()).apply();
    }

    public static int getTextColor(Context context, int widgetId) {
        return prefs(context, widgetId).getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR);
    }

    public static void setTextColor(Context context, int widgetId, int color) {
        prefs(context, widgetId).edit().putInt(KEY_TEXT_COLOR, color).apply();
    }

    public static float getTextSize(Context context, int widgetId) {
        return prefs(context, widgetId).getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE_SP);
    }

    public static void setTextSize(Context context, int widgetId, float sizeSp) {
        prefs(context, widgetId).edit().putFloat(KEY_TEXT_SIZE, sizeSp).apply();
    }

    public static String getPattern(Context context, int widgetId) {
        return prefs(context, widgetId).getString(KEY_PATTERN, DEFAULT_PATTERN);
    }

    public static void setPattern(Context context, int widgetId, String pattern) {
        prefs(context, widgetId).edit().putString(KEY_PATTERN, pattern).apply();
    }

    public static int getBackgroundColor(Context context, int widgetId) {
        return prefs(context, widgetId).getInt(KEY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);
    }

    public static void setBackgroundColor(Context context, int widgetId, int color) {
        prefs(context, widgetId).edit().putInt(KEY_BACKGROUND_COLOR, color).apply();
    }

    public static boolean getShowBatteryBar(Context context, int widgetId) {
        return prefs(context, widgetId).getBoolean(KEY_SHOW_BATTERY_BAR, false);
    }

    public static void setShowBatteryBar(Context context, int widgetId, boolean show) {
        prefs(context, widgetId).edit().putBoolean(KEY_SHOW_BATTERY_BAR, show).apply();
    }

    public static boolean getShowTimeLeftBar(Context context, int widgetId) {
        return prefs(context, widgetId).getBoolean(KEY_SHOW_TIME_LEFT_BAR, false);
    }

    public static void setShowTimeLeftBar(Context context, int widgetId, boolean show) {
        prefs(context, widgetId).edit().putBoolean(KEY_SHOW_TIME_LEFT_BAR, show).apply();
    }

    public static void delete(Context context, int widgetId) {
        prefs(context, widgetId).edit().clear().apply();
    }
}
