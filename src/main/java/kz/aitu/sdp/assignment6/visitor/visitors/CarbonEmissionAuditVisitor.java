package kz.aitu.sdp.assignment6.visitor.visitors;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;
import kz.aitu.sdp.assignment6.visitor.elements.*;

/**
 * Visitor 5 (Trade-off proof Commit A: New operation added without touching any element class):
 * Calculates the environmental carbon footprint rate (kg CO2 / hour) across any grid subtree.
 *
 * Demonstrates the core advantage of the Visitor Pattern: Adding a completely new operation
 * requires zero modifications to existing element classes.
 */
public class CarbonEmissionAuditVisitor implements GridVisitor<Double> {

    private static final double GRID_EMISSION_FACTOR = 0.45;
    private static final double SOLAR_OFFSET_FACTOR = -0.45;

    @Override
    public Double visit(PowerGrid grid) {
        double total = 0.0;
        for (Substation sub : grid.getSubstations()) {
            total += sub.accept(this);
        }
        return total;
    }

    @Override
    public Double visit(Substation substation) {
        double total = 0.0;
        for (DistributionLine line : substation.getLines()) {
            total += line.accept(this);
        }
        return total;
    }

    @Override
    public Double visit(DistributionLine line) {
        double total = 0.0;
        for (GridElement elem : line.getElements()) {
            total += elem.accept(this);
        }
        return total;
    }

    @Override
    public Double visit(ResidentialConsumer consumer) {
        return consumer.getActiveDemandKw() * GRID_EMISSION_FACTOR;
    }

    @Override
    public Double visit(IndustrialConsumer consumer) {
        return consumer.getActiveDemandKw() * GRID_EMISSION_FACTOR;
    }

    @Override
    public Double visit(SolarGenerator generator) {
        return generator.isOnline() ? generator.getCurrentGenerationKw() * SOLAR_OFFSET_FACTOR : 0.0;
    }

    @Override
    public Double visit(BatteryStorage storage) {
        return switch (storage.getMode()) {
            case CHARGING -> storage.getActivePowerRateKw() * GRID_EMISSION_FACTOR;
            case DISCHARGING -> -storage.getActivePowerRateKw() * 0.20;
            case IDLE -> 0.0;
        };
    }
}
