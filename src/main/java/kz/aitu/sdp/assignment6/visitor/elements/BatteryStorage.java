package kz.aitu.sdp.assignment6.visitor.elements;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;

import java.util.Objects;

/**
 * Leaf 4: Grid-scale utility battery storage capable of charging (absorbing load)
 * or discharging (injecting generation) to stabilize the distribution line.
 * Holds data only.
 */
public class BatteryStorage implements GridElement {

    public enum OperatingMode {
        CHARGING,
        DISCHARGING,
        IDLE
    }

    private final String id;
    private final String name;
    private final double capacityKwh;
    private final double currentChargeKwh;
    private final double activePowerRateKw;
    private final OperatingMode mode;

    public BatteryStorage(String id,
                          String name,
                          double capacityKwh,
                          double currentChargeKwh,
                          double activePowerRateKw,
                          OperatingMode mode) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.capacityKwh = capacityKwh;
        this.currentChargeKwh = currentChargeKwh;
        this.activePowerRateKw = activePowerRateKw;
        this.mode = Objects.requireNonNull(mode, "mode cannot be null");
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

    public double getCapacityKwh() {
        return capacityKwh;
    }

    public double getCurrentChargeKwh() {
        return currentChargeKwh;
    }

    public double getActivePowerRateKw() {
        return activePowerRateKw;
    }

    public OperatingMode getMode() {
        return mode;
    }

    public double getStateOfChargePercent() {
        return capacityKwh > 0 ? (currentChargeKwh / capacityKwh) * 100.0 : 0.0;
    }
}
