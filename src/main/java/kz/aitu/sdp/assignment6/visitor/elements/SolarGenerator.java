package kz.aitu.sdp.assignment6.visitor.elements;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;

import java.util.Objects;

/**
 * Leaf 3: Photovoltaic solar array contributing renewable generation to the feeder line.
 * Holds data only.
 */
public class SolarGenerator implements GridElement {

    private final String id;
    private final String name;
    private final double ratedCapacityKw;
    private final double currentGenerationKw;
    private final double inverterEfficiency;
    private final boolean online;

    public SolarGenerator(String id,
                          String name,
                          double ratedCapacityKw,
                          double currentGenerationKw,
                          double inverterEfficiency,
                          boolean online) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.ratedCapacityKw = ratedCapacityKw;
        this.currentGenerationKw = currentGenerationKw;
        this.inverterEfficiency = inverterEfficiency;
        this.online = online;
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

    public double getRatedCapacityKw() {
        return ratedCapacityKw;
    }

    public double getCurrentGenerationKw() {
        return currentGenerationKw;
    }

    public double getInverterEfficiency() {
        return inverterEfficiency;
    }

    public boolean isOnline() {
        return online;
    }
}
