package com.landenlabs.widtime.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.landenlabs.widtime.data.CityQuery;
import com.landenlabs.widtime.data.OpenMeteoGeocoder;
import com.landenlabs.widtime.model.City;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * City lookup screen. Typing shows built-in matches at once, then, after a short pause, results
 * from the online geocoder ({@link OpenMeteoGeocoder}); if the network fails the built-in matches
 * stay. Selecting a result returns its IANA zone id; when a city can't be found the manual entry
 * section lets the user supply a label and a fixed UTC offset instead.
 */
public class AddCityActivity extends AppCompatActivity {

    public static final String EXTRA_LABEL = "com.landenlabs.widtime.EXTRA_LABEL";
    public static final String EXTRA_ZONE_ID = "com.landenlabs.widtime.EXTRA_ZONE_ID";

    /** Wait this long after the last keystroke before asking the geocoder. */
    private static final long SEARCH_DELAY_MS = 400;

    private RecyclerView resultsView;
    private TextView noMatchesText;
    private TextView statusText;
    private View manualEntrySection;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Runnable pendingSearch;
    /** Bumped on every keystroke so late answers to older text are ignored. */
    private int searchGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_city);
        SystemBarInsets.apply(this);

        EditText searchInput = findViewById(R.id.city_search_input);
        resultsView = findViewById(R.id.city_results_recycler_view);
        noMatchesText = findViewById(R.id.no_matches_text);
        statusText = findViewById(R.id.search_status_text);
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
                onSearchTextChanged(s.toString());
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

    @Override
    protected void onDestroy() {
        searchGeneration++;
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }

    private void onSearchTextChanged(String text) {
        int generation = ++searchGeneration;
        if (pendingSearch != null) {
            mainHandler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
        // Built-in matches right away (search on the name part, so "Tokyo, JP" still works).
        CityQuery query = CityQuery.parse(text);
        showResults(CityDatabase.search(query.name));
        if (query.name.length() < OpenMeteoGeocoder.MIN_QUERY_LENGTH) {
            statusText.setVisibility(View.GONE);
            return;
        }
        String language = Locale.getDefault().getLanguage();
        pendingSearch = () -> {
            pendingSearch = null;
            statusText.setText(R.string.city_searching);
            statusText.setVisibility(View.VISIBLE);
            executor.execute(() -> {
                List<City> found;
                try {
                    found = OpenMeteoGeocoder.search(text, language);
                } catch (IOException e) {
                    found = null;
                }
                List<City> result = found;
                mainHandler.post(() -> onSearchFinished(generation, result));
            });
        };
        mainHandler.postDelayed(pendingSearch, SEARCH_DELAY_MS);
    }

    /** {@code cities} is null when the lookup failed; the built-in matches are left showing. */
    private void onSearchFinished(int generation, List<City> cities) {
        if (generation != searchGeneration || isFinishing()) {
            return;  // The user kept typing; a newer search will report.
        }
        if (cities == null) {
            statusText.setText(R.string.city_search_offline);
            statusText.setVisibility(View.VISIBLE);
            return;
        }
        statusText.setVisibility(View.GONE);
        if (!cities.isEmpty()) {
            showResults(cities);  // Otherwise keep any built-in matches already shown.
        }
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
