package kz.aitu.sdp.assignment6.visitor.elements;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Container 1: Step-down electrical substation connecting the transmission grid
 * to regional feeder lines. Holds data and child distribution lines only.
 */
public class Substation implements GridElement {

    private final String id;
    private final String name;
    private final double transformerCapacityMva;
    private final double primaryVoltageKv;
    private final double secondaryVoltageKv;
    private final List<DistributionLine> lines;

    public Substation(String id,
                      String name,
                      double transformerCapacityMva,
                      double primaryVoltageKv,
                      double secondaryVoltageKv) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.transformerCapacityMva = transformerCapacityMva;
        this.primaryVoltageKv = primaryVoltageKv;
        this.secondaryVoltageKv = secondaryVoltageKv;
        this.lines = new ArrayList<>();
    }

    public void addLine(DistributionLine line) {
        lines.add(Objects.requireNonNull(line, "line cannot be null"));
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

    public double getTransformerCapacityMva() {
        return transformerCapacityMva;
    }

    public double getPrimaryVoltageKv() {
        return primaryVoltageKv;
    }

    public double getSecondaryVoltageKv() {
        return secondaryVoltageKv;
    }

    public List<DistributionLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
