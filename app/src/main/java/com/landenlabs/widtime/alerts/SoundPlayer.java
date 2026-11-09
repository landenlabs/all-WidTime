package com.landenlabs.widtime.alerts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import com.landenlabs.widtime.util.ChargeLogic;

/** Plays the short charging ping and the 5-second unplug alarm, keeping the CPU awake meanwhile. */
final class SoundPlayer {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int PING_MILLIS = 300;

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

    /** The user's default alarm sound for {@link ChargeLogic#ALARM_PLAY_MILLIS}, on the alarm stream. */
    static synchronized void playAlarm(Context context) {
        stopAlarm();
        wakeFor(context, ChargeLogic.ALARM_PLAY_MILLIS + 1000);
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
                alarmTone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, (int) ChargeLogic.ALARM_PLAY_MILLIS);
            } catch (RuntimeException ignored) {
                alarmTone = null;
            }
        }
        MAIN.postDelayed(STOP_ALARM, ChargeLogic.ALARM_PLAY_MILLIS);
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
