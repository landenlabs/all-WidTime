// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.wear;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The watch app's only screen: the battery reading it sends to the phone. Opening it once after
 * install is what arms the charger job (a newly installed app can't schedule anything until
 * it has been started).
 */
public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inset by 15% each side, so the text fits a round screen.
        int inset = (int) (getResources().getDisplayMetrics().widthPixels * 0.15f);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        column.setPadding(inset, inset, inset, inset);

        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        status.setTextSize(22);
        column.addView(status);

        TextView footer = new TextView(this);
        footer.setGravity(Gravity.CENTER);
        footer.setTextSize(12);
        footer.setPadding(0, inset / 3, 0, 0);
        footer.setText(R.string.status_footer);
        column.addView(footer);

        FrameLayout root = new FrameLayout(this);
        root.addView(column, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        show(BatteryState.read(this));
        new Thread(() -> {
            BatteryState state = ChargeWatch.check(this, true);
            runOnUiThread(() -> show(state));
        }, "watch-open").start();
    }

    private void show(BatteryState state) {
        int level = Math.max(state.level, 0);
        int text = state.full ? R.string.status_full
                : state.charging ? R.string.status_charging : R.string.status_battery;
        status.setText(getString(text, level));
    }
}
