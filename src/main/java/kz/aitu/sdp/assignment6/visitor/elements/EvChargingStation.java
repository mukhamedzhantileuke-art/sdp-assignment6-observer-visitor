package kz.aitu.sdp.assignment6.visitor.elements;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;

import java.util.Objects;

/**
 * Leaf 5 (Trade-off demonstration element): Electric Vehicle DC Fast Charging Station.
 * Represents an aggregated dynamic EV charging load.
 *
 * Demonstrates the core disadvantage of the Visitor Pattern: Adding a new element type
 * forces changes across the GridVisitor interface and EVERY concrete visitor class.
 */
public class EvChargingStation implements GridElement {

    private final String id;
    private final String name;
    private final int chargingBays;
    private final double activeDrawKw;
    private final boolean fastChargingEnabled;

    public EvChargingStation(String id,
                             String name,
                             int chargingBays,
                             double activeDrawKw,
                             boolean fastChargingEnabled) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.chargingBays = chargingBays;
        this.activeDrawKw = activeDrawKw;
        this.fastChargingEnabled = fastChargingEnabled;
    }

    @Override
    public <R> R accept(GridVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getChargingBays() {
        return chargingBays;
    }

    public double getActiveDrawKw() {
        return activeDrawKw;
    }

    public boolean isFastChargingEnabled() {
        return fastChargingEnabled;
    }
}
