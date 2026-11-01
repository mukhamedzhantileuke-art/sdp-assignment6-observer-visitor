package kz.aitu.sdp.assignment6.visitor.visitors;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;
import kz.aitu.sdp.assignment6.visitor.elements.*;

/**
 * Visitor 2 (State Accumulator across Whole Tree):
 * Traverses the entire composite grid structure and accumulates detailed system health metrics,
 * component counts, aggregate capacities, and anomaly indicators into a {@link GridHealthReport}.
 */
public class GridHealthAuditVisitor implements GridVisitor<GridHealthReport> {

    private int substations;
    private int lines;
    private int residential;
    private int industrial;
    private int solarGenerators;
    private int batteryStorages;
    private int evStations;

    private double totalDemandKw;
    private double totalRenewableGenerationKw;
    private double totalBatteryCapacityKwh;
    private double currentBatteryStorageKwh;

    private int offlineGenerators;
    private int lowBatteryCount;

    @Override
    public GridHealthReport visit(PowerGrid grid) {
        for (Substation sub : grid.getSubstations()) {
            sub.accept(this);
        }
        return buildReport();
    }

    @Override
    public GridHealthReport visit(Substation substation) {
        substations++;
        for (DistributionLine line : substation.getLines()) {
            line.accept(this);
        }
        return buildReport();
    }

    @Override
    public GridHealthReport visit(DistributionLine line) {
        lines++;
        for (GridElement elem : line.getElements()) {
            elem.accept(this);
        }
        return buildReport();
    }

    @Override
    public GridHealthReport visit(ResidentialConsumer consumer) {
        residential++;
        totalDemandKw += consumer.getActiveDemandKw();
        return buildReport();
    }

    @Override
    public GridHealthReport visit(IndustrialConsumer consumer) {
        industrial++;
        totalDemandKw += consumer.getActiveDemandKw();
        return buildReport();
    }

    @Override
    public GridHealthReport visit(SolarGenerator generator) {
        solarGenerators++;
        if (generator.isOnline()) {
            totalRenewableGenerationKw += generator.getCurrentGenerationKw();
        } else {
            offlineGenerators++;
        }
        return buildReport();
    }

    @Override
    public GridHealthReport visit(BatteryStorage storage) {
        batteryStorages++;
        totalBatteryCapacityKwh += storage.getCapacityKwh();
        currentBatteryStorageKwh += storage.getCurrentChargeKwh();
        if (storage.getStateOfChargePercent() < 20.0) {
            lowBatteryCount++;
        }
        return buildReport();
    }


    public GridHealthReport buildReport() {
        return new GridHealthReport(
                substations,
                lines,
                residential,
                industrial,
                solarGenerators,
                batteryStorages,
                evStations,
                totalDemandKw,
                totalRenewableGenerationKw,
                totalBatteryCapacityKwh,
                currentBatteryStorageKwh,
                offlineGenerators,
                lowBatteryCount
        );
    }
}
