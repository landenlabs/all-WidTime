// ----------------------------------------------------------------------
// Copyright (c) 2026 LanDen Labs - Dennis Lang
// https://landenlabs.com
// ----------------------------------------------------------------------

package com.landenlabs.widtime.wear;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Runs a check for the "put on charger" job and the 30 minute periodic job (see ChargeWatch). */
public class ChargeJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            BatteryState state = ChargeWatch.check(this, false);
            // The charger job ran but the watch is off the charger again (a brief contact):
            // keep it, so it fires on the next real charge.
            boolean keep = params.getJobId() == ChargeWatch.JOB_CHARGER && !state.charging;
            jobFinished(params, keep);
        }, "watch-check").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // The charger job stops when the charger is pulled: keep it for the next charge.
        return params.getJobId() == ChargeWatch.JOB_CHARGER;
    }
}
