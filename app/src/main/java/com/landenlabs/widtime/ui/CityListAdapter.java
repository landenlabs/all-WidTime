package com.landenlabs.widtime.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.widtime.R;
import com.landenlabs.widtime.model.CityEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * The settings screen's city list. Each row: a selection checkbox, a drag handle, a preview of
 * the city in its own style on the widget background (tap to edit), and a show/hide eye.
 * Selection is by identity, so it survives reordering.
 */
class CityListAdapter extends RecyclerView.Adapter<CityListAdapter.ViewHolder> {

    /** Payload for {@link #refreshTimes()}: rebind only the time text. */
    private static final Object PAYLOAD_TIME = new Object();

    interface Listener {
        void onEdit(CityEntry entry);

        void onSelectionChanged();

        void onVisibilityToggled(CityEntry entry);

        void onStartDrag(RecyclerView.ViewHolder holder);
    }

    private final List<CityEntry> entries;
    private final Listener listener;
    private final Set<CityEntry> selected = Collections.newSetFromMap(new IdentityHashMap<>());
    private int backgroundColor;

    CityListAdapter(List<CityEntry> entries, int backgroundColor, Listener listener) {
        this.entries = entries;
        this.backgroundColor = backgroundColor;
        this.listener = listener;
    }

    Set<CityEntry> selected() {
        return selected;
    }

    /** Checked entries in list order. */
    List<CityEntry> selectedInOrder() {
        List<CityEntry> list = new ArrayList<>();
        for (CityEntry entry : entries) {
            if (selected.contains(entry)) {
                list.add(entry);
            }
        }
        return list;
    }

    @SuppressLint("NotifyDataSetChanged")
    void setAllSelected(boolean all) {
        selected.clear();
        if (all) {
            selected.addAll(entries);
        }
        notifyDataSetChanged();
        listener.onSelectionChanged();
    }

    /** Drops selections for entries no longer in the list (after delete or Undo). */
    void pruneSelection() {
        Set<CityEntry> present = Collections.newSetFromMap(new IdentityHashMap<>());
        present.addAll(entries);
        selected.retainAll(present);
    }

    @SuppressLint("NotifyDataSetChanged")
    void setBackgroundColor(int color) {
        backgroundColor = color;
        notifyDataSetChanged();
    }

    void refreshTimes() {
        notifyItemRangeChanged(0, entries.size(), PAYLOAD_TIME);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_city_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.size() == 1 && payloads.get(0) == PAYLOAD_TIME) {
            CityPreview.bindTime(holder.time, entries.get(position));
        } else {
            onBindViewHolder(holder, position);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CityEntry entry = entries.get(position);
        CityPreview.bind(holder.preview, holder.label, holder.offset, holder.time, entry, backgroundColor);

        holder.check.setOnCheckedChangeListener(null);
        holder.check.setChecked(selected.contains(entry));
        holder.check.setContentDescription(holder.itemView.getContext().getString(R.string.select_city, entry.label));
        holder.check.setOnCheckedChangeListener((b, checked) -> {
            CityEntry e = entryAt(holder);
            if (e == null) {
                return;
            }
            if (checked) {
                selected.add(e);
            } else {
                selected.remove(e);
            }
            listener.onSelectionChanged();
        });

        holder.preview.setOnClickListener(v -> {
            CityEntry e = entryAt(holder);
            if (e != null) {
                listener.onEdit(e);
            }
        });
        // Long-press is a shortcut for the checkbox.
        holder.preview.setOnLongClickListener(v -> {
            holder.check.toggle();
            return true;
        });

        holder.visible.setImageResource(entry.visible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
        holder.visible.setContentDescription(holder.itemView.getContext().getString(
                entry.visible ? R.string.show_on_widget : R.string.hidden_on_widget));
        holder.visible.setOnClickListener(v -> {
            CityEntry e = entryAt(holder);
            if (e != null) {
                listener.onVisibilityToggled(e);
            }
        });

        holder.drag.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                listener.onStartDrag(holder);
            }
            return false;
        });
    }

    private CityEntry entryAt(ViewHolder holder) {
        int pos = holder.getBindingAdapterPosition();
        return pos == RecyclerView.NO_POSITION ? null : entries.get(pos);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox check;
        final ImageView drag;
        final View preview;
        final TextView label;
        final TextView offset;
        final TextView time;
        final ImageButton visible;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            check = itemView.findViewById(R.id.entry_check);
            drag = itemView.findViewById(R.id.entry_drag);
            preview = itemView.findViewById(R.id.entry_preview);
            label = itemView.findViewById(R.id.entry_label);
            offset = itemView.findViewById(R.id.entry_offset);
            time = itemView.findViewById(R.id.entry_time);
            visible = itemView.findViewById(R.id.entry_visible);
        }
    }
}
