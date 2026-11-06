package com.dennislang.widtime.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dennislang.widtime.R;
import com.dennislang.widtime.model.CityEntry;

import java.util.List;

/** Shows the cities configured on a widget instance; the label is editable in place, with a remove button per row. */
public class CityEntryAdapter extends RecyclerView.Adapter<CityEntryAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private final List<CityEntry> entries;
    private final OnRemoveListener removeListener;

    public CityEntryAdapter(List<CityEntry> entries, OnRemoveListener removeListener) {
        this.entries = entries;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_city_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CityEntry entry = entries.get(position);

        if (holder.labelWatcher != null) {
            holder.label.removeTextChangedListener(holder.labelWatcher);
        }
        holder.label.setText(entry.label);
        holder.labelWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    entries.get(adapterPosition).label = s.toString();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        holder.label.addTextChangedListener(holder.labelWatcher);

        holder.zone.setText(entry.zoneId);
        holder.removeButton.setOnClickListener(v -> removeListener.onRemove(holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final EditText label;
        final TextView zone;
        final View removeButton;
        TextWatcher labelWatcher;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.entry_label);
            zone = itemView.findViewById(R.id.entry_zone);
            removeButton = itemView.findViewById(R.id.entry_remove_button);
        }
    }
}
