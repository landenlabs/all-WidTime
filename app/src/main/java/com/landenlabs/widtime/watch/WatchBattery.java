// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.watch;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.landenlabs.widtime.R;
import com.landenlabs.widtime.ui.LauncherActivity;
import com.landenlabs.widtime.util.WatchLogic;
import com.landenlabs.widtime.util.WidgetPrefs;
import com.landenlabs.widtime.widget.TimeWidgetProvider;


/**
 * The paired watch's battery, as last reported by the WidTime watch app (module :wear) over the
 * Wearable Data Layer: shown on widgets that ask for it, and a notification once per charge when
 * the watch is full. One watch is assumed; with several, the latest report wins.
 */
public final class WatchBattery {

    private static final String PREFS = "watch_battery";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_CHARGING = "charging";
    private static final String KEY_FULL = "full";
    private static final String KEY_TIME = "time";
    private static final String KEY_FULL_NOTIFIED = "full_notified";

    private static final String CHANNEL_WATCH = "watch_charge";
    private static final int NOTIFICATION_WATCH_FULL = 7;

    /** The latest reading. {@link #level} is -1 if the watch has never reported. */
    public static final class State {
        public final int level;
        public final boolean charging;
        public final boolean full;
        public final long timeMillis;

        State(int level, boolean charging, boolean full, long timeMillis) {
            this.level = level;
            this.charging = charging;
            this.full = full;
            this.timeMillis = timeMillis;
        }

        public boolean isKnown() {
            return level >= 0;
        }

        /** Recent enough to trust (see WatchLogic.isFresh). */
        public boolean isFresh() {
            return isKnown() && WatchLogic.isFresh(charging, System.currentTimeMillis() - timeMillis);
        }
    }

    /** Whether some watch has the WidTime watch app, or null if there's no Wear OS connection. */
    public interface FindListener {
        void onResult(Boolean installed);
    }

    private WatchBattery() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static State get(Context context) {
        SharedPreferences p = prefs(context);
        return new State(p.getInt(KEY_LEVEL, -1), p.getBoolean(KEY_CHARGING, false),
                p.getBoolean(KEY_FULL, false), p.getLong(KEY_TIME, 0));
    }

    /** A reading arrived from the watch (WatchBatteryListener). */
    static void onReport(Context context, int level, boolean charging, boolean full, long timeMillis) {
        SharedPreferences p = prefs(context);
        if (timeMillis < p.getLong(KEY_TIME, 0)) {
            return; // An older reading delivered late.
        }
        boolean notified = p.getBoolean(KEY_FULL_NOTIFIED, false);
        boolean notify = WatchLogic.shouldNotifyFull(notified, charging, full);
        p.edit().putInt(KEY_LEVEL, level).putBoolean(KEY_CHARGING, charging).putBoolean(KEY_FULL, full)
                .putLong(KEY_TIME, timeMillis)
                .putBoolean(KEY_FULL_NOTIFIED, WatchLogic.nextFullNotified(notified, charging, full))
                .apply();
        if (notify && isFullAlertEnabled(context)) {
            showFullNotification(context, level);
        } else if (!charging) {
            // Off the charger: the notice has done its job.
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_WATCH_FULL);
        }
        TimeWidgetProvider.refreshAll(context);
    }

    /** One watch: the full notice is given once if any widget has it on. */
    private static boolean isFullAlertEnabled(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int widgetId : manager.getAppWidgetIds(new ComponentName(context, TimeWidgetProvider.class))) {
            if (WidgetPrefs.getWatchFullAlert(context, widgetId)) {
                return true;
            }
        }
        return false;
    }

    private static void showFullNotification(Context context, int level) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            // Default importance: the user's notification sound, no heads-up interruption.
            nm.createNotificationChannel(new NotificationChannel(CHANNEL_WATCH,
                    context.getString(R.string.channel_watch), NotificationManager.IMPORTANCE_DEFAULT));
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_WATCH)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle(context.getString(R.string.watch_full_title))
                .setContentText(context.getString(R.string.watch_full_text, Math.max(level, 0)))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(LauncherActivity.fromMonitorIntent(context));
        NotificationManagerCompat.from(context).notify(NOTIFICATION_WATCH_FULL, builder.build());
    }

    /**
     * Asks every connected watch for a reading now. Does nothing without Google Play services or
     * a paired watch.
     */
    public static void requestRefresh(Context context) {
        Context app = context.getApplicationContext();
        Wearable.getNodeClient(app).getConnectedNodes().addOnSuccessListener(nodes -> {
            for (Node node : nodes) {
                Wearable.getMessageClient(app).sendMessage(node.getId(), WatchProtocol.PATH_REFRESH, new byte[0]);
            }
        });
    }

    /** Looks for a paired watch with the WidTime watch app installed; the result is on the main thread. */
    public static void findWatchApp(Context context, FindListener listener) {
        Wearable.getCapabilityClient(context.getApplicationContext())
                .getCapability(WatchProtocol.CAPABILITY, CapabilityClient.FILTER_ALL)
                .addOnSuccessListener(info -> listener.onResult(!info.getNodes().isEmpty()))
                .addOnFailureListener(e -> listener.onResult(null)); // No Wear OS / Play services.
    }
}
