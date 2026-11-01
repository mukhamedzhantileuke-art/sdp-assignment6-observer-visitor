package kz.aitu.sdp.assignment6.observer.listeners;

import kz.aitu.sdp.assignment6.observer.DefaultGridSubject;
import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridEventType;
import kz.aitu.sdp.assignment6.observer.GridObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Observer Chain Node: Acts as BOTH a listener (subscribing to raw METER_READING events)
 * AND a subject (publishing AGGREGATED_LOAD_REPORT events downstream).
 *
 * Implements the Observer Chain / Aggregator pattern required by the specification.
 */
public class RegionalLoadAggregator extends DefaultGridSubject implements GridObserver<GridEvent> {

    private final String observerId;
    private final int batchThreshold;
    private final Map<String, DoubleAdder> subtreeLoadMap;
    private final AtomicInteger currentBatchCount;
    private final AtomicLong aggregatedReportSequence;

    public RegionalLoadAggregator(String observerId, int batchThreshold) {
        super("RegionalLoadAggregatorSubject-" + observerId);
        this.observerId = observerId;
        this.batchThreshold = Math.max(1, batchThreshold);
        this.subtreeLoadMap = new ConcurrentHashMap<>();
        this.currentBatchCount = new AtomicInteger(0);
        this.aggregatedReportSequence = new AtomicLong(1);
    }

    @Override
    public void onEvent(GridEvent event) {
        String subtreeId = event.getTargetSubtreeId().isEmpty() ? "GENERAL-GRID" : event.getTargetSubtreeId();
        subtreeLoadMap.computeIfAbsent(subtreeId, k -> new DoubleAdder()).add(event.getMetricValue());

        int count = currentBatchCount.incrementAndGet();
        if (count >= batchThreshold) {
            publishAggregatedReport(subtreeId);
            currentBatchCount.set(0);
        }
    }

    /**
     * Publishes an aggregated event to all downstream observers registered for AGGREGATED_LOAD_REPORT.
     */
    public synchronized void publishAggregatedReport(String subtreeId) {
        DoubleAdder adder = subtreeLoadMap.get(subtreeId);
        double totalLoad = adder != null ? adder.sum() : 0.0;
        String eventId = "AGG-" + aggregatedReportSequence.getAndIncrement();
        GridEvent aggregatedEvent = GridEvent.of(
                eventId,
                GridEventType.AGGREGATED_LOAD_REPORT,
                observerId,
                subtreeId,
                totalLoad,
                String.format("Batch aggregation report for subtree %s: total net load = %.2f kW", subtreeId, totalLoad)
        );
        notifyObservers(aggregatedEvent);
    }

    /**
     * Flushes any remaining pending aggregated data for all subtrees.
     */
    public void flushAll() {
        for (String subtreeId : subtreeLoadMap.keySet()) {
            publishAggregatedReport(subtreeId);
        }
        currentBatchCount.set(0);
    }

    @Override
    public String getObserverId() {
        return observerId;
    }

    public double getAggregatedLoad(String subtreeId) {
        DoubleAdder adder = subtreeLoadMap.get(subtreeId);
        return adder != null ? adder.sum() : 0.0;
    }

    public int getBatchThreshold() {
        return batchThreshold;
    }
}
