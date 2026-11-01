package kz.aitu.sdp.assignment6.observer.listeners;

import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridObserver;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Listener 1: Calculates consumer billing based on smart meter readings and dynamic tariffs.
 */
public class BillingServiceListener implements GridObserver<GridEvent> {

    private final String observerId;
    private final double tariffPerKwh;
    private final Map<String, DoubleAdder> consumerTotals;
    private final LongAdder processedReadings;

    public BillingServiceListener(String observerId, double tariffPerKwh) {
        this.observerId = observerId;
        this.tariffPerKwh = tariffPerKwh;
        this.consumerTotals = new ConcurrentHashMap<>();
        this.processedReadings = new LongAdder();
    }

    @Override
    public void onEvent(GridEvent event) {
        double powerKw = event.getMetricValue();
        // Assuming 1-hour interval equivalent for simplified energy calculation
        double billAmount = powerKw * tariffPerKwh;
        consumerTotals.computeIfAbsent(event.getSourceId(), k -> new DoubleAdder()).add(billAmount);
        processedReadings.increment();
    }

    @Override
    public String getObserverId() {
        return observerId;
    }

    public double getBilledAmount(String consumerId) {
        DoubleAdder adder = consumerTotals.get(consumerId);
        return adder != null ? adder.sum() : 0.0;
    }

    public double getTotalBilledRevenue() {
        return consumerTotals.values().stream().mapToDouble(DoubleAdder::sum).sum();
    }

    public long getProcessedReadingsCount() {
        return processedReadings.sum();
    }

    public Map<String, DoubleAdder> getConsumerTotals() {
        return Collections.unmodifiableMap(consumerTotals);
    }
}
