package com.dennislang.widtime.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dennislang.widtime.R;
import com.dennislang.widtime.alerts.ChargeAlerts;

/** "Charger disconnected" screen with the button that stops the repeating unplug alarm. */
public class AlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setContentView(R.layout.activity_alarm);

        Button stop = findViewById(R.id.stop_alarm_button);
        if (!ChargeAlerts.isAlarmActive(this)) {
            ((TextView) findViewById(R.id.alarm_message)).setText(R.string.alarm_not_active);
            stop.setText(R.string.close);
        }
        stop.setOnClickListener(v -> {
            ChargeAlerts.stopAlarm(this);
            finish();
        });
    }
}
