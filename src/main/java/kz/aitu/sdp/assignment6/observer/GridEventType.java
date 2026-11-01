package kz.aitu.sdp.assignment6.observer;

/**
 * Categorizes domain events occurring within the electrical smart grid.
 */
public enum GridEventType {
    METER_READING,
    LOAD_SURGE,
    OUTAGE_ALERT,
    VOLTAGE_SAG,
    AGGREGATED_LOAD_REPORT,
    MAINTENANCE_TICKET_CREATED
}
