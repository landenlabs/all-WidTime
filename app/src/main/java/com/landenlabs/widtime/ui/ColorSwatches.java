package com.landenlabs.widtime.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import com.landenlabs.widtime.R;

import java.util.function.IntConsumer;

/** Fills a horizontal container with the preset color swatches (text color and background). */
final class ColorSwatches {

    private static final int[][] SWATCHES = {
            {R.color.swatch_white, R.string.color_white},
            {R.color.swatch_black, R.string.color_black},
            {R.color.swatch_yellow, R.string.color_yellow},
            {R.color.swatch_red, R.string.color_red},
            {R.color.swatch_cyan, R.string.color_cyan},
            {R.color.swatch_green, R.string.color_green},
    };

    private ColorSwatches() {
    }

    /** Adds one 36dp round button per preset; {@code onPick} receives the opaque ARGB color. */
    static void populate(LinearLayout container, IntConsumer onPick) {
        Context context = container.getContext();
        int size = dp(context, 36);
        int gap = dp(context, 8);
        int outline = MaterialColors.getColor(container, com.google.android.material.R.attr.colorOutline);
        container.removeAllViews();
        for (int[] swatch : SWATCHES) {
            int color = ContextCompat.getColor(context, swatch[0]);
            ImageButton button = new ImageButton(context);
            // Fill and outline drawn separately: a tint would recolor the outline too, and the
            // white swatch would vanish on a light background.
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(color);
            dot.setStroke(dp(context, 1), outline);
            button.setBackground(dot);
            button.setContentDescription(context.getString(swatch[1]));
            button.setOnClickListener(v -> onPick.accept(color));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(gap);
            container.addView(button, lp);
        }
    }

    private static int dp(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics()));
    }
}
