package com.landenlabs.widtime.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;

import androidx.core.view.ViewCompat;

import com.landenlabs.widtime.R;

/**
 * Makes a settings section collapsible: tapping its header shows or hides its body and turns the
 * chevron. Whether each section is open is remembered across visits (for all widgets).
 */
final class CollapsibleSection {

    private static final String PREFS = "settings_ui";

    private CollapsibleSection() {
    }

    static void bind(Activity activity, int headerId, int bodyId, int chevronId, String key, boolean expandedByDefault) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        View header = activity.findViewById(headerId);
        View body = activity.findViewById(bodyId);
        View chevron = activity.findViewById(chevronId);
        String prefKey = "expanded_" + key;

        apply(activity, header, body, chevron, prefs.getBoolean(prefKey, expandedByDefault));
        header.setOnClickListener(v -> {
            boolean expanded = body.getVisibility() != View.VISIBLE;
            prefs.edit().putBoolean(prefKey, expanded).apply();
            apply(activity, header, body, chevron, expanded);
        });
    }

    private static void apply(Activity activity, View header, View body, View chevron, boolean expanded) {
        body.setVisibility(expanded ? View.VISIBLE : View.GONE);
        chevron.setRotation(expanded ? 180f : 0f);
        ViewCompat.setStateDescription(header, activity.getString(expanded ? R.string.expanded : R.string.collapsed));
    }
}
