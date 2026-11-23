// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** The while-charging poll alarm (see ChargeWatch). */
public class PollReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult result = goAsync();
        new Thread(() -> {
            try {
                ChargeWatch.check(context, false);
            } finally {
                result.finish();
            }
        }, "watch-poll").start();
    }
}
