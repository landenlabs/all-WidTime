// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.wear;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/** The phone asked for a reading now (its watch settings were opened or turned on). */
public class PhoneListener extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (WatchProtocol.PATH_REFRESH.equals(event.getPath())) {
            // Called on a background thread, so it may block while sending.
            ChargeWatch.check(this, true);
        }
    }
}
