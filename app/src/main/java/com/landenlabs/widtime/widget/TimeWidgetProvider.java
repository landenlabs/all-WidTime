package com.landenlabs.widtime.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.landenlabs.widtime.R;
import com.landenlabs.widtime.alerts.ChargeAlerts;
import com.landenlabs.widtime.model.CityEntry;
import com.landenlabs.widtime.ui.WidgetConfigActivity;
import com.landenlabs.widtime.util.BatteryStatus;
import com.landenlabs.widtime.util.ChargeLogic;
import com.landenlabs.widtime.util.ChargingInfo;
import com.landenlabs.widtime.util.DischargeEstimator;
import com.landenlabs.widtime.util.WidgetMode;
import com.landenlabs.widtime.util.WidgetPrefs;

import java.util.List;
import java.util.Locale;

public class TimeWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_TICK = "com.landenlabs.widtime.ACTION_TICK";
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
        views.setOnClickPendingIntent(R.id.charging_bell, ChargeAlerts.toggleMuteIntent(context));

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
        List<CityEntry> visible = WidgetPrefs.getVisibleCities(context, widgetId);

        // Battery-only mode replaces the whole widget, charging or not; only the unplug alarm wins.
        boolean batteryOnly = !alarm && WidgetMode.isBatteryOnly(visible.size(),
                WidgetPrefs.getShowBatteryBar(context, widgetId),
                WidgetPrefs.getShowTimeLeftBar(context, widgetId), status.levelPercent);
        views.setViewVisibility(R.id.big_battery_panel, batteryOnly ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_content, batteryOnly ? View.GONE : View.VISIBLE);
        if (batteryOnly) {
            applyBigBatteryBar(context, views, status.levelPercent, charging.plugged);
            return;
        }

        applyBatteryBars(context, views, widgetId, status, charging.plugged);

        // At 100% the charge is done: back to the clocks (the battery bar stays while plugged).
        boolean showCharging = !alarm && charging.plugged && !ChargeLogic.isFull(charging.levelPercent);
        views.setViewVisibility(R.id.alarm_panel, alarm ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.charging_panel, showCharging ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.city_area, alarm || showCharging ? View.GONE : View.VISIBLE);

        // The charging and alarm panels take the first visible city's style.
        CityEntry style = visible.isEmpty() ? WidgetPrefs.defaultStyle(context, widgetId) : visible.get(0);
        int color = style.textColor;
        float size = style.textSizeSp;
        if (showCharging) {
            views.setTextViewText(R.id.charging_headline, context.getString(
                    charging.full ? R.string.charged_headline : R.string.charging_headline,
                    Math.max(charging.levelPercent, 0)));
            views.setTextViewText(R.id.charging_detail, chargingDetail(charging));
            styleText(views, R.id.charging_headline, color, size);
            styleText(views, R.id.charging_detail, color, size * 0.8f);
            views.setViewVisibility(R.id.charging_usb, charging.usbData != null ? View.VISIBLE : View.GONE);
            if (charging.usbData != null) {
                views.setTextViewText(R.id.charging_usb, charging.usbData);
                styleText(views, R.id.charging_usb, color, size * 0.8f);
            }
            applyBell(context, views, color, size);
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

    /** Bell on the charging panel's right edge: mutes pings, speech + unplug alarm for this charge only. */
    private static void applyBell(Context context, RemoteViews views, int color, float textSizeSp) {
        if (!ChargeAlerts.hasChargingSounds(context)) {
            views.setViewVisibility(R.id.charging_bell, View.GONE);
            return;
        }
        boolean muted = ChargeAlerts.isSessionMuted(context);
        views.setViewVisibility(R.id.charging_bell, View.VISIBLE);
        views.setImageViewResource(R.id.charging_bell, muted ? R.drawable.ic_bell_off : R.drawable.ic_bell);
        views.setInt(R.id.charging_bell, "setColorFilter", color);
        views.setInt(R.id.charging_bell, "setImageAlpha", muted ? 0x99 : 0xFF);
        views.setContentDescription(R.id.charging_bell,
                context.getString(muted ? R.string.bell_off_desc : R.string.bell_on_desc));
        if (Build.VERSION.SDK_INT >= 31) {
            // Scale with the text (two lines of it), but keep a usable touch target.
            float sideSp = Math.max(textSizeSp * 2f, 24f);
            views.setViewLayoutWidth(R.id.charging_bell, sideSp, TypedValue.COMPLEX_UNIT_SP);
            views.setViewLayoutHeight(R.id.charging_bell, sideSp, TypedValue.COMPLEX_UNIT_SP);
        }
    }

    /** The top (battery level) bar is always shown while charging, whatever the widget setting. */
    private static void applyBatteryBars(Context context, RemoteViews views, int widgetId,
                                         BatteryStatus status, boolean plugged) {
        boolean showBattery = (plugged || WidgetPrefs.getShowBatteryBar(context, widgetId))
                && status.levelPercent >= 0;
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

    /** The battery-only bar: level fill and centered percent; brighter fill and a bolt while plugged. */
    private static void applyBigBatteryBar(Context context, RemoteViews views, int levelPercent,
                                           boolean plugged) {
        int color = DischargeEstimator.colorForPercent(levelPercent);
        if (plugged) {
            color = WidgetMode.blendTowardWhite(color, WidgetMode.CHARGING_WHITEN);
        }
        views.setInt(R.id.big_battery_bar, "setImageLevel", levelPercent * 100);
        views.setInt(R.id.big_battery_bar, "setColorFilter", color);
        views.setTextViewText(R.id.big_battery_text, context.getString(
                plugged ? R.string.big_battery_text_charging : R.string.big_battery_text, levelPercent));
        views.setContentDescription(R.id.big_battery_text, context.getString(
                plugged ? R.string.big_battery_desc_charging : R.string.big_battery_desc, levelPercent));
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
