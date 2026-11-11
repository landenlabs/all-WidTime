package com.landenlabs.widtime.alerts;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.landenlabs.widtime.R;
import com.landenlabs.widtime.ui.AlarmActivity;
import com.landenlabs.widtime.util.ChargeLogic;
import com.landenlabs.widtime.util.ChargingInfo;
import com.landenlabs.widtime.util.WidgetPrefs;
import com.landenlabs.widtime.widget.TimeWidgetProvider;

/**
 * Charging pings and the unplug alarm. State lives in SharedPreferences so that alarms delivered
 * by AlarmManager behave correctly even if the process was restarted in between.
 *
 * <ul>
 *   <li>Charger connected: a ping every 2 min (x5), then every 5 min (x5), then every 10 min.</li>
 *   <li>Charger disconnected: 5 s of alarm, 5 min pause, repeated until the user taps Stop
 *       (notification, alarm screen, or widget) or the charger is reconnected.</li>
 * </ul>
 */
public final class ChargeAlerts {

    static final String ACTION_PING = "com.landenlabs.widtime.ACTION_CHARGE_PING";
    static final String ACTION_ALARM = "com.landenlabs.widtime.ACTION_UNPLUG_ALARM";
    static final String ACTION_STOP_ALARM = "com.landenlabs.widtime.ACTION_STOP_ALARM";
    static final String ACTION_TOGGLE_MUTE = "com.landenlabs.widtime.ACTION_TOGGLE_MUTE";

    static final String CHANNEL_MONITOR = "charge_monitor";
    static final String CHANNEL_ALARM = "charger_alarm";
    static final int NOTIFICATION_MONITOR = 1;
    private static final int NOTIFICATION_ALARM = 2;

    private static final String PREFS = "charge_alerts";
    /** Legacy app-wide switch; now per widget (WidgetPrefs), and only read as their fallback. */
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_SESSION = "charging_session";
    private static final String KEY_PINGS = "pings_played";
    private static final String KEY_ALARM = "alarm_active";
    /** Bell tapped off on the widget: no pings, speech or unplug alarm until this charge ends. */
    private static final String KEY_MUTED = "session_muted";
    /** Legacy app-wide "Speak battery %" step; now per widget, and only read as their fallback. */
    private static final String KEY_SPEAK_STEP = "speak_step";
    /** Highest level / step announced (or passed) this charge; -1 until the first reading. */
    private static final String KEY_SPEAK_MARK = "speak_mark";
    private static final String KEY_LAST_SPOKEN = "last_spoken_millis";
    /**
     * Set once the 100% ping or "Fully charged" has played (or would have, if muted). After that,
     * pings, speech and the unplug alarm stay quiet until the next charging session starts.
     */
    private static final String KEY_FULL_DONE = "full_alert_done";
    /** A ping this soon after an announcement is silent, so the two don't overlap. */
    private static final long PING_AFTER_SPEECH_GAP_MILLIS = 60_000L;
    /** How long the settings screen plays the alarm sound when the alarm is switched on. */
    private static final long PREVIEW_ALARM_MILLIS = 2_000L;

    private ChargeAlerts() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** This widget's charging pings + unplug alarm switch. */
    public static boolean isEnabled(Context context, int widgetId) {
        return WidgetPrefs.getChargeAlerts(context, widgetId, legacyEnabled(context));
    }

    public static void setEnabled(Context context, int widgetId, boolean enabled) {
        WidgetPrefs.setChargeAlerts(context, widgetId, enabled);
        syncService(context);
    }

    /** This widget's "Speak battery %" step, 0 = off. */
    public static int getSpeakStep(Context context, int widgetId) {
        return WidgetPrefs.getSpeakStep(context, widgetId, legacySpeakStep(context));
    }

    /** 0 turns speech off. Re-arms the mark, so a change mid-charge starts from the current level. */
    public static void setSpeakStep(Context context, int widgetId, int step) {
        WidgetPrefs.setSpeakStep(context, widgetId, step);
        prefs(context).edit().putInt(KEY_SPEAK_MARK, -1).apply();
        syncService(context);
    }

    /** A widget's settings were replaced wholesale (Undo): re-arm speech and resync the monitor. */
    public static void settingsRestored(Context context) {
        prefs(context).edit().putInt(KEY_SPEAK_MARK, -1).apply();
        syncService(context);
    }

