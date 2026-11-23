// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.watch;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Started by Google Play services when the watch app sends a battery reading, so the phone app
 * needs no running service for it.
 */
public class WatchBatteryListener extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event : events) {
            if (event.getType() != DataEvent.TYPE_CHANGED
                    || !WatchProtocol.PATH_BATTERY.equals(event.getDataItem().getUri().getPath())) {
                continue;
            }
            DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
            WatchBattery.onReport(this,
                    map.getInt(WatchProtocol.KEY_LEVEL, -1),
                    map.getBoolean(WatchProtocol.KEY_CHARGING, false),
                    map.getBoolean(WatchProtocol.KEY_FULL, false),
                    map.getLong(WatchProtocol.KEY_TIME, System.currentTimeMillis()));
        }
    }
}
