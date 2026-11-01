package kz.aitu.sdp.assignment6.observer.listeners;

import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridObserver;

import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Listener 4: Accumulates real-time consumption statistics (peak load, total energy, mean load).
 */
public class ConsumptionStatisticsTrackerListener implements GridObserver<GridEvent> {

    private final String observerId;
    private final LongAdder readingCount;
    private final DoubleAdder totalEnergyKwh;
    private final DoubleAccumulator peakPowerKw;
    private final DoubleAccumulator minPowerKw;

    public ConsumptionStatisticsTrackerListener(String observerId) {
        this.observerId = observerId;
        this.readingCount = new LongAdder();
        this.totalEnergyKwh = new DoubleAdder();
        this.peakPowerKw = new DoubleAccumulator(Math::max, Double.MIN_VALUE);
        this.minPowerKw = new DoubleAccumulator(Math::min, Double.MAX_VALUE);
    }

    @Override
    public void onEvent(GridEvent event) {
        double power = event.getMetricValue();
        readingCount.increment();
        totalEnergyKwh.add(power);
        peakPowerKw.accumulate(power);
        minPowerKw.accumulate(power);
    }

    @Override
    public String getObserverId() {
        return observerId;
    }

    public long getReadingCount() {
        return readingCount.sum();
    }

    public double getTotalEnergyKwh() {
        return totalEnergyKwh.sum();
    }

    public double getPeakPowerKw() {
        double peak = peakPowerKw.get();
        return peak == Double.MIN_VALUE ? 0.0 : peak;
    }

    public double getMinPowerKw() {
        double min = minPowerKw.get();
        return min == Double.MAX_VALUE ? 0.0 : min;
    }

    public double getAveragePowerKw() {
        long count = readingCount.sum();
        return count > 0 ? totalEnergyKwh.sum() / count : 0.0;
    }
}