    /**
     * The device has one charger and one speaker, so pings and the unplug alarm run once,
     * if any widget has them on.
     */
    static boolean isEnabled(Context context) {
        for (int widgetId : widgetIds(context)) {
            if (isEnabled(context, widgetId)) {
                return true;
            }
        }
        return false;
    }

    /** One voice for the device: the smallest step any widget turned on (ChargeLogic.combinedSpeakStep). */
    static int getSpeakStep(Context context) {
        int[] ids = widgetIds(context);
        int[] steps = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            steps[i] = getSpeakStep(context, ids[i]);
        }
        return ChargeLogic.combinedSpeakStep(steps);
    }

    /** Before these settings were per widget they were app-wide; widgets without their own value use it. */
    private static boolean legacyEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    private static int legacySpeakStep(Context context) {
        return prefs(context).getInt(KEY_SPEAK_STEP, 0);
    }

    /** Settings preview: the unplug alarm's sound, cut short. */
    public static void previewAlarm(Context context) {
        SoundPlayer.playAlarm(context, PREVIEW_ALARM_MILLIS);
    }

    /** Settings preview: a sample announcement at the current battery level, with the real voice. */
    public static void previewSpeech(Context context) {
        int level = ChargingInfo.read(context).levelPercent;
        Speaker.speak(context, context.getString(R.string.speak_test, level >= 0 ? level : 50));
    }

    /** Pings, unplug alarm, or speech on this widget: whether its charging bell has anything to mute. */
    public static boolean hasChargingSounds(Context context, int widgetId) {
        return isEnabled(context, widgetId) || getSpeakStep(context, widgetId) > 0;
    }

    /** The unplug alarm is sounding (device-wide). */
    public static boolean isAlarmActive(Context context) {
        return prefs(context).getBoolean(KEY_ALARM, false);
    }

    /** The unplug alarm is sounding and this widget asked for it, so it shows the alarm panel. */
    public static boolean isAlarmActive(Context context, int widgetId) {
        return isAlarmActive(context) && isEnabled(context, widgetId);
    }

    /** True if the user muted pings, speech and the unplug alarm for the current charging session. */
    public static boolean isSessionMuted(Context context) {
        return prefs(context).getBoolean(KEY_MUTED, false);
    }

    /** Widget bell tapped: flip the mute for this charging session only. */
    static void toggleSessionMuted(Context context) {
        prefs(context).edit().putBoolean(KEY_MUTED, !isSessionMuted(context)).apply();
        TimeWidgetProvider.refreshAll(context);
    }

    /** True once this charge reached 100% and its single "full" ping / announcement is done. */
    public static boolean isFullAlertDone(Context context) {
        return prefs(context).getBoolean(KEY_FULL_DONE, false);
    }

    /** The final 100% notice was given: stop the ping schedule for the rest of this charge. */
    private static void markFullAlertDone(Context context) {
        prefs(context).edit().putBoolean(KEY_FULL_DONE, true).apply();
        cancel(context, ACTION_PING);
    }

    public static PendingIntent toggleMuteIntent(Context context) {
        Intent intent = new Intent(context, ChargeAlertReceiver.class).setAction(ACTION_TOGGLE_MUTE);
        return PendingIntent.getBroadcast(context, 5, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Runs the monitor service while alerts or speech are on and at least one widget exists.
     * Android 12+ refuses foreground-service starts from the background; those callers (widget
     * updates) are covered by starts from the settings screen, boot, and app update.
     */
    public static void syncService(Context context) {
        Intent service = new Intent(context, ChargeMonitorService.class);
        boolean widgets = hasWidgets(context);
        boolean alerts = isEnabled(context) && widgets;
        if (!alerts) {
            cancel(context, ACTION_PING);
            prefs(context).edit().putBoolean(KEY_SESSION, false).apply();
            if (isAlarmActive(context)) {
                stopAlarm(context);
            }
        }
        if (alerts || (widgets && getSpeakStep(context) > 0)) {
            try {
                ContextCompat.startForegroundService(context, service);
            } catch (IllegalStateException ignored) {
                // ForegroundServiceStartNotAllowedException: started from the background.
            }
        } else {
            context.stopService(service);
            prefs(context).edit().putBoolean(KEY_MUTED, false).putBoolean(KEY_FULL_DONE, false)
                    .putInt(KEY_SPEAK_MARK, -1).apply();
        }
    }

    /** Alarms and in-flight sessions don't survive a reboot. */
    public static void onBoot(Context context) {
        prefs(context).edit().putBoolean(KEY_SESSION, false).putBoolean(KEY_ALARM, false)
                .putBoolean(KEY_MUTED, false).putBoolean(KEY_FULL_DONE, false)
                .putInt(KEY_SPEAK_MARK, -1).apply();
    }

    /** Catch up on plug/unplug that happened while the monitor wasn't running. */
    static void reconcile(Context context) {
        if (!isEnabled(context)) {
            return;
        }
        boolean plugged = ChargingInfo.read(context).plugged;
        boolean session = prefs(context).getBoolean(KEY_SESSION, false);
        if (plugged && !session) {
            onPowerConnected(context);
        } else if (!plugged && session) {
            onPowerDisconnected(context);
        }
    }

    static void onPowerConnected(Context context) {
        if (isAlarmActive(context)) {
            stopAlarm(context);
        }
        SharedPreferences p = prefs(context);
        if (!p.getBoolean(KEY_SESSION, false)) {
            // A new charge (speech-only mode never sets KEY_SESSION) re-arms the 100% notice.
            p.edit().putBoolean(KEY_FULL_DONE, false).apply();
        }
        if (isEnabled(context) && !p.getBoolean(KEY_SESSION, false)) {
            p.edit().putBoolean(KEY_SESSION, true).putInt(KEY_PINGS, 0).putBoolean(KEY_MUTED, false).apply();
            schedule(context, ACTION_PING, System.currentTimeMillis() + ChargeLogic.pingDelayMillis(0), false);
        }
        TimeWidgetProvider.refreshAll(context);
    }

    static void onPowerDisconnected(Context context) {
        cancel(context, ACTION_PING);
        boolean muted = isSessionMuted(context);
        boolean fullDone = isFullAlertDone(context);
        // The mute and the 100% notice only cover the charge that just ended.
        prefs(context).edit().putBoolean(KEY_SESSION, false).putBoolean(KEY_MUTED, false)
                .putBoolean(KEY_FULL_DONE, false).apply();
        if (isEnabled(context) && !muted && !fullDone) {
            prefs(context).edit().putBoolean(KEY_ALARM, true).apply();
            SoundPlayer.playAlarm(context);
            scheduleNextAlarm(context);
            showAlarmNotification(context);
        }
        TimeWidgetProvider.refreshAll(context);
    }

    static void onPingDue(Context context) {
        SharedPreferences p = prefs(context);
        if (!isEnabled(context) || !p.getBoolean(KEY_SESSION, false)) {
            return;
        }
        ChargingInfo info = ChargingInfo.read(context);
        if (!info.plugged) {
            // Missed the disconnect broadcast (monitor wasn't running).
            onPowerDisconnected(context);
            return;
        }
        if (isFullAlertDone(context)) {
            return; // Already said "full" this charge; no more pings until the next one.
        }
        long sinceSpeech = System.currentTimeMillis() - p.getLong(KEY_LAST_SPOKEN, 0);
        if (!isSessionMuted(context) && sinceSpeech >= PING_AFTER_SPEECH_GAP_MILLIS) {
            SoundPlayer.playPing(context); // Skipped pings keep their schedule, so unmuting resumes it.
        }
        if (ChargeLogic.isFull(info.levelPercent)) {
            markFullAlertDone(context); // That was the last ping of this charge.
            TimeWidgetProvider.refreshAll(context);
            return;
        }
        int played = p.getInt(KEY_PINGS, 0) + 1;
        p.edit().putInt(KEY_PINGS, played).apply();
        schedule(context, ACTION_PING, System.currentTimeMillis() + ChargeLogic.pingDelayMillis(played), false);
        TimeWidgetProvider.refreshAll(context);
    }

    /**
     * Every battery level / plug change (from the monitor service). While charging, says
     * "Charged to N percent" each time the level reaches a new step; see ChargeLogic.shouldSpeak.
     */
    static void onBatteryLevel(Context context, int levelPercent, boolean plugged) {
        int step = getSpeakStep(context);
        SharedPreferences p = prefs(context);
        int mark = p.getInt(KEY_SPEAK_MARK, -1);
        if (step <= 0 || !plugged) {
            if (mark != -1) {
                p.edit().putInt(KEY_SPEAK_MARK, -1).apply(); // Next charge starts fresh.
            }
            return;
        }
        boolean speak = ChargeLogic.shouldSpeak(mark, levelPercent, step);
        int next = ChargeLogic.nextSpeakMark(mark, levelPercent, step);
        if (next != mark) {
            p.edit().putInt(KEY_SPEAK_MARK, next).apply();
        }
        if (!speak || isFullAlertDone(context)) {
            return; // After the 100% notice, stay quiet until the next charge.
        }
        // Muted steps still raise the mark, so unmuting doesn't announce a stale level.
        boolean full = ChargeLogic.isFull(levelPercent);
        if (!isSessionMuted(context)) {
            p.edit().putLong(KEY_LAST_SPOKEN, System.currentTimeMillis()).apply();
            Speaker.speak(context, full
                    ? context.getString(R.string.speak_full)
                    : context.getString(R.string.speak_charged, levelPercent));
        }
        if (full) {
            markFullAlertDone(context); // "Fully charged" replaces any further pings.
        }
    }

    static void onAlarmDue(Context context) {
        if (!isAlarmActive(context)) {
            return;
        }
        if (ChargingInfo.read(context).plugged) {
            stopAlarm(context);
            return;
        }
        SoundPlayer.playAlarm(context);
        scheduleNextAlarm(context);
    }

    /** User pressed Stop (or the charger came back): silence now and cancel the repeats. */
    public static void stopAlarm(Context context) {
        prefs(context).edit().putBoolean(KEY_ALARM, false).apply();
        cancel(context, ACTION_ALARM);
        SoundPlayer.stopAlarm();
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ALARM);
        TimeWidgetProvider.refreshAll(context);
    }

    private static void scheduleNextAlarm(Context context) {
        long at = System.currentTimeMillis() + ChargeLogic.ALARM_PLAY_MILLIS + ChargeLogic.ALARM_PAUSE_MILLIS;
        schedule(context, ACTION_ALARM, at, true);
    }

    static void createChannels(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        NotificationChannel monitor = new NotificationChannel(CHANNEL_MONITOR,
                context.getString(R.string.channel_monitor), NotificationManager.IMPORTANCE_MIN);
        monitor.setShowBadge(false);
        NotificationChannel alarm = new NotificationChannel(CHANNEL_ALARM,
                context.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH);
        alarm.setSound(null, null); // The alarm sound itself is played by SoundPlayer.
        nm.createNotificationChannel(monitor);
        nm.createNotificationChannel(alarm);
    }

    private static void showAlarmNotification(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return; // The widget still shows a tap-to-stop panel.
        }
        createChannels(context);
        PendingIntent open = alarmActivityIntent(context);
        PendingIntent stop = PendingIntent.getBroadcast(context, 3,
                new Intent(context, ChargeAlertReceiver.class).setAction(ACTION_STOP_ALARM),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALARM)
                .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
                .setContentTitle(context.getString(R.string.alarm_title))
                .setContentText(context.getString(R.string.alarm_text))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(open)
                .setFullScreenIntent(open, true)
                .addAction(0, context.getString(R.string.stop_alarm), stop);
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ALARM, builder.build());
    }

    public static PendingIntent alarmActivityIntent(Context context) {
        Intent intent = new Intent(context, AlarmActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, 4, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static boolean hasWidgets(Context context) {
        return widgetIds(context).length > 0;
    }

    private static int[] widgetIds(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        return manager.getAppWidgetIds(new ComponentName(context, TimeWidgetProvider.class));
    }

    private static PendingIntent actionIntent(Context context, String action) {
        Intent intent = new Intent(context, ChargeAlertReceiver.class).setAction(action);
        int requestCode = ACTION_PING.equals(action) ? 1 : 2;
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Exact, Doze-proof scheduling. The unplug alarm uses setAlarmClock (wakes the device even
     * in deep Doze, shows the alarm icon); pings use setExactAndAllowWhileIdle, since Doze
     * doesn't engage while charging. Falls back to inexact if exact alarms aren't permitted.
     */
    private static void schedule(Context context, String action, long atMillis, boolean alarmClock) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) {
            return;
        }
        PendingIntent pi = actionIntent(context, action);
        if (!canScheduleExact(context)) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi);
        } else if (alarmClock) {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(atMillis, alarmActivityIntent(context)), pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi);
        }
    }

    /**
     * SCHEDULE_EXACT_ALARM is granted at install up to Android 13, but is off by default on
     * Android 14+ and the user can revoke it on 12+ (Settings > Alarms & reminders).
     */
    public static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        AlarmManager am = context.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    private static void cancel(Context context, String action) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am != null) {
            am.cancel(actionIntent(context, action));
        }
    }
}
