package kz.aitu.sdp.assignment6.visitor.elements;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Root composite container: Represents a regional electrical transmission and distribution grid.
 * Holds data and child substations only.
 */
public class PowerGrid implements GridElement {

    private final String id;
    private final String name;
    private final String region;
    private final List<Substation> substations;

    public PowerGrid(String id, String name, String region) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.region = Objects.requireNonNull(region, "region cannot be null");
        this.substations = new ArrayList<>();
    }

    public void addSubstation(Substation substation) {
        substations.add(Objects.requireNonNull(substation, "substation cannot be null"));
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

    public String getRegion() {
        return region;
    }

    public List<Substation> getSubstations() {
        return Collections.unmodifiableList(substations);
    }
}
