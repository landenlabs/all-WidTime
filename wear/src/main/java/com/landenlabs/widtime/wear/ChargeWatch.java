// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.wear;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Watches the watch's charger without a running service, which would cost battery all day:
 * <ul>
 *   <li>A job that requires charging wakes the app when the watch is put on its charger.</li>
 *   <li>While on the charger, an alarm re-checks every minute (every 5 once full) and notices
 *       when the watch is taken off. Charging lifts Android's background limits, so these run on
 *       time.</li>
 *   <li>A 30 minute periodic job reports the level while on battery, and re-arms the charging
 *       job if anything went missing.</li>
 * </ul>
 * Each check sends the reading to the phone when it changed, or every 10 minutes regardless,
 * so the phone can tell a current reading from an old one.
 */
final class ChargeWatch {

    private static final String TAG = "WidTimeWatch";

    static final int JOB_CHARGER = 1;
    private static final int JOB_CHECK = 2;
    private static final long POLL_CHARGING_MILLIS = 60_000L;
    private static final long POLL_FULL_MILLIS = 5 * 60_000L;
    private static final long CHECK_PERIOD_MILLIS = 30 * 60_000L;
    private static final long CHECK_FLEX_MILLIS = 10 * 60_000L;
    private static final long HEARTBEAT_MILLIS = 10 * 60_000L;
    /** Under the 10 s a receiver may take, so a slow Data Layer can't cause an ANR. */
    private static final long SEND_TIMEOUT_MILLIS = 8_000L;

    private static final String PREFS = "watch_report";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_CHARGING = "charging";
    private static final String KEY_FULL = "full";
    private static final String KEY_SENT = "sent_millis";

    private ChargeWatch() {
    }

    /**
     * Reads the battery, arms whatever should run next, and reports to the phone if needed.
     * Blocks while sending, so call it off the main thread.
     *
     * @param force send even if nothing changed (the phone asked, or the app was opened)
     */
    static BatteryState check(Context context, boolean force) {
        Context app = context.getApplicationContext();
        BatteryState state = BatteryState.read(app);
        if (state.charging) {
            schedulePoll(app, state.full ? POLL_FULL_MILLIS : POLL_CHARGING_MILLIS);
        } else {
            cancelPoll(app);
            scheduleChargerJob(app);
        }
        schedulePeriodicCheck(app);

        SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean changed = p.getInt(KEY_LEVEL, -2) != state.level
                || p.getBoolean(KEY_CHARGING, false) != state.charging
                || p.getBoolean(KEY_FULL, false) != state.full;
        long now = System.currentTimeMillis();
        if (force || changed || now - p.getLong(KEY_SENT, 0) >= HEARTBEAT_MILLIS) {
            if (send(app, state, now)) {
                p.edit().putInt(KEY_LEVEL, state.level).putBoolean(KEY_CHARGING, state.charging)
                        .putBoolean(KEY_FULL, state.full).putLong(KEY_SENT, now).apply();
            }
        }
        return state;
    }

    /**
     * An urgent data item: the Data Layer delivers it to the phone right away if connected, or
     * when it reconnects. Returns false if it couldn't be stored, so the next check retries.
     */
    private static boolean send(Context context, BatteryState state, long now) {
        PutDataMapRequest request = PutDataMapRequest.create(WatchProtocol.PATH_BATTERY);
        DataMap map = request.getDataMap();
        map.putInt(WatchProtocol.KEY_LEVEL, state.level);
        map.putBoolean(WatchProtocol.KEY_CHARGING, state.charging);
        map.putBoolean(WatchProtocol.KEY_FULL, state.full);
        map.putLong(WatchProtocol.KEY_TIME, now);
        PutDataRequest put = request.asPutDataRequest().setUrgent();
        try {
            Tasks.await(Wearable.getDataClient(context).putDataItem(put),
                    SEND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Battery report not sent", e);
            return false;
        }
    }

    private static void scheduleChargerJob(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js == null || js.getPendingJob(JOB_CHARGER) != null) {
            return;
        }
        js.schedule(new JobInfo.Builder(JOB_CHARGER, new ComponentName(context, ChargeJobService.class))
                .setRequiresCharging(true)
                .setPersisted(true)
                .build());
    }

    private static void schedulePeriodicCheck(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js == null || js.getPendingJob(JOB_CHECK) != null) {
            return;
        }
        js.schedule(new JobInfo.Builder(JOB_CHECK, new ComponentName(context, ChargeJobService.class))
                .setPeriodic(CHECK_PERIOD_MILLIS, CHECK_FLEX_MILLIS)
                .setPersisted(true)
                .build());
    }

    /** Inexact, so no exact-alarm permission is needed; a later call replaces the pending one. */
    private static void schedulePoll(Context context, long delayMillis) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am != null) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delayMillis, pollIntent(context));
        }
    }

    private static void cancelPoll(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am != null) {
            am.cancel(pollIntent(context));
        }
    }

    private static PendingIntent pollIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, PollReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
