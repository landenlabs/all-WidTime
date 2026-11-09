package com.landenlabs.widtime.ui;

import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.landenlabs.widtime.R;
import com.landenlabs.widtime.model.CityEntry;
import com.landenlabs.widtime.util.DateTimePatterns;
import com.landenlabs.widtime.util.UtcOffsets;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

/** Draws a city the way the widget will: its name and time in its own style, on the widget background. */
final class CityPreview {

    private static final float HIDDEN_ALPHA = 0.4f;

    private CityPreview() {
    }

    static void bind(View tile, TextView label, @Nullable TextView offset, TextView time,
                     CityEntry entry, int backgroundColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(backgroundColor);
        bg.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                tile.getResources().getDisplayMetrics()));
        tile.setBackground(bg);
        tile.setAlpha(entry.visible ? 1f : HIDDEN_ALPHA);

        label.setText(entry.label);
        style(label, entry.textColor, entry.textSizeSp);
        bindTime(time, entry);

        if (offset != null) {
            offset.setText(UtcOffsets.label(entry.zoneId, Instant.now()));
            offset.setTextColor((entry.textColor & 0x00FFFFFF) | 0xB3000000);
        }
    }

    /** Only the time, for the once-a-minute refresh. */
    static void bindTime(TextView time, CityEntry entry) {
        time.setText(format(time, entry));
        style(time, entry.textColor, entry.textSizeSp);
    }

    private static String format(TextView view, CityEntry entry) {
        try {
            return DateTimePatterns.format(ZonedDateTime.now(ZoneId.of(entry.zoneId)), entry.pattern, Locale.getDefault());
        } catch (DateTimeException | IllegalArgumentException e) {
            return view.getContext().getString(R.string.invalid_pattern);
        }
    }

    private static void style(TextView view, int color, float sizeSp) {
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
    }
}
