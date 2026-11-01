package kz.aitu.sdp.assignment6.integration;

import kz.aitu.sdp.assignment6.Main;
import kz.aitu.sdp.assignment6.observer.DefaultGridSubject;
import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridEventType;
import kz.aitu.sdp.assignment6.observer.GridObserver;
import kz.aitu.sdp.assignment6.observer.sources.SubstationTelemetrySource;
import kz.aitu.sdp.assignment6.visitor.elements.PowerGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration Link Tests: Observer Event triggers Visitor on Subtree")
class IntegrationLinkTest {

    private PowerGrid grid;
    private SubstationTelemetrySource scadaSource;
    private DefaultGridSubject centralBus;
    private GridInspectorIntegrationService inspectorService;

    @BeforeEach
    void setUp() {
        grid = Main.buildSampleGrid();
        scadaSource = new SubstationTelemetrySource();
        centralBus = new DefaultGridSubject("CentralBus");
        inspectorService = new GridInspectorIntegrationService("INSPECTOR-01", grid, centralBus);

        scadaSource.subscribe(GridEventType.LOAD_SURGE, inspectorService);
        scadaSource.subscribe(GridEventType.VOLTAGE_SAG, inspectorService);
        scadaSource.subscribe(GridEventType.OUTAGE_ALERT, inspectorService);
    }

    @Test
    @DisplayName("Test 12: Telemetry event triggers inspection visitor strictly on the affected subtree")
    void testAlertTriggersInspectionOnSubtreeOnly() {
        assertEquals(0, inspectorService.getTicketCount());

        // Emit alert targeted at Feeder 101 only
        scadaSource.emitLoadSurge("SENSOR-F101", "LINE-FEEDER-101", 950.0, "Severe current surge detected");

        assertEquals(1, inspectorService.getTicketCount());
        MaintenanceTicket ticket = inspectorService.getCreatedTickets().get(0);

        assertEquals("LINE-FEEDER-101", ticket.targetSubtreeId());
        assertEquals(MaintenanceTicket.Severity.HIGH, ticket.severity());
        // In Feeder 101: Residential (6.5 + 4.2) - Solar (18.0) + Battery charging (10.0) = 2.7 kW (+ small loss)
        assertTrue(ticket.measuredNetLoadKw() > 0.0, "Measured load should reflect Feeder 101 elements");
    }

    @Test
    @DisplayName("Test 13: Generated maintenance ticket is republished as a new event into the event bus")
    void testMaintenanceTicketRepublishedAsEvent() {
        AtomicReference<GridEvent> capturedTicketEvent = new AtomicReference<>();

        // Register a dispatcher listener to listen to MAINTENANCE_TICKET_CREATED
        centralBus.subscribe(GridEventType.MAINTENANCE_TICKET_CREATED, new GridObserver<>() {
            @Override
            public void onEvent(GridEvent event) {
                capturedTicketEvent.set(event);
            }

            @Override
            public String getObserverId() {
                return "DISPATCH-CREW-LISTENER";
            }
        });

        // Trigger an outage alert on Substation 2
        scadaSource.emitOutageAlert("BREAKER-S2", "SUB-MEDEU-02", "Busbar insulator flashover");

        GridEvent ticketEvent = capturedTicketEvent.get();
        assertNotNull(ticketEvent, "Maintenance ticket event must be republished back into system");
        assertEquals(GridEventType.MAINTENANCE_TICKET_CREATED, ticketEvent.getType());
        assertEquals("SUB-MEDEU-02", ticketEvent.getTargetSubtreeId());
        assertTrue(ticketEvent.getMessage().contains("Busbar insulator flashover"));
    }

    @Test
    @DisplayName("Test 14: Non-existent target subtree does not throw exceptions")
    void testNonExistentSubtreeGracefullyHandled() {
        scadaSource.emitLoadSurge("UNKNOWN-SENSOR", "LINE-GHOST-999", 500.0, "Ghost line alert");
        assertEquals(0, inspectorService.getTicketCount(), "No ticket should be created for non-existent subtree");
    }
}
