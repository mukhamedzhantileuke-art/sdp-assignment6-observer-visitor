package kz.aitu.sdp.assignment6.visitor.elements;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;

import java.util.Objects;

/**
 * Leaf 2: High-voltage industrial manufacturing facility or commercial complex.
 * Holds data only.
 */
public class IndustrialConsumer implements GridElement {

    private final String id;
    private final String name;
    private final String industrySector;
    private final double contractedDemandKw;
    private final double activeDemandKw;
    private final double powerFactor;

    public IndustrialConsumer(String id,
                              String name,
                              String industrySector,
                              double contractedDemandKw,
                              double activeDemandKw,
                              double powerFactor) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.industrySector = Objects.requireNonNull(industrySector, "industrySector cannot be null");
        this.contractedDemandKw = contractedDemandKw;
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

    public String getIndustrySector() {
        return industrySector;
    }

    public double getContractedDemandKw() {
        return contractedDemandKw;
    }

    public double getActiveDemandKw() {
        return activeDemandKw;
    }

    public double getPowerFactor() {
        return powerFactor;
    }
}
