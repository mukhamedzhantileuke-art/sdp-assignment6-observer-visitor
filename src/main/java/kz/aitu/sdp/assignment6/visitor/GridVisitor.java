package kz.aitu.sdp.assignment6.visitor;

import kz.aitu.sdp.assignment6.visitor.elements.*;

/**
 * Generic Visitor interface for operations over the Energy Grid composite tree.
 * Supports double dispatch across all container and leaf nodes.
 *
 * @param <R> return type of the visitor evaluation
 */
public interface GridVisitor<R> {

    R visit(PowerGrid grid);

    R visit(Substation substation);

    R visit(DistributionLine line);

    R visit(ResidentialConsumer consumer);

    R visit(IndustrialConsumer consumer);

    R visit(SolarGenerator generator);

    R visit(BatteryStorage storage);

    R visit(EvChargingStation evStation);

}
