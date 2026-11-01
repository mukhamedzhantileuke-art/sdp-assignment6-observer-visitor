package kz.aitu.sdp.assignment6.observer.sources;

import kz.aitu.sdp.assignment6.observer.DefaultGridSubject;
import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridEventType;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Data source 2: Substation sensory feed monitoring transformer health,
 * line loads, bus voltages, and breaker status.
 */
public class SubstationTelemetrySource extends DefaultGridSubject {

    private final AtomicLong sequenceCounter = new AtomicLong(1);

    public SubstationTelemetrySource() {
        super("SubstationTelemetryFeed");
    }

    public void emitLoadSurge(String sensorId, String targetSubtreeId, double peakLoadKw, String warning) {
        String eventId = "SURGE-" + sequenceCounter.getAndIncrement();
        notifyObservers(GridEvent.of(eventId, GridEventType.LOAD_SURGE, sensorId, targetSubtreeId, peakLoadKw, warning));
    }

    public void emitVoltageSag(String sensorId, String targetSubtreeId, double voltageLevel, String warning) {
        String eventId = "SAG-" + sequenceCounter.getAndIncrement();
        notifyObservers(GridEvent.of(eventId, GridEventType.VOLTAGE_SAG, sensorId, targetSubtreeId, voltageLevel, warning));
    }

    public void emitOutageAlert(String sensorId, String targetSubtreeId, String faultDescription) {
        String eventId = "OUTAGE-" + sequenceCounter.getAndIncrement();
        notifyObservers(GridEvent.of(eventId, GridEventType.OUTAGE_ALERT, sensorId, targetSubtreeId, 0.0, faultDescription));
    }
}
