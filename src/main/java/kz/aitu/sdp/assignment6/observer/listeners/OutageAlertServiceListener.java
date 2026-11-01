package kz.aitu.sdp.assignment6.observer.listeners;

import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listener 3: Emergency response service tracking line trips, outages, and voltage sags.
 */
public class OutageAlertServiceListener implements GridObserver<GridEvent> {

    private final String observerId;
    private final AtomicInteger outageAlertCount;
    private final AtomicInteger voltageSagCount;
    private final List<GridEvent> activeIncidents;

    public OutageAlertServiceListener(String observerId) {
        this.observerId = observerId;
        this.outageAlertCount = new AtomicInteger(0);
        this.voltageSagCount = new AtomicInteger(0);
        this.activeIncidents = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onEvent(GridEvent event) {
        activeIncidents.add(event);
        switch (event.getType()) {
            case OUTAGE_ALERT -> outageAlertCount.incrementAndGet();
            case VOLTAGE_SAG -> voltageSagCount.incrementAndGet();
            default -> { /* ignore non-incident events */ }
        }
    }

    @Override
    public String getObserverId() {
        return observerId;
    }

    public int getOutageAlertCount() {
        return outageAlertCount.get();
    }

    public int getVoltageSagCount() {
        return voltageSagCount.get();
    }

    public int getTotalIncidentCount() {
        return outageAlertCount.get() + voltageSagCount.get();
    }

    public List<GridEvent> getActiveIncidents() {
        return List.copyOf(activeIncidents);
    }
}
