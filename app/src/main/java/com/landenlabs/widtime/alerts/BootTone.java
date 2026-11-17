// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.alerts;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

import com.landenlabs.widtime.util.WidgetPrefs;
import com.landenlabs.widtime.widget.TimeWidgetProvider;

/**
 * Optional "phone is ready" chime after a restart.
 *
 * <p>Plays on {@code LOCKED_BOOT_COMPLETED}, which arrives when the lock screen is up (before the
 * first unlock on encrypted phones). At that point the widget settings, kept in credential
 * storage, can't be read yet, so {@link #sync} mirrors "any widget wants the chime" into
 * device-protected storage, which is readable then. The chime is synthesized (no ringtone files,
 * which also live in credential storage) and plays on the notification stream, so it follows the
 * notification volume and stays quiet on silent / vibrate.
 */
public final class BootTone extends BroadcastReceiver {

    private static final String PREFS = "boot_tone";
    private static final String KEY_ENABLED = "enabled";

    private static final int SAMPLE_RATE = 44_100;
    /** C5, E5, G5, C6: a rising major arpeggio. */
    private static final double[] NOTES_HZ = {523.25, 659.25, 783.99, 1046.50};
    private static final int NOTE_STEP_MILLIS = 140;
    private static final int NOTE_RING_MILLIS = 600;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())
                || !mirror(context).getBoolean(KEY_ENABLED, false)) {
            return;
        }
        PendingResult pending = goAsync(); // Keep the process alive until the chime has played.
        long millis = play(context);
        MAIN.postDelayed(pending::finish, millis + 200);
    }

    /** This widget's boot chime switch. */
    public static boolean isEnabled(Context context, int widgetId) {
        return WidgetPrefs.getBootTone(context, widgetId);
    }

    public static void setEnabled(Context context, int widgetId, boolean enabled) {
        WidgetPrefs.setBootTone(context, widgetId, enabled);
        sync(context);
    }

    /** Settings preview. */
    public static void preview(Context context) {
        play(context);
    }

    /**
     * Copies "any widget has the boot chime on" into device-protected storage for the next boot.
     * Called from {@link ChargeAlerts#syncService}, which runs on every widget add / update / delete,
     * app update, and settings change.
     */
    static void sync(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, TimeWidgetProvider.class));
        boolean any = false;
        for (int id : ids) {
            any |= isEnabled(context, id);
        }
        SharedPreferences p = mirror(context);
        if (p.getBoolean(KEY_ENABLED, false) != any) {
            p.edit().putBoolean(KEY_ENABLED, any).apply();
        }
    }

    private static SharedPreferences mirror(Context context) {
        return context.getApplicationContext().createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Starts the chime; returns its length in milliseconds (0 if audio isn't available). */
    private static long play(Context context) {
        short[] pcm = chime();
        long millis = pcm.length * 1000L / SAMPLE_RATE;
        try {
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.length * 2)
                    .build();
            track.write(pcm, 0, pcm.length);
            track.play();
            MAIN.postDelayed(track::release, millis + 100);
            return millis;
        } catch (RuntimeException e) {
            return 0; // Audio unavailable right now (e.g. during a call); skip the chime.
        }
    }

    /** The four notes, each struck in turn and left to ring out with a soft exponential decay. */
    private static short[] chime() {
        int step = SAMPLE_RATE * NOTE_STEP_MILLIS / 1000;
        int ring = SAMPLE_RATE * NOTE_RING_MILLIS / 1000;
        int attack = SAMPLE_RATE / 200; // 5 ms, avoids a click at each note start.
        float[] mix = new float[step * (NOTES_HZ.length - 1) + ring];
        for (int n = 0; n < NOTES_HZ.length; n++) {
            double w = 2 * Math.PI * NOTES_HZ[n] / SAMPLE_RATE;
            int start = n * step;
            for (int i = 0; i < ring; i++) {
                double env = Math.min(1.0, i / (double) attack) * Math.exp(-5.0 * i / ring);
                mix[start + i] += (float) (env * (Math.sin(w * i) + 0.3 * Math.sin(2 * w * i)));
            }
        }
        short[] pcm = new short[mix.length];
        for (int i = 0; i < mix.length; i++) {
            float v = Math.max(-1f, Math.min(1f, mix[i] * 0.35f));
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }
}
