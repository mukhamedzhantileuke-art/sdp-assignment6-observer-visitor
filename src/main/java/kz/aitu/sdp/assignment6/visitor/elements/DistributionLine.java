package kz.aitu.sdp.assignment6.visitor.elements;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Container 2: Medium/low-voltage distribution feeder line supplying consumers,
 * storage, and distributed generators. Holds data and child leaf elements only.
 */
public class DistributionLine implements GridElement {

    private final String id;
    private final String name;
    private final double resistanceOhms;
    private final double maxCurrentAmps;
    private final double nominalVoltageKv;
    private final double lengthKm;
    private final List<GridElement> elements;

    public DistributionLine(String id,
                            String name,
                            double resistanceOhms,
                            double maxCurrentAmps,
                            double nominalVoltageKv,
                            double lengthKm) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.resistanceOhms = resistanceOhms;
        this.maxCurrentAmps = maxCurrentAmps;
        this.nominalVoltageKv = nominalVoltageKv;
        this.lengthKm = lengthKm;
        this.elements = new ArrayList<>();
    }

    public void addElement(GridElement element) {
        elements.add(Objects.requireNonNull(element, "element cannot be null"));
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

    public double getResistanceOhms() {
        return resistanceOhms;
    }

    public double getMaxCurrentAmps() {
        return maxCurrentAmps;
    }

    public double getNominalVoltageKv() {
        return nominalVoltageKv;
    }

    public double getLengthKm() {
        return lengthKm;
    }

    public List<GridElement> getElements() {
        return Collections.unmodifiableList(elements);
    }
}
