package com.landenlabs.widtime.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DischargeEstimatorTest {

    private static final long MIN = 60_000L;
    private static final long HOUR = 60 * MIN;

    @Test
    public void colorThresholds() {
        assertEquals(DischargeEstimator.COLOR_GREEN, DischargeEstimator.colorForPercent(61));
        assertEquals(DischargeEstimator.COLOR_YELLOW, DischargeEstimator.colorForPercent(60));
        assertEquals(DischargeEstimator.COLOR_YELLOW, DischargeEstimator.colorForPercent(40));
        assertEquals(DischargeEstimator.COLOR_RED, DischargeEstimator.colorForPercent(39.9));
    }

    @Test
    public void percentOfFullScaleClampsAt24h() {
        assertEquals(50.0, DischargeEstimator.percentOfFullScale(12), 1e-9);
        assertEquals(100.0, DischargeEstimator.percentOfFullScale(30), 1e-9);
        assertEquals(0.0, DischargeEstimator.percentOfFullScale(-1), 1e-9);
    }

    @Test
    public void rateFromSteadyDrain() {
        List<long[]> s = new ArrayList<>();
        // 1% every 15 minutes = 4%/h, starting at 80%.
        for (int i = 0; i <= 8; i++) {
            DischargeEstimator.addSample(s, i * 15 * MIN, 80 - i);
        }
        double rate = DischargeEstimator.ratePercentPerHour(s);
        assertEquals(4.0, rate, 1e-9);
        assertEquals(18.0, DischargeEstimator.hoursLeft(72, rate), 1e-9);
    }

    @Test
    public void notTrustedUntilEnoughDrop() {
        List<long[]> s = new ArrayList<>();
        DischargeEstimator.addSample(s, 0, 90);
        DischargeEstimator.addSample(s, 30 * MIN, 89);
        assertTrue(Double.isNaN(DischargeEstimator.ratePercentPerHour(s)));
        assertTrue(Double.isNaN(DischargeEstimator.hoursLeft(89, Double.NaN)));
    }

    @Test
    public void duplicateLevelsIgnoredAndRiseResets() {
        List<long[]> s = new ArrayList<>();
        DischargeEstimator.addSample(s, 0, 50);
        DischargeEstimator.addSample(s, MIN, 50);
        assertEquals(1, s.size());
        DischargeEstimator.addSample(s, 2 * MIN, 49);
        DischargeEstimator.addSample(s, 3 * MIN, 70);
        assertEquals(1, s.size());
        assertEquals(70, s.get(0)[1]);
    }

    @Test
    public void oldSamplesPrunedButTwoKept() {
        List<long[]> s = new ArrayList<>();
        DischargeEstimator.addSample(s, 0, 90);
        DischargeEstimator.addSample(s, HOUR, 89);
        DischargeEstimator.addSample(s, 10 * HOUR, 85);
        assertEquals(2, s.size());
        assertEquals(89, s.get(0)[1]);
        // 89 -> 85 over 9 h
        assertEquals(4.0 / 9.0, DischargeEstimator.ratePercentPerHour(s), 1e-9);
    }
}
