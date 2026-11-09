package com.landenlabs.widtime.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the current battery level and estimates hours of battery left.
 *
 * <p>Android has no public "time until empty" API, so each {@link #refresh} call (driven by the
 * widget's per-minute tick) records level drops while unplugged and derives a discharge rate.
 * While charging, the last measured rate is applied to the current level. Before any rate has
 * been measured, an instantaneous estimate from the battery's charge counter and current draw is
 * used if the device reports them.
 */
public final class BatteryStatus {

    private static final String PREFS = "battery_stats";
    private static final String KEY_SAMPLES = "samples";
    private static final String KEY_LAST_RATE = "last_rate_pct_per_hour";

    /** Battery level 0..100, or -1 if unavailable. */
    public final int levelPercent;
    public final boolean plugged;
    /** Estimated hours to empty, or {@code NaN} while still unknown. */
    public final double hoursLeft;

    private BatteryStatus(int levelPercent, boolean plugged, double hoursLeft) {
        this.levelPercent = levelPercent;
        this.plugged = plugged;
        this.hoursLeft = hoursLeft;
    }

    /** Reads the battery, records a discharge sample, and returns the current estimate. */
    public static synchronized BatteryStatus refresh(Context context) {
        Context app = context.getApplicationContext();
        Intent battery = app.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return new BatteryStatus(-1, false, Double.NaN);
        }
        int rawLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (rawLevel < 0 || scale <= 0) {
            return new BatteryStatus(-1, false, Double.NaN);
        }
        int level = Math.round(rawLevel * 100f / scale);
        boolean plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;

        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<long[]> samples = readSamples(prefs);
        float lastRate = prefs.getFloat(KEY_LAST_RATE, Float.NaN);
        SharedPreferences.Editor editor = prefs.edit();

        double rate;
        if (plugged) {
            samples.clear();
            rate = lastRate;
        } else {
            DischargeEstimator.addSample(samples, System.currentTimeMillis(), level);
            rate = DischargeEstimator.ratePercentPerHour(samples);
            if (!Double.isNaN(rate)) {
                editor.putFloat(KEY_LAST_RATE, (float) rate);
            } else if (!Float.isNaN(lastRate)) {
                rate = lastRate;
            }
        }
        editor.putString(KEY_SAMPLES, writeSamples(samples)).apply();

        double hours = DischargeEstimator.hoursLeft(level, rate);
        if (Double.isNaN(hours) && !plugged) {
            hours = instantaneousHoursLeft(app);
        }
        return new BatteryStatus(level, plugged, hours);
    }

    /** Charge remaining divided by current draw; many devices report 0 or MIN_VALUE for these. */
    private static double instantaneousHoursLeft(Context context) {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm == null) {
            return Double.NaN;
        }
        int chargeMicroAh = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        int currentMicroA = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        if (currentMicroA == 0 || currentMicroA == Integer.MIN_VALUE) {
            currentMicroA = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        }
        if (chargeMicroAh <= 0 || chargeMicroAh == Integer.MIN_VALUE
                || currentMicroA == 0 || currentMicroA == Integer.MIN_VALUE) {
            return Double.NaN;
        }
        double hours = chargeMicroAh / (double) Math.abs((long) currentMicroA);
        return hours > 0 && hours < 24 * 14 ? hours : Double.NaN;
    }

    private static List<long[]> readSamples(SharedPreferences prefs) {
        List<long[]> samples = new ArrayList<>();
        String json = prefs.getString(KEY_SAMPLES, null);
        if (json == null) {
            return samples;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONArray pair = array.getJSONArray(i);
                samples.add(new long[]{pair.getLong(0), pair.getLong(1)});
            }
        } catch (JSONException e) {
            samples.clear();
        }
        return samples;
    }

    private static String writeSamples(List<long[]> samples) {
        JSONArray array = new JSONArray();
        for (long[] s : samples) {
            JSONArray pair = new JSONArray();
            pair.put(s[0]);
            pair.put(s[1]);
            array.put(pair);
        }
        return array.toString();
    }
}
