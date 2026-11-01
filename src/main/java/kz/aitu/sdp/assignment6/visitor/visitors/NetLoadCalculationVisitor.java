package kz.aitu.sdp.assignment6.visitor.visitors;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;
import kz.aitu.sdp.assignment6.visitor.elements.*;

/**
 * Visitor 1 (Numeric Calculation): Computes the net active power load (in kW) across
 * any node or subtree in the electrical grid.
 *
 * Traversal Decision: Visitor-driven traversal allows this visitor to aggregate values
 * recursively while applying distribution line technical losses (I^2 * R / 1000).
 *
 * Calculation Formula:
 * Net Load = Total Consumption (Residential + Industrial + EV Charging)
 *          - Renewable Generation (Solar)
 *          + Battery Flow (+ if Charging, - if Discharging, 0 if Idle)
 *          + Line Technical Losses (kW)
 */
public class NetLoadCalculationVisitor implements GridVisitor<Double> {

    @Override
    public Double visit(PowerGrid grid) {
        double total = 0.0;
        for (Substation substation : grid.getSubstations()) {
            total += substation.accept(this);
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
        double elementsNetLoad = 0.0;
        for (GridElement element : line.getElements()) {
            elementsNetLoad += element.accept(this);
        }

        // Calculate simplified line resistive losses (P_loss = I^2 * R / 1000 kW)
        // Approximate current based on net load: I = P / (sqrt(3) * V_nom * pf)
        double technicalLossKw = 0.0;
        if (elementsNetLoad > 0 && line.getNominalVoltageKv() > 0) {
            double approxCurrentAmps = elementsNetLoad / (Math.sqrt(3.0) * line.getNominalVoltageKv() * 0.95);
            // Cap current to avoid runaway in extreme test cases
            double currentUsed = Math.min(approxCurrentAmps, line.getMaxCurrentAmps());
            technicalLossKw = (Math.pow(currentUsed, 2) * line.getResistanceOhms()) / 1000.0;
        }

        return elementsNetLoad + technicalLossKw;
    }

    @Override
    public Double visit(ResidentialConsumer consumer) {
        return consumer.getActiveDemandKw();
    }

    @Override
    public Double visit(IndustrialConsumer consumer) {
        return consumer.getActiveDemandKw();
    }

    @Override
    public Double visit(SolarGenerator generator) {
        return generator.isOnline() ? -generator.getCurrentGenerationKw() : 0.0;
    }

    @Override
    public Double visit(BatteryStorage storage) {
        return switch (storage.getMode()) {
            case CHARGING -> storage.getActivePowerRateKw(); // Absorbs load from grid
            case DISCHARGING -> -storage.getActivePowerRateKw(); // Injects power into grid
            case IDLE -> 0.0;
        };
    }

}
