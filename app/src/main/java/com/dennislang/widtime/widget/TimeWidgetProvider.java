package com.dennislang.widtime.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.dennislang.widtime.R;
import com.dennislang.widtime.alerts.ChargeAlerts;
import com.dennislang.widtime.ui.WidgetConfigActivity;
import com.dennislang.widtime.util.BatteryStatus;
import com.dennislang.widtime.util.ChargingInfo;
import com.dennislang.widtime.util.DischargeEstimator;
import com.dennislang.widtime.util.WidgetPrefs;

import java.util.Locale;

public class TimeWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_TICK = "com.dennislang.widtime.ACTION_TICK";
    private static final long TICK_INTERVAL_MILLIS = 60_000L;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId);
        }
        scheduleTick(context);
        ChargeAlerts.syncService(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TICK.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, TimeWidgetProvider.class));
            if (ids.length > 0) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
                refreshAll(context);
            } else {
                cancelTick(context);
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        scheduleTick(context);
    }

    @Override
    public void onDisabled(Context context) {
        cancelTick(context);
        ChargeAlerts.syncService(context); // No widgets left: stops the charger monitor.
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            WidgetPrefs.delete(context, widgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_time);

        views.setInt(R.id.widget_root, "setBackgroundColor", WidgetPrefs.getBackgroundColor(context, widgetId));
        applyDynamicState(context, views, widgetId, BatteryStatus.refresh(context), ChargingInfo.read(context),
                ChargeAlerts.isAlarmActive(context));

        Intent serviceIntent = new Intent(context, CityRemoteViewsService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        serviceIntent.setData(Uri.parse("widtime://widget/" + widgetId));
        views.setRemoteAdapter(R.id.widget_list, serviceIntent);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty_view);

        Intent configIntent = new Intent(context, WidgetConfigActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        configIntent.setData(Uri.parse("widtime://configure/" + widgetId));
        PendingIntent configPendingIntent = PendingIntent.getActivity(
                context, widgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, configPendingIntent);
        views.setOnClickPendingIntent(R.id.widget_empty_view, configPendingIntent);
        views.setPendingIntentTemplate(R.id.widget_list, configPendingIntent);
        views.setOnClickPendingIntent(R.id.alarm_panel, ChargeAlerts.alarmActivityIntent(context));

        appWidgetManager.updateAppWidget(widgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list);
    }

    /**
     * Partially updates every widget's battery bars and its cities / charging / alarm panel.
     * The city list's contents are refreshed separately via notifyAppWidgetViewDataChanged.
     */
    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, TimeWidgetProvider.class));
        if (widgetIds.length == 0) {
            return;
        }
        BatteryStatus status = BatteryStatus.refresh(context);
        ChargingInfo charging = ChargingInfo.read(context);
        boolean alarm = ChargeAlerts.isAlarmActive(context);
        for (int widgetId : widgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_time);
            applyDynamicState(context, views, widgetId, status, charging, alarm);
            manager.partiallyUpdateAppWidget(widgetId, views);
        }
    }

    private static void applyDynamicState(Context context, RemoteViews views, int widgetId,
                                          BatteryStatus status, ChargingInfo charging, boolean alarm) {
        applyBatteryBars(context, views, widgetId, status);

        boolean showCharging = !alarm && charging.plugged;
        views.setViewVisibility(R.id.alarm_panel, alarm ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.charging_panel, showCharging ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.city_area, alarm || showCharging ? View.GONE : View.VISIBLE);

        int color = WidgetPrefs.getTextColor(context, widgetId);
        float size = WidgetPrefs.getTextSize(context, widgetId);
        if (showCharging) {
            views.setTextViewText(R.id.charging_headline, context.getString(
                    charging.full ? R.string.charged_headline : R.string.charging_headline,
                    Math.max(charging.levelPercent, 0)));
            views.setTextViewText(R.id.charging_detail, chargingDetail(charging));
            styleText(views, R.id.charging_headline, color, size);
            styleText(views, R.id.charging_detail, color, size * 0.8f);
        }
        if (alarm) {
            views.setTextViewTextSize(R.id.alarm_headline, TypedValue.COMPLEX_UNIT_SP, size);
            styleText(views, R.id.alarm_detail, color, size * 0.8f);
        }
    }

    /** e.g. "Fast · AC · 1.85 A"; parts the device doesn't report are left out. */
    private static String chargingDetail(ChargingInfo info) {
        StringBuilder sb = new StringBuilder();
        if (info.speed != null) {
            sb.append(info.speed.label);
        }
        if (info.source != null) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(info.source);
        }
        if (!Double.isNaN(info.currentMilliAmps)) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(String.format(Locale.getDefault(), "%.2f A", info.currentMilliAmps / 1000.0));
        }
        return sb.toString();
    }

    private static void styleText(RemoteViews views, int viewId, int color, float sizeSp) {
        views.setTextColor(viewId, color);
        views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, sizeSp);
    }

    private static void applyBatteryBars(Context context, RemoteViews views, int widgetId, BatteryStatus status) {
        boolean showBattery = WidgetPrefs.getShowBatteryBar(context, widgetId) && status.levelPercent >= 0;
        views.setViewVisibility(R.id.battery_bar, showBattery ? View.VISIBLE : View.GONE);
        if (showBattery) {
            setBar(views, R.id.battery_bar, status.levelPercent);
        }

        boolean showTimeLeft = WidgetPrefs.getShowTimeLeftBar(context, widgetId);
        views.setViewVisibility(R.id.time_left_bar, showTimeLeft ? View.VISIBLE : View.GONE);
        if (showTimeLeft) {
            // Unknown estimate: show an empty track rather than a misleading value.
            double percent = Double.isNaN(status.hoursLeft)
                    ? 0 : DischargeEstimator.percentOfFullScale(status.hoursLeft);
            setBar(views, R.id.time_left_bar, percent);
        }
    }

    private static void setBar(RemoteViews views, int viewId, double percent) {
        views.setInt(viewId, "setImageLevel", (int) Math.round(percent * 100));
        views.setInt(viewId, "setColorFilter", DischargeEstimator.colorForPercent(percent));
    }

    private static void scheduleTick(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        alarmManager.setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis(),
                TICK_INTERVAL_MILLIS,
                tickPendingIntent(context));
    }

    private static void cancelTick(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(tickPendingIntent(context));
        }
    }

    private static PendingIntent tickPendingIntent(Context context) {
        Intent intent = new Intent(context, TimeWidgetProvider.class);
        intent.setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
