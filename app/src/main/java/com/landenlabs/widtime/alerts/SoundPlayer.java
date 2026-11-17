// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.alerts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.landenlabs.widtime.util.ChargeLogic;

/** Plays the short charging ping and the 5-second unplug alarm, keeping the CPU awake meanwhile. */
final class SoundPlayer {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int PING_MILLIS = 300;
    private static final int LOW_BATTERY_MILLIS = 1_000;
    /** Off / on pairs in ms: three 400 ms buzzes, 1.6 s in total. */
    private static final long[] LOW_BATTERY_VIBRATE_PATTERN = {0, 400, 200, 400, 200, 400};

    private static MediaPlayer alarmPlayer;
    private static ToneGenerator alarmTone;
    private static final Runnable STOP_ALARM = SoundPlayer::stopAlarm;

    private SoundPlayer() {
    }

    /** Short double "ping" on the notification stream, so it follows the notification volume. */
    static void playPing(Context context) {
        wakeFor(context, PING_MILLIS + 1000);
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90);
            tone.startTone(ToneGenerator.TONE_PROP_ACK, PING_MILLIS);
            MAIN.postDelayed(tone::release, PING_MILLIS + 200);
        } catch (RuntimeException ignored) {
            // Audio unavailable right now (e.g. during a call); skip this ping.
        }
    }

    /**
     * "Battery low", following the phone's ringer mode: low double beeps on the notification
     * stream (at the notification volume) when the ringer is on, three long buzzes on vibrate,
     * and nothing on silent.
     */
    static void playLowBattery(Context context) {
        wakeFor(context, LOW_BATTERY_MILLIS + 1000);
        AudioManager audio = context.getSystemService(AudioManager.class);
        int mode = audio != null ? audio.getRingerMode() : AudioManager.RINGER_MODE_NORMAL;
        if (mode == AudioManager.RINGER_MODE_VIBRATE) {
            vibrate(context, LOW_BATTERY_VIBRATE_PATTERN);
            return;
        }
        if (mode == AudioManager.RINGER_MODE_SILENT) {
            return;
        }
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            tone.startTone(ToneGenerator.TONE_PROP_NACK, LOW_BATTERY_MILLIS);
            MAIN.postDelayed(tone::release, LOW_BATTERY_MILLIS + 200);
        } catch (RuntimeException ignored) {
            // Audio unavailable right now (e.g. during a call); skip it.
        }
    }

    private static void vibrate(Context context, long[] pattern) {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = context.getSystemService(VibratorManager.class);
            vibrator = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            vibrator = context.getSystemService(Vibrator.class);
        }
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        // Notification usage, so the system's vibrate-on-vibrate-mode rules apply to it.
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1),
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
    }

    /** The user's default alarm sound for {@link ChargeLogic#ALARM_PLAY_MILLIS}, on the alarm stream. */
    static void playAlarm(Context context) {
        playAlarm(context, ChargeLogic.ALARM_PLAY_MILLIS);
    }

    /** The same alarm sound as {@link #playAlarm(Context)}, cut to {@code millis} (settings preview). */
    static synchronized void playAlarm(Context context, long millis) {
        stopAlarm();
        wakeFor(context, millis + 1000);
        Context app = context.getApplicationContext();
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        try {
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(app, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
            alarmPlayer = player;
        } catch (Exception e) {
            // No alarm sound configured or it can't be read: fall back to a generated tone.
            try {
                alarmTone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                alarmTone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, (int) millis);
            } catch (RuntimeException ignored) {
                alarmTone = null;
            }
        }
        MAIN.postDelayed(STOP_ALARM, millis);
    }

    static synchronized void stopAlarm() {
        MAIN.removeCallbacks(STOP_ALARM);
        if (alarmPlayer != null) {
            try {
                alarmPlayer.stop();
            } catch (IllegalStateException ignored) {
                // Already stopped.
            }
            alarmPlayer.release();
            alarmPlayer = null;
        }
        if (alarmTone != null) {
            alarmTone.stopTone();
            alarmTone.release();
            alarmTone = null;
        }
    }

    private static void wakeFor(Context context, long millis) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "widtime:charge-alert").acquire(millis);
        }
    }
}
