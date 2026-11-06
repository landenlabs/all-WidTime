package com.dennislang.widtime.widget;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.dennislang.widtime.R;
import com.dennislang.widtime.model.CityEntry;
import com.dennislang.widtime.util.DateTimePatterns;
import com.dennislang.widtime.util.WidgetPrefs;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CityRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final int widgetId;
    private final List<CityEntry> entries = new ArrayList<>();

    private int textColor;
    private float textSizeSp;
    private String pattern;

    CityRemoteViewsFactory(Context context, int widgetId) {
        this.context = context;
        this.widgetId = widgetId;
    }

    @Override
    public void onCreate() {
        loadData();
    }

    @Override
    public void onDataSetChanged() {
        loadData();
    }

    private void loadData() {
        entries.clear();
        entries.addAll(WidgetPrefs.getCities(context, widgetId));
        textColor = WidgetPrefs.getTextColor(context, widgetId);
        textSizeSp = WidgetPrefs.getTextSize(context, widgetId);
        pattern = WidgetPrefs.getPattern(context, widgetId);
    }

    @Override
    public void onDestroy() {
        entries.clear();
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_time_item);
        if (position < 0 || position >= entries.size()) {
            return row;
        }
        CityEntry entry = entries.get(position);

        row.setTextViewText(R.id.item_city_label, entry.label);
        row.setTextColor(R.id.item_city_label, textColor);
        row.setTextViewTextSize(R.id.item_city_label, TypedValue.COMPLEX_UNIT_SP, textSizeSp);

        row.setTextViewText(R.id.item_city_time, formatTime(entry));
        row.setTextColor(R.id.item_city_time, textColor);
        row.setTextViewTextSize(R.id.item_city_time, TypedValue.COMPLEX_UNIT_SP, textSizeSp);

        row.setOnClickFillInIntent(R.id.item_root, new Intent());
        return row;
    }

    private String formatTime(CityEntry entry) {
        try {
            ZoneId zoneId = ZoneId.of(entry.zoneId);
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            return DateTimePatterns.format(now, pattern, Locale.getDefault());
        } catch (DateTimeException | IllegalArgumentException e) {
            return context.getString(R.string.format_error);
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
