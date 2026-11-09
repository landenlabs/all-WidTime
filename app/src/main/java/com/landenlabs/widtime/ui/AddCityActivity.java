package com.landenlabs.widtime.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.widtime.R;
import com.landenlabs.widtime.data.CityDatabase;
import com.landenlabs.widtime.model.City;

import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.util.List;

/**
 * City lookup screen. Selecting a search result returns its IANA zone id; when a city can't be
 * found the manual entry section lets the user supply a label and a fixed UTC offset instead.
 */
public class AddCityActivity extends AppCompatActivity {

    public static final String EXTRA_LABEL = "com.landenlabs.widtime.EXTRA_LABEL";
    public static final String EXTRA_ZONE_ID = "com.landenlabs.widtime.EXTRA_ZONE_ID";

    private RecyclerView resultsView;
    private TextView noMatchesText;
    private View manualEntrySection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_city);
        SystemBarInsets.apply(this);

        EditText searchInput = findViewById(R.id.city_search_input);
        resultsView = findViewById(R.id.city_results_recycler_view);
        noMatchesText = findViewById(R.id.no_matches_text);
        Button manualToggleButton = findViewById(R.id.manual_entry_toggle_button);
        manualEntrySection = findViewById(R.id.manual_entry_section);
        EditText manualLabelInput = findViewById(R.id.manual_label_input);
        EditText manualOffsetInput = findViewById(R.id.manual_offset_input);
        Button manualConfirmButton = findViewById(R.id.manual_entry_confirm_button);

        resultsView.setLayoutManager(new LinearLayoutManager(this));
        showResults(CityDatabase.all());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                showResults(CityDatabase.search(s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        manualToggleButton.setOnClickListener(v -> manualEntrySection.setVisibility(
                manualEntrySection.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        manualConfirmButton.setOnClickListener(v -> {
            String label = manualLabelInput.getText().toString().trim();
            String offsetText = manualOffsetInput.getText().toString().trim();
            if (label.isEmpty()) {
                manualLabelInput.setError(getString(R.string.manual_city_label_hint));
                return;
            }
            try {
                ZoneOffset offset = ZoneOffset.of(offsetText);
                finishWithResult(label, offset.getId());
            } catch (DateTimeException e) {
                Toast.makeText(this, R.string.invalid_offset, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showResults(List<City> cities) {
        resultsView.setAdapter(new CityPickerAdapter(cities, city ->
                finishWithResult(city.name, city.zoneId)));
        boolean empty = cities.isEmpty();
        noMatchesText.setVisibility(empty ? View.VISIBLE : View.GONE);
        resultsView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void finishWithResult(String label, String zoneId) {
        Intent result = new Intent();
        result.putExtra(EXTRA_LABEL, label);
        result.putExtra(EXTRA_ZONE_ID, zoneId);
        setResult(RESULT_OK, result);
        finish();
    }
}
