package com.landenlabs.widtime.ui;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Targeting API 35+ makes every activity edge-to-edge (and API 36 removes the opt-out), so the
 * layout would otherwise draw under the status bar, navigation bar, cutout and keyboard.
 */
final class SystemBarInsets {

    private SystemBarInsets() {
    }

    /** Pads the activity's content frame by the system bar, cutout and IME insets. Call after setContentView. */
    static void apply(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
