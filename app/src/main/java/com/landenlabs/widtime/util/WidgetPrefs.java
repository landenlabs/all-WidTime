package com.landenlabs.widtime.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.landenlabs.widtime.model.CityEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-widget-instance settings storage: the cities shown (each with its own text color, size,
 * date/time format and visibility), background, battery bars, and charging alert / speech choices.
 */
public final class WidgetPrefs {

    private static final String PREFS_PREFIX = "widget_";
    private static final String KEY_CITIES = "cities";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_SHOW_BATTERY_BAR = "show_battery_bar";
    private static final String KEY_SHOW_TIME_LEFT_BAR = "show_time_left_bar";
    private static final String KEY_CHARGE_ALERTS = "charge_alerts";
    private static final String KEY_SPEAK_STEP = "speak_step";

    // Before per-city styles, one color/size/format applied to the whole widget. These keys are
    // only read now, as the style for cities saved without one.
    private static final String LEGACY_KEY_TEXT_COLOR = "text_color";
    private static final String LEGACY_KEY_TEXT_SIZE = "text_size";
    private static final String LEGACY_KEY_PATTERN = "pattern";

    public static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;
    public static final float DEFAULT_TEXT_SIZE_SP = 16f;
    public static final String DEFAULT_PATTERN = "EEE d MMM, HH:mm";
    public static final int DEFAULT_BACKGROUND_COLOR = 0xCC202020;

    private WidgetPrefs() {
    }

    private static SharedPreferences prefs(Context context, int widgetId) {
        return context.getSharedPreferences(PREFS_PREFIX + widgetId, Context.MODE_PRIVATE);
    }

    /** Style template for new cities when the list is empty, and for cities saved without a style. */
    public static CityEntry defaultStyle(Context context, int widgetId) {
        SharedPreferences p = prefs(context, widgetId);
        return new CityEntry("", "UTC",
                p.getInt(LEGACY_KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR),
                p.getFloat(LEGACY_KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE_SP),
                p.getString(LEGACY_KEY_PATTERN, DEFAULT_PATTERN),
                true);
    }

    public static List<CityEntry> getCities(Context context, int widgetId) {
        List<CityEntry> entries = new ArrayList<>();
        String json = prefs(context, widgetId).getString(KEY_CITIES, null);
        if (json == null) {
            return entries;
        }
        CityEntry fallback = defaultStyle(context, widgetId);
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                entries.add(new CityEntry(
                        obj.getString("label"),
                        obj.getString("zoneId"),
                        obj.optInt("textColor", fallback.textColor),
                        (float) obj.optDouble("textSize", fallback.textSizeSp),
                        obj.optString("pattern", fallback.pattern),
                        obj.optBoolean("visible", true)));
            }
        } catch (JSONException e) {
            // Corrupt prefs; surface an empty list rather than crashing the widget host.
        }
        return entries;
    }

    /** Only the cities drawn on the widget, in order. */
    public static List<CityEntry> getVisibleCities(Context context, int widgetId) {
        List<CityEntry> visible = new ArrayList<>();
        for (CityEntry entry : getCities(context, widgetId)) {
            if (entry.visible) {
                visible.add(entry);
            }
        }
        return visible;
    }

    public static void setCities(Context context, int widgetId, List<CityEntry> entries) {
        JSONArray array = new JSONArray();
        try {
            for (CityEntry entry : entries) {
                JSONObject obj = new JSONObject();
                obj.put("label", entry.label);
                obj.put("zoneId", entry.zoneId);
                obj.put("textColor", entry.textColor);
                obj.put("textSize", entry.textSizeSp);
                obj.put("pattern", entry.pattern);
                obj.put("visible", entry.visible);
                array.put(obj);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs(context, widgetId).edit().putString(KEY_CITIES, array.toString()).apply();
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

    /**
     * Charging pings + unplug alarm for this widget. {@code fallback} is used until this widget
     * saves its own value (widgets created before the setting was per widget).
     */
    public static boolean getChargeAlerts(Context context, int widgetId, boolean fallback) {
        return prefs(context, widgetId).getBoolean(KEY_CHARGE_ALERTS, fallback);
    }

    public static void setChargeAlerts(Context context, int widgetId, boolean enabled) {
        prefs(context, widgetId).edit().putBoolean(KEY_CHARGE_ALERTS, enabled).apply();
    }

    /** "Speak battery %" step for this widget (0 = off); {@code fallback} as for getChargeAlerts. */
    public static int getSpeakStep(Context context, int widgetId, int fallback) {
        return prefs(context, widgetId).getInt(KEY_SPEAK_STEP, fallback);
    }

    public static void setSpeakStep(Context context, int widgetId, int step) {
        prefs(context, widgetId).edit().putInt(KEY_SPEAK_STEP, Math.max(step, 0)).apply();
    }

    /** Copy of every stored value for this widget, for {@link #restore} (the settings screen's Undo). */
    public static Map<String, ?> snapshot(Context context, int widgetId) {
        return new HashMap<>(prefs(context, widgetId).getAll());
    }

    /** Replaces this widget's stored values with a {@link #snapshot}. */
    @SuppressWarnings("unchecked")
    public static void restore(Context context, int widgetId, Map<String, ?> snapshot) {
        SharedPreferences.Editor editor = prefs(context, widgetId).edit().clear();
        for (Map.Entry<String, ?> e : snapshot.entrySet()) {
            Object value = e.getValue();
            if (value instanceof String) {
                editor.putString(e.getKey(), (String) value);
            } else if (value instanceof Integer) {
                editor.putInt(e.getKey(), (Integer) value);
            } else if (value instanceof Float) {
                editor.putFloat(e.getKey(), (Float) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(e.getKey(), (Boolean) value);
            } else if (value instanceof Long) {
                editor.putLong(e.getKey(), (Long) value);
            } else if (value instanceof Set) {
                editor.putStringSet(e.getKey(), (Set<String>) value);
            }
        }
        editor.apply();
    }

    public static void delete(Context context, int widgetId) {
        prefs(context, widgetId).edit().clear().apply();
    }
}
