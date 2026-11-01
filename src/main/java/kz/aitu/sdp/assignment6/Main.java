package kz.aitu.sdp.assignment6;

import kz.aitu.sdp.assignment6.concurrency.GridSimulationEngine;
import kz.aitu.sdp.assignment6.concurrency.RenewableAnomalyProducer;
import kz.aitu.sdp.assignment6.concurrency.SmartMeterReadingProducer;
import kz.aitu.sdp.assignment6.concurrency.SubstationTelemetryProducer;
import kz.aitu.sdp.assignment6.integration.GridInspectorIntegrationService;
import kz.aitu.sdp.assignment6.integration.MaintenanceTicket;
import kz.aitu.sdp.assignment6.observer.DefaultGridSubject;
import kz.aitu.sdp.assignment6.observer.GridEventType;
import kz.aitu.sdp.assignment6.observer.listeners.*;
import kz.aitu.sdp.assignment6.observer.sources.SmartMeterReadingSource;
import kz.aitu.sdp.assignment6.observer.sources.SubstationTelemetrySource;
import kz.aitu.sdp.assignment6.visitor.elements.*;
import kz.aitu.sdp.assignment6.visitor.visitors.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Composition Root (Clean Code chapters 11–12):
 * Bootstraps the Energy Grid system using pure constructor injection.
 * Assembles the composite grid hierarchy, registers observer subscriptions,
 * executes visitor operations, triggers integration inspections, and coordinates
 * multi-threaded producers on an ExecutorService with graceful shutdown.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Energy Grid Smart Management System (Assignment 6)");
        System.out.println("Patterns: Observer & Visitor | Clean Code ch. 11-13\n");

        // 1. Build Composite Tree (Level 1: Grid -> Level 2: Substation -> Level 3: Line -> Level 4: Leaves)
        PowerGrid grid = buildSampleGrid();

        // 2. Demonstrate Visitor Pattern Operations
        System.out.println("--- 1. VISITOR OPERATIONS ON GRID TREE ---");

        // Visitor 1: Numeric Calculation
        NetLoadCalculationVisitor netLoadVisitor = new NetLoadCalculationVisitor();
        double totalNetLoad = grid.accept(netLoadVisitor);
        System.out.printf("Visitor 1 [Net Load Calculation]: Total Active Grid Load = %.2f kW\n", totalNetLoad);

        // Visitor 2: State Accumulator across Whole Tree
        GridHealthAuditVisitor healthVisitor = new GridHealthAuditVisitor();
        GridHealthReport report = grid.accept(healthVisitor);
        System.out.printf("Visitor 2 [Health Audit]: Assets=%d (Subs=%d, Lines=%d, Consumers=%d, Solar=%d, Storage=%d, EV=%d)\n",
                report.getTotalAssets(), report.totalSubstations(), report.totalDistributionLines(),
                report.totalResidentialConsumers() + report.totalIndustrialConsumers(),
                report.totalSolarGenerators(), report.totalBatteryStorages(), report.totalEvStations());
        System.out.printf("           Total Demand: %.2f kW | Renewable: %.2f kW | Battery Capacity: %.2f kWh | Low Battery Count: %d\n",
                report.totalDemandKw(), report.totalRenewableGenerationKw(), report.totalBatteryCapacityKwh(), report.lowBatteryStorageCount());

        // Visitor 3: Filter Returning List of Elements
        OverloadedComponentFilterVisitor filterVisitor = new OverloadedComponentFilterVisitor();
        List<GridElement> flagged = grid.accept(filterVisitor);
        System.out.printf("Visitor 3 [Hazard Filter]: Found %d flagged/overloaded components: %s\n",
                flagged.size(), flagged.stream().map(GridElement::getId).toList());

        // Visitor 4: Hand-written JSON Export with correct nesting
        GridJsonExportVisitor jsonVisitor = new GridJsonExportVisitor();
        String jsonExport = grid.accept(jsonVisitor);
        System.out.println("Visitor 4 [JSON Export Preview (first 15 lines)]:");
        jsonExport.lines().limit(15).forEach(line -> System.out.println("  " + line));
        System.out.println("  ... (truncated JSON output)\n");

        // 3. Assemble Observer Pattern Infrastructure
        System.out.println("--- 2. OBSERVER PATTERN & AGGREGATOR CHAIN ---");

        SmartMeterReadingSource meterSource = new SmartMeterReadingSource();
        SubstationTelemetrySource scadaSource = new SubstationTelemetrySource();
        DefaultGridSubject integrationEventBus = new DefaultGridSubject("CentralIntegrationBus");

        // Listeners
        BillingServiceListener billingService = new BillingServiceListener("BILLING-SVC", 0.14);
        GridLoadBalancerListener loadBalancer = new GridLoadBalancerListener("BALANCER-SVC", 800.0);
        OutageAlertServiceListener outageService = new OutageAlertServiceListener("OUTAGE-SVC");
        ConsumptionStatisticsTrackerListener statsTracker = new ConsumptionStatisticsTrackerListener("STATS-SVC");
        RegionalLoadAggregator loadAggregator = new RegionalLoadAggregator("REGIONAL-AGG-01", 5);

        // Register Subscriptions
        meterSource.subscribe(GridEventType.METER_READING, billingService);
        meterSource.subscribe(GridEventType.METER_READING, statsTracker);
        meterSource.subscribe(GridEventType.METER_READING, loadAggregator);

        // Observer Chain: Downstream listeners subscribe to Aggregator
        loadAggregator.subscribe(GridEventType.AGGREGATED_LOAD_REPORT, loadBalancer);
        loadAggregator.subscribe(GridEventType.AGGREGATED_LOAD_REPORT, statsTracker);

        scadaSource.subscribe(GridEventType.LOAD_SURGE, loadBalancer);
        scadaSource.subscribe(GridEventType.VOLTAGE_SAG, outageService);
        scadaSource.subscribe(GridEventType.OUTAGE_ALERT, outageService);

        // 4. Integration Link: Observer triggers Visitor on Subtree only
        GridInspectorIntegrationService inspectorService = new GridInspectorIntegrationService(
                "INSPECTOR-INTEGRATION-SVC", grid, integrationEventBus);
        scadaSource.subscribe(GridEventType.LOAD_SURGE, inspectorService);
        scadaSource.subscribe(GridEventType.VOLTAGE_SAG, inspectorService);
        scadaSource.subscribe(GridEventType.OUTAGE_ALERT, inspectorService);

        // Dynamic unsubscription verification: listener removes itself from inside handler
        meterSource.subscribe(GridEventType.METER_READING, new kz.aitu.sdp.assignment6.observer.GridObserver<>() {
            @Override
            public void onEvent(kz.aitu.sdp.assignment6.observer.GridEvent event) {
                // Safely unsubscribe from within handler
                meterSource.unsubscribe(GridEventType.METER_READING, this);
            }

            @Override
            public String getObserverId() {
                return "SELF-UNSUBSCRIBING-ONE-SHOT-LISTENER";
            }
        });

        // 5. Run Concurrency Producers on ExecutorService (Clean Code ch. 13)
        System.out.println("--- 3. MULTI-THREADED PRODUCERS & CONCURRENCY ---");
        int threadPoolSize = 3;
        GridSimulationEngine engine = new GridSimulationEngine(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(3);

        engine.submitProducer(new SmartMeterReadingProducer(meterSource, 50, "LINE-FEEDER-101", latch));
        engine.submitProducer(new SubstationTelemetryProducer(scadaSource, 20, "SUB-ALATAU-01", latch));
        engine.submitProducer(new RenewableAnomalyProducer(scadaSource, 20, "LINE-FEEDER-101", latch));

        try {
            latch.await();
            loadAggregator.flushAll(); // Flush aggregator queue
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean terminatedCleanly = engine.shutdownGracefully(5000);
        System.out.printf("Producers finished. Clean graceful shutdown: %b\n", terminatedCleanly);

        // 6. Summary Output
        System.out.println("\n--- 4. POST-RUN SYSTEM AUDIT ---");
        System.out.printf("Total Billed Revenue: $%.2f across %d meter readings\n",
                billingService.getTotalBilledRevenue(), billingService.getProcessedReadingsCount());
        System.out.printf("Load Balancer Actions: %d interventions (Total Curtailed: %.2f kW)\n",
                loadBalancer.getBalancingActionCount(), loadBalancer.getTotalCurtailedLoadKw());
        System.out.printf("Outage Service Incidents: %d (Outages: %d, Sags: %d)\n",
                outageService.getTotalIncidentCount(), outageService.getOutageAlertCount(), outageService.getVoltageSagCount());
        System.out.printf("Statistics Tracker: Read %d readings, Average Power = %.2f kW, Peak = %.2f kW\n",
                statsTracker.getReadingCount(), statsTracker.getAveragePowerKw(), statsTracker.getPeakPowerKw());
        System.out.printf("Integration Inspector Tickets Generated: %d\n", inspectorService.getTicketCount());

        for (MaintenanceTicket ticket : inspectorService.getCreatedTickets()) {
            System.out.printf("  -> Ticket %s [%s] on %s: %s\n",
                    ticket.ticketId(), ticket.severity(), ticket.targetSubtreeId(), ticket.diagnosis());
        }

        System.out.println("\nSystem execution completed successfully.");
    }

    /**
     * Builds a realistic 4-level composite tree:
     * Level 1: PowerGrid (Regional transmission)
     * Level 2: Substation (Containers 1 & 2)
     * Level 3: DistributionLine (Feeders)
     * Level 4: Leaves (ResidentialConsumer, IndustrialConsumer, SolarGenerator, BatteryStorage, EvChargingStation)
     */
    public static PowerGrid buildSampleGrid() {
        PowerGrid grid = new PowerGrid("GRID-ALMATY-NORTH", "Northern Almaty Regional Grid", "Almaty Metropolitan");

        // Substation 1
        Substation sub1 = new Substation("SUB-ALATAU-01", "Alatau Step-down Substation", 63.0, 110.0, 10.0);

        DistributionLine feeder101 = new DistributionLine("LINE-FEEDER-101", "Feeder Line 101 (Microdistrict)", 0.25, 400.0, 10.0, 4.5);
        feeder101.addElement(new ResidentialConsumer("RES-001", "Apartment Complex A", "MTR-1001", "Abay Ave 45", 6.5, 0.95));
        feeder101.addElement(new ResidentialConsumer("RES-002", "Apartment Complex B", "MTR-1002", "Abay Ave 47", 4.2, 0.92));
        feeder101.addElement(new SolarGenerator("SOLAR-001", "Community Rooftop Solar", 25.0, 18.0, 0.96, true));
        feeder101.addElement(new BatteryStorage("BAT-001", "Feeder Battery Storage", 100.0, 65.0, 10.0, BatteryStorage.OperatingMode.CHARGING));

        DistributionLine feeder102 = new DistributionLine("LINE-FEEDER-102", "Feeder Line 102 (Industrial)", 0.15, 600.0, 10.0, 8.0);
        feeder102.addElement(new IndustrialConsumer("IND-001", "Almaty Metal Works", "Metallurgy", 450.0, 410.0, 0.88));
        feeder102.addElement(new SolarGenerator("SOLAR-002", "Industrial Solar Park", 50.0, 35.0, 0.97, true));

        sub1.addLine(feeder101);
        sub1.addLine(feeder102);

        // Substation 2
        Substation sub2 = new Substation("SUB-MEDEU-02", "Medeu Mountain Substation", 40.0, 110.0, 10.0);
        DistributionLine feeder201 = new DistributionLine("LINE-FEEDER-201", "Feeder Line 201 (Foothills)", 0.35, 300.0, 10.0, 6.2);
        feeder201.addElement(new ResidentialConsumer("RES-003", "Cottage Settlement Medeu", "MTR-2001", "Gornaya St 12", 8.1, 0.94));
        feeder201.addElement(new BatteryStorage("BAT-002", "Substation Emergency Battery", 80.0, 10.0, 15.0, BatteryStorage.OperatingMode.DISCHARGING)); // Low charge (<15%)

        sub2.addLine(feeder201);

        grid.addSubstation(sub1);
        grid.addSubstation(sub2);

        return grid;
    }
}
