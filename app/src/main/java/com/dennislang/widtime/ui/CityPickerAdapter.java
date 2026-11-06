package com.dennislang.widtime.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dennislang.widtime.R;
import com.dennislang.widtime.model.City;

import java.util.List;

/** Search results shown in {@link AddCityActivity}; tapping a row selects that city. */
public class CityPickerAdapter extends RecyclerView.Adapter<CityPickerAdapter.ViewHolder> {

    public interface OnCitySelectedListener {
        void onCitySelected(City city);
    }

    private final List<City> cities;
    private final OnCitySelectedListener listener;

    public CityPickerAdapter(List<City> cities, OnCitySelectedListener listener) {
        this.cities = cities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_city_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        City city = cities.get(position);
        holder.text.setText(city.getDisplayName());
        holder.text.setOnClickListener(v -> listener.onCitySelected(city));
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView text;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.result_text);
        }
    }
}
