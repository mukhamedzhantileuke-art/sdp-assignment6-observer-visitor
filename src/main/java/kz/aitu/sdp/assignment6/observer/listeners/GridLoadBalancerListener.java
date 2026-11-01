package kz.aitu.sdp.assignment6.observer.listeners;

import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridObserver;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Listener 2: Monitors regional surges and aggregated loads to initiate automated load balancing.
 */
public class GridLoadBalancerListener implements GridObserver<GridEvent> {

    private final String observerId;
    private final double overloadThresholdKw;
    private final AtomicInteger balancingActionCount;
    private final DoubleAdder totalCurtailedLoadKw;

    public GridLoadBalancerListener(String observerId, double overloadThresholdKw) {
        this.observerId = observerId;
        this.overloadThresholdKw = overloadThresholdKw;
        this.balancingActionCount = new AtomicInteger(0);
        this.totalCurtailedLoadKw = new DoubleAdder();
    }

    @Override
    public void onEvent(GridEvent event) {
        double currentLoad = event.getMetricValue();
        if (currentLoad > overloadThresholdKw) {
            double excess = currentLoad - overloadThresholdKw;
            totalCurtailedLoadKw.add(excess);
            balancingActionCount.incrementAndGet();
        }
    }

    @Override
    public String getObserverId() {
        return observerId;
    }

    public int getBalancingActionCount() {
        return balancingActionCount.get();
    }

    public double getTotalCurtailedLoadKw() {
        return totalCurtailedLoadKw.sum();
    }

    public double getOverloadThresholdKw() {
        return overloadThresholdKw;
    }
}
