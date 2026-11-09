package com.landenlabs.widtime.alerts;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Speaks the "Charged to N percent" announcements on the notification stream (so it follows the
 * notification volume and Do Not Disturb). The engine starts lazily; a phrase requested while it
 * is still starting is held and spoken once ready. Falls back to the ping if speech isn't available.
 */
final class Speaker {

    private static final long WAKE_MILLIS = 10_000;

    private static Context app;
    private static TextToSpeech tts;
    private static boolean ready;
    private static String pending;

    private Speaker() {
    }

    static synchronized void speak(Context context, String text) {
        app = context.getApplicationContext();
        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "widtime:speak").acquire(WAKE_MILLIS);
        }
        if (ready) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "widtime-level");
            return;
        }
        pending = text; // Only the newest phrase matters.
        if (tts == null) {
            tts = new TextToSpeech(app, Speaker::onInit);
        }
    }

    private static synchronized void onInit(int status) {
        if (tts == null) {
            return; // Shut down before the engine finished starting.
        }
        boolean ok = status == TextToSpeech.SUCCESS;
        if (ok) {
            tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            int lang = tts.setLanguage(Locale.getDefault());
            ok = lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED;
        }
        String text = pending;
        pending = null;
        if (!ok) {
            // No engine or no voice for this language: ping instead, and retry the engine next time.
            tts.shutdown();
            tts = null;
            if (text != null) {
                SoundPlayer.playPing(app);
            }
            return;
        }
        ready = true;
        if (text != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "widtime-level");
        }
    }

    static synchronized void shutdown() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        ready = false;
        pending = null;
    }
}
