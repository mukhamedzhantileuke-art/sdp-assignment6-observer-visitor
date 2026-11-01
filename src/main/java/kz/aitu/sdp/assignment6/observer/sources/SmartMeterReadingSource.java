package kz.aitu.sdp.assignment6.observer.sources;

import kz.aitu.sdp.assignment6.observer.DefaultGridSubject;
import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridEventType;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Data source 1: Smart meter telemetry feed that captures active power consumption,
 * voltage levels, and power factors from connected consumers.
 */
public class SmartMeterReadingSource extends DefaultGridSubject {

    private final AtomicLong sequenceCounter = new AtomicLong(1);

    public SmartMeterReadingSource() {
        super("SmartMeterTelemetryFeed");
    }

    public void emitMeterReading(String meterId, String targetSubtreeId, double activePowerKw, String message) {
        String eventId = "MTR-" + sequenceCounter.getAndIncrement();
        GridEvent event = GridEvent.of(eventId, GridEventType.METER_READING, meterId, targetSubtreeId, activePowerKw, message);
        notifyObservers(event);
    }
}
