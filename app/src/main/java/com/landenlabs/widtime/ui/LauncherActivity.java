// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.landenlabs.widtime.R;
import com.landenlabs.widtime.model.CityEntry;
import com.landenlabs.widtime.util.WidgetPrefs;
import com.landenlabs.widtime.widget.TimeWidgetProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Launcher icon screen. The app is a home screen widget, so this only explains how to add one,
 * offers to pin one (Android 8+ launchers that support it), and opens the settings of each
 * widget already on the home screen.
 */
public class LauncherActivity extends AppCompatActivity {

    private LinearLayout widgetList;
    private TextView noWidgets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        SystemBarInsets.apply(this);

        ((TextView) findViewById(R.id.launcher_version)).setText(getString(R.string.launcher_version, versionName()));
        widgetList = findViewById(R.id.launcher_widget_list);
        noWidgets = findViewById(R.id.launcher_no_widgets);

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        View addButton = findViewById(R.id.launcher_add_widget);
        if (manager.isRequestPinAppWidgetSupported()) {
            // The launcher shows its own confirm dialog. The pinned widget opens its settings when
            // tapped and shows up in the list below when this screen resumes.
            addButton.setOnClickListener(v -> manager.requestPinAppWidget(widgetProvider(), null, null));
        } else {
            addButton.setVisibility(View.GONE);
        }

        findViewById(R.id.launcher_privacy).setOnClickListener(v -> PrivacyDialog.show(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        showWidgets();
    }

    private void showWidgets() {
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(widgetProvider());
        widgetList.removeAllViews();
        noWidgets.setVisibility(ids.length == 0 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < ids.length; i++) {
            int widgetId = ids[i];
            MaterialButton button = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setText(getString(R.string.launcher_widget_item, i + 1, citySummary(widgetId)));
            button.setAllCaps(false);
            button.setIconResource(R.drawable.ic_settings);
            button.setOnClickListener(v -> openSettings(widgetId));
            widgetList.addView(button, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private String citySummary(int widgetId) {
        List<String> labels = new ArrayList<>();
        for (CityEntry entry : WidgetPrefs.getVisibleCities(this, widgetId)) {
            labels.add(entry.label);
        }
        return labels.isEmpty() ? getString(R.string.launcher_widget_no_cities) : String.join(", ", labels);
    }

    /** Same intent the widget's own tap uses (see TimeWidgetProvider). */
    private void openSettings(int widgetId) {
        Intent intent = new Intent(this, WidgetConfigActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setData(Uri.parse("widtime://configure/" + widgetId));
        startActivity(intent);
    }

    private ComponentName widgetProvider() {
        return new ComponentName(this, TimeWidgetProvider.class);
    }

    private String versionName() {
        try {
            String name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return name == null ? "" : name;
        } catch (PackageManager.NameNotFoundException e) {
            return "";  // Can't happen for our own package.
        }
    }
}
