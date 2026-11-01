package kz.aitu.sdp.assignment6.visitor.elements;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;

import java.util.Objects;

/**
 * Leaf 1: Residential household consumer with metered active power draw.
 * Holds data only.
 */
public class ResidentialConsumer implements GridElement {

    private final String id;
    private final String name;
    private final String meterId;
    private final String address;
    private final double activeDemandKw;
    private final double powerFactor;

    public ResidentialConsumer(String id,
                               String name,
                               String meterId,
                               String address,
                               double activeDemandKw,
                               double powerFactor) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.meterId = Objects.requireNonNull(meterId, "meterId cannot be null");
        this.address = Objects.requireNonNull(address, "address cannot be null");
        this.activeDemandKw = activeDemandKw;
        this.powerFactor = powerFactor;
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

    public String getMeterId() {
        return meterId;
    }

    public String getAddress() {
        return address;
    }

    public double getActiveDemandKw() {
        return activeDemandKw;
    }

    public double getPowerFactor() {
        return powerFactor;
    }
}
