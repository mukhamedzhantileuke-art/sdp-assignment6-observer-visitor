package kz.aitu.sdp.assignment6.integration;

import kz.aitu.sdp.assignment6.observer.DefaultGridSubject;
import kz.aitu.sdp.assignment6.observer.GridEvent;
import kz.aitu.sdp.assignment6.observer.GridEventType;
import kz.aitu.sdp.assignment6.observer.GridObserver;
import kz.aitu.sdp.assignment6.visitor.elements.DistributionLine;
import kz.aitu.sdp.assignment6.visitor.elements.GridElement;
import kz.aitu.sdp.assignment6.visitor.elements.PowerGrid;
import kz.aitu.sdp.assignment6.visitor.elements.Substation;
import kz.aitu.sdp.assignment6.visitor.visitors.NetLoadCalculationVisitor;
import kz.aitu.sdp.assignment6.visitor.visitors.OverloadedComponentFilterVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Integration Bridge between Observer (Part A) and Visitor (Part B).
 *
 * An incoming critical event (LOAD_SURGE, VOLTAGE_SAG, OUTAGE_ALERT) triggers
 * inspection visitors strictly on the affected subtree (substation or feeder line).
 * The resulting findings are compiled into a {@link MaintenanceTicket} and republished
 * back into the grid subject as a MAINTENANCE_TICKET_CREATED event.
 */
public class GridInspectorIntegrationService implements GridObserver<GridEvent> {

    private final String observerId;
    private final PowerGrid powerGrid;
    private final DefaultGridSubject eventPublisher;
    private final List<MaintenanceTicket> createdTickets;
    private final AtomicLong ticketSequence;

    public GridInspectorIntegrationService(String observerId,
                                           PowerGrid powerGrid,
                                           DefaultGridSubject eventPublisher) {
        this.observerId = Objects.requireNonNull(observerId, "observerId cannot be null");
        this.powerGrid = Objects.requireNonNull(powerGrid, "powerGrid cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
        this.createdTickets = new CopyOnWriteArrayList<>();
        this.ticketSequence = new AtomicLong(1);
    }

    @Override
    public void onEvent(GridEvent event) {
        String targetSubtreeId = event.getTargetSubtreeId();
        if (targetSubtreeId == null || targetSubtreeId.isBlank()) {
            return;
        }

        GridElement targetSubtree = findSubtree(targetSubtreeId);
        if (targetSubtree == null) {
            // Target subtree not found; skip local inspection
            return;
        }

        // 1. Run Numeric Calculation Visitor on the target subtree ONLY
        NetLoadCalculationVisitor loadVisitor = new NetLoadCalculationVisitor();
        double measuredSubtreeLoad = targetSubtree.accept(loadVisitor);

        // 2. Run Overloaded Filter Visitor on the target subtree ONLY
        OverloadedComponentFilterVisitor filterVisitor = new OverloadedComponentFilterVisitor();
        List<GridElement> hazardousElements = targetSubtree.accept(filterVisitor);

        // Determine ticket severity based on telemetry event and visitor findings
        MaintenanceTicket.Severity severity;
        if (event.getType() == GridEventType.OUTAGE_ALERT || !hazardousElements.isEmpty()) {
            severity = MaintenanceTicket.Severity.CRITICAL;
        } else if (event.getType() == GridEventType.LOAD_SURGE) {
            severity = MaintenanceTicket.Severity.HIGH;
        } else {
            severity = MaintenanceTicket.Severity.MEDIUM;
        }

        List<String> flaggedIds = hazardousElements.stream()
                .map(GridElement::getId)
                .toList();

        String ticketId = "TCK-" + ticketSequence.getAndIncrement();
        String diagnosis = String.format("Telemetry [%s] triggered inspection on [%s]. Net load: %.2f kW. Flagged components: %s. Event msg: %s",
                event.getType(), targetSubtreeId, measuredSubtreeLoad, flaggedIds, event.getMessage());

        MaintenanceTicket ticket = new MaintenanceTicket(
                ticketId,
                targetSubtreeId,
                severity,
                measuredSubtreeLoad,
                flaggedIds,
                diagnosis,
                System.currentTimeMillis()
        );

        createdTickets.add(ticket);

        // 3. Republish the maintenance ticket as a new event into the subject
        GridEvent ticketEvent = GridEvent.of(
                ticketId,
                GridEventType.MAINTENANCE_TICKET_CREATED,
                observerId,
                targetSubtreeId,
                measuredSubtreeLoad,
                diagnosis
        );
        eventPublisher.notifyObservers(ticketEvent);
    }

    /**
     * Locates a Substation or DistributionLine within the grid by identifier.
     */
    private GridElement findSubtree(String targetId) {
        if (powerGrid.getId().equals(targetId)) {
            return powerGrid;
        }
        for (Substation sub : powerGrid.getSubstations()) {
            if (sub.getId().equals(targetId)) {
                return sub;
            }
            for (DistributionLine line : sub.getLines()) {
                if (line.getId().equals(targetId)) {
                    return line;
                }
            }
        }
        return null;
    }

    @Override
    public String getObserverId() {
        return observerId;
    }

    public List<MaintenanceTicket> getCreatedTickets() {
        return Collections.unmodifiableList(createdTickets);
    }

    public int getTicketCount() {
        return createdTickets.size();
    }
}
