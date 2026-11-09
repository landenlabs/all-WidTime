package com.landenlabs.widtime.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.landenlabs.widtime.R;
import com.landenlabs.widtime.model.CityEntry;
import com.landenlabs.widtime.util.DateTimePatterns;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Bottom sheet that edits one city (name, color, size, format) or several checked cities (style
 * only). Edits apply to the cities as you make them, like the rest of the settings screen. For
 * several cities, a field whose values differ shows "Mixed" and is only applied once changed.
 */
final class CityStyleSheet {

    static final float MIN_TEXT_SIZE_SP = 8f;
    private static final int TEXT_SIZE_STEPS = 32;

    interface Listener {
        /** Something about the targets changed; save and redraw. */
        void onChanged();

        void onDelete(List<CityEntry> targets);
    }

    private final BottomSheetDialog dialog;
    private final List<CityEntry> targets;
    private final Listener listener;
    private final int backgroundColor;

    private final View preview;
    private final TextView previewLabel;
    private final TextView previewTime;
    private final TextInputLayout hexLayout;
    private final EditText hexInput;
    private final TextView sizeValue;
    private final TextInputLayout patternLayout;

    @SuppressLint("InflateParams") // A bottom sheet has no parent to inflate into.
    private CityStyleSheet(Context context, List<CityEntry> targets, int backgroundColor, Listener listener) {
        this.targets = targets;
        this.listener = listener;
        this.backgroundColor = backgroundColor;
        CityEntry first = targets.get(0);
        boolean single = targets.size() == 1;

        View view = LayoutInflater.from(context).inflate(R.layout.sheet_city_style, null, false);
        dialog = new BottomSheetDialog(context);
        dialog.setContentView(view);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);

        TextView title = view.findViewById(R.id.sheet_title);
        title.setText(single ? context.getString(R.string.edit_city_title, first.label)
                : context.getString(R.string.edit_cities_title, targets.size()));

        preview = view.findViewById(R.id.sheet_preview);
        previewLabel = view.findViewById(R.id.sheet_preview_label);
        previewTime = view.findViewById(R.id.sheet_preview_time);

        // Name: only for a single city.
        TextInputLayout nameLayout = view.findViewById(R.id.sheet_name_layout);
        EditText nameInput = view.findViewById(R.id.sheet_name);
        nameLayout.setVisibility(single ? View.VISIBLE : View.GONE);
        if (single) {
            nameInput.setText(first.label);
            nameInput.addTextChangedListener(afterChange(text -> {
                first.label = text;
                changed();
            }));
        }

        // Text color.
        hexLayout = view.findViewById(R.id.sheet_hex_layout);
        hexInput = view.findViewById(R.id.sheet_hex);
        Integer color = common(e -> e.textColor);
        if (color != null) {
            hexInput.setText(hex(color));
        } else {
            hexLayout.setHelperText(context.getString(R.string.mixed));
        }
        hexInput.addTextChangedListener(afterChange(text -> {
            try {
                int rgb = Color.parseColor(text);
                for (CityEntry e : targets) {
                    e.textColor = 0xFF000000 | rgb;
                }
                hexLayout.setHelperText(null);
                changed();
            } catch (IllegalArgumentException ignored) {
                // Not a full/valid hex value yet; keep the last valid color until it is.
            }
        }));
        ColorSwatches.populate(view.findViewById(R.id.sheet_swatches), picked -> hexInput.setText(hex(picked)));

        // Text size.
        sizeValue = view.findViewById(R.id.sheet_size_value);
        SeekBar sizeSlider = view.findViewById(R.id.sheet_size);
        Float size = common(e -> e.textSizeSp);
        sizeSlider.setMax(TEXT_SIZE_STEPS);
        sizeSlider.setProgress(Math.round((size != null ? size : first.textSizeSp) - MIN_TEXT_SIZE_SP));
        if (size != null) {
            showSize(size);
        } else {
            sizeValue.setText(R.string.text_size_mixed);
        }
        sizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                float sp = MIN_TEXT_SIZE_SP + progress;
                for (CityEntry e : targets) {
                    e.textSizeSp = sp;
                }
                showSize(sp);
                changed();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Date/time format.
        patternLayout = view.findViewById(R.id.sheet_pattern_layout);
        EditText patternInput = view.findViewById(R.id.sheet_pattern);
        String pattern = common(e -> e.pattern);
        if (pattern != null) {
            patternInput.setText(pattern);
        } else {
            patternLayout.setHelperText(context.getString(R.string.mixed));
        }
        patternInput.addTextChangedListener(afterChange(text -> {
            try {
                DateTimePatterns.validate(text, Locale.getDefault());
            } catch (IllegalArgumentException e) {
                patternLayout.setError(context.getString(R.string.invalid_pattern));
                return;
            }
            patternLayout.setError(null);
            patternLayout.setHelperText(null);
            for (CityEntry e : targets) {
                e.pattern = text;
            }
            changed();
        }));

        view.findViewById(R.id.sheet_delete).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onDelete(targets);
        });
        view.findViewById(R.id.sheet_done).setOnClickListener(v -> dialog.dismiss());

        updatePreview();
    }

    static void show(Context context, List<CityEntry> targets, int backgroundColor, Listener listener) {
        if (!targets.isEmpty()) {
            new CityStyleSheet(context, targets, backgroundColor, listener).dialog.show();
        }
    }

    private void changed() {
        updatePreview();
        listener.onChanged();
    }

    private void updatePreview() {
        CityPreview.bind(preview, previewLabel, null, previewTime, targets.get(0), backgroundColor);
        preview.setAlpha(1f); // The preview shows the style even for a hidden city.
    }

    private void showSize(float sp) {
        sizeValue.setText(sizeValue.getContext().getString(R.string.text_size_value, Math.round(sp)));
    }

    /** The value every target shares, or null if they differ ("Mixed"). */
    private <T> T common(Function<CityEntry, T> getter) {
        T value = getter.apply(targets.get(0));
        for (CityEntry e : targets) {
            if (!Objects.equals(value, getter.apply(e))) {
                return null;
            }
        }
        return value;
    }

    private static String hex(int color) {
        return String.format(Locale.US, "#%06X", 0xFFFFFF & color);
    }

    /** TextWatcher reporting the new text. Added after the field is filled in, so only edits (typing or a swatch tap) are reported. */
    private static TextWatcher afterChange(Consumer<String> onChange) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onChange.accept(s.toString());
            }
        };
    }
}
