package com.landenlabs.widtime.alerts;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.landenlabs.widtime.R;
import com.landenlabs.widtime.util.ChargingInfo;
import com.landenlabs.widtime.widget.TimeWidgetProvider;

/**
 * Since Android 8, POWER_CONNECTED/DISCONNECTED are only delivered to receivers registered at
 * runtime, so this foreground service stays running to catch them instantly (and to refresh the
 * widget's charging panel and speak "Charged to N percent" when the level changes). It shows a
 * minimal, silent notification.
 */
public class ChargeMonitorService extends Service {

    private int lastLevel = -1;
    private int lastPlugged = -1;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                ChargeAlerts.onPowerConnected(context);
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                ChargeAlerts.onPowerDisconnected(context);
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                if (level != lastLevel || plugged != lastPlugged) {
                    lastLevel = level;
                    lastPlugged = plugged;
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                    int percent = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
                    ChargeAlerts.onBatteryLevel(context, percent, plugged != 0);
                    TimeWidgetProvider.refreshAll(context);
                }
            } else if (ChargingInfo.ACTION_USB_STATE.equals(action)) {
                // Host enumeration lands a moment after plug-in; also fires on USB mode changes.
                TimeWidgetProvider.refreshAll(context);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ChargingInfo.ACTION_USB_STATE);
        // System broadcasts are still delivered to non-exported receivers.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Every startForegroundService() must be answered with startForeground().
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ChargeAlerts.NOTIFICATION_MONITOR, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(ChargeAlerts.NOTIFICATION_MONITOR, notification);
        }
        ChargeAlerts.reconcile(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        Speaker.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        ChargeAlerts.createChannels(this);
        return new NotificationCompat.Builder(this, ChargeAlerts.CHANNEL_MONITOR)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle(getString(R.string.monitor_notification_title))
                .setContentText(getString(R.string.monitor_notification_text))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }
}
