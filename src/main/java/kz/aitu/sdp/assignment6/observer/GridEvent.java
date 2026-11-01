package kz.aitu.sdp.assignment6.observer;

import java.util.Objects;

/**
 * Immutable domain event representing telemetry readings, alerts, or aggregated grid metrics.
 * Adheres to Clean Code chapter 11 by isolating state into an unmodifiable value object.
 */
public final class GridEvent {
    private final String eventId;
    private final GridEventType type;
    private final long timestamp;
    private final String sourceId;
    private final String targetSubtreeId;
    private final double metricValue;
    private final String message;

    public GridEvent(String eventId,
                     GridEventType type,
                     long timestamp,
                     String sourceId,
                     String targetSubtreeId,
                     double metricValue,
                     String message) {
        this.eventId = Objects.requireNonNull(eventId, "eventId cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.timestamp = timestamp;
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId cannot be null");
        this.targetSubtreeId = targetSubtreeId != null ? targetSubtreeId : "";
        this.metricValue = metricValue;
        this.message = message != null ? message : "";
    }

    public static GridEvent of(String eventId,
                               GridEventType type,
                               String sourceId,
                               String targetSubtreeId,
                               double metricValue,
                               String message) {
        return new GridEvent(eventId, type, System.currentTimeMillis(), sourceId, targetSubtreeId, metricValue, message);
    }

    public String getEventId() {
        return eventId;
    }

    public GridEventType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetSubtreeId() {
        return targetSubtreeId;
    }

    public double getMetricValue() {
        return metricValue;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridEvent other)) return false;
        return timestamp == other.timestamp &&
                Double.compare(other.metricValue, metricValue) == 0 &&
                eventId.equals(other.eventId) &&
                type == other.type &&
                sourceId.equals(other.sourceId) &&
                targetSubtreeId.equals(other.targetSubtreeId) &&
                message.equals(other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, type, timestamp, sourceId, targetSubtreeId, metricValue, message);
    }

    @Override
    public String toString() {
        return String.format("GridEvent{id='%s', type=%s, src='%s', subtree='%s', val=%.2f, msg='%s'}",
                eventId, type, sourceId, targetSubtreeId, metricValue, message);
    }
}
