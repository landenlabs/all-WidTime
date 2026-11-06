package com.dennislang.widtime.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.dennislang.widtime.alerts.ChargeAlerts;

/**
 * Re-arms the per-minute refresh alarm after a reboot (alarms don't survive it) and restarts the
 * charger monitor after a reboot or app update; both broadcasts may start foreground services.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(action);
        if (!boot && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        if (boot) {
            ChargeAlerts.onBoot(context);
        }
        ChargeAlerts.syncService(context);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, TimeWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(provider);
        if (widgetIds.length > 0) {
            Intent update = new Intent(context, TimeWidgetProvider.class);
            update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
            context.sendBroadcast(update);
        }
    }
}
