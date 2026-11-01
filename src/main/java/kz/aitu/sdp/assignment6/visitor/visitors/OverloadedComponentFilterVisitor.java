package kz.aitu.sdp.assignment6.visitor.visitors;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;
import kz.aitu.sdp.assignment6.visitor.elements.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Visitor 3 (Filter returning a List of Elements):
 * Traverses a grid subtree and collects all components operating in hazardous,
 * overloaded, or degraded conditions requiring field inspection or load shedding.
 */
public class OverloadedComponentFilterVisitor implements GridVisitor<List<GridElement>> {

    private final List<GridElement> flaggedElements = new ArrayList<>();

    @Override
    public List<GridElement> visit(PowerGrid grid) {
        for (Substation sub : grid.getSubstations()) {
            sub.accept(this);
        }
        return Collections.unmodifiableList(flaggedElements);
    }

    @Override
    public List<GridElement> visit(Substation substation) {
        for (DistributionLine line : substation.getLines()) {
            line.accept(this);
        }
        return Collections.unmodifiableList(flaggedElements);
    }

    @Override
    public List<GridElement> visit(DistributionLine line) {
        // Calculate downstream active consumer load to check against line capacity
        double downstreamDemandKw = 0.0;
        for (GridElement elem : line.getElements()) {
            elem.accept(this);
            if (elem instanceof ResidentialConsumer rc) {
                downstreamDemandKw += rc.getActiveDemandKw();
            } else if (elem instanceof IndustrialConsumer ic) {
                downstreamDemandKw += ic.getActiveDemandKw();

            }
        }

        // Rated line active power capacity: P_max = sqrt(3) * V * I_max * pf
        double lineRatingKw = Math.sqrt(3.0) * line.getNominalVoltageKv() * line.getMaxCurrentAmps() * 0.95;
        if (downstreamDemandKw > lineRatingKw) {
            flaggedElements.add(line);
        }

        return Collections.unmodifiableList(flaggedElements);
    }

    @Override
    public List<GridElement> visit(ResidentialConsumer consumer) {
        // Flag extreme household spike (> 25 kW) indicating potential meter fault or illicit load
        if (consumer.getActiveDemandKw() > 25.0) {
            flaggedElements.add(consumer);
        }
        return Collections.unmodifiableList(flaggedElements);
    }

    @Override
    public List<GridElement> visit(IndustrialConsumer consumer) {
        // Flag industrial plant exceeding contracted grid power
        if (consumer.getActiveDemandKw() > consumer.getContractedDemandKw()) {
            flaggedElements.add(consumer);
        }
        return Collections.unmodifiableList(flaggedElements);
    }

    @Override
    public List<GridElement> visit(SolarGenerator generator) {
        // Flag offline or non-producing generator
        if (!generator.isOnline() || generator.getCurrentGenerationKw() <= 0.0) {
            flaggedElements.add(generator);
        }
        return Collections.unmodifiableList(flaggedElements);
    }

    @Override
    public List<GridElement> visit(BatteryStorage storage) {
        // Flag critical battery depletion (< 15% state of charge)
        if (storage.getStateOfChargePercent() < 15.0) {
            flaggedElements.add(storage);
        }
        return Collections.unmodifiableList(flaggedElements);
    }


    public List<GridElement> getFlaggedElements() {
        return Collections.unmodifiableList(flaggedElements);
    }
}
