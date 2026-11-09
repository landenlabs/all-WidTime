package com.landenlabs.widtime.alerts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.landenlabs.widtime.util.ChargeLogic;

/** Receives the scheduled ping/alarm AlarmManager events and the notification's Stop action. */
public class ChargeAlertReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        long keepAliveMillis = 0;
        switch (action) {
            case ChargeAlerts.ACTION_PING:
                ChargeAlerts.onPingDue(context);
                keepAliveMillis = 1_000;
                break;
            case ChargeAlerts.ACTION_ALARM:
                ChargeAlerts.onAlarmDue(context);
                keepAliveMillis = ChargeLogic.ALARM_PLAY_MILLIS + 500;
                break;
            case ChargeAlerts.ACTION_STOP_ALARM:
                ChargeAlerts.stopAlarm(context);
                break;
            default:
                return;
        }
        if (keepAliveMillis > 0) {
            // Keep the process alive while the sound plays, in case the monitor service isn't running.
            PendingResult result = goAsync();
            new Handler(Looper.getMainLooper()).postDelayed(result::finish, keepAliveMillis);
        }
    }
}
