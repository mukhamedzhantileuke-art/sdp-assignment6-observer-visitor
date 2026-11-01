package kz.aitu.sdp.assignment6.visitor.visitors;

/**
 * Immutable audit report accumulated across the grid tree by {@link GridHealthAuditVisitor}.
 */
public record GridHealthReport(
        int totalSubstations,
        int totalDistributionLines,
        int totalResidentialConsumers,
        int totalIndustrialConsumers,
        int totalSolarGenerators,
        int totalBatteryStorages,
        int totalEvStations,
        double totalDemandKw,
        double totalRenewableGenerationKw,
        double totalBatteryCapacityKwh,
        double currentBatteryStorageKwh,
        int offlineGeneratorsCount,
        int lowBatteryStorageCount
) {
    public int getTotalAssets() {
        return totalSubstations + totalDistributionLines + totalResidentialConsumers +
                totalIndustrialConsumers + totalSolarGenerators + totalBatteryStorages + totalEvStations;
    }

    public double getNetGenerationDeficitKw() {
        return Math.max(0.0, totalDemandKw - totalRenewableGenerationKw);
    }

    public double getRenewablePenetrationRatio() {
        return totalDemandKw > 0 ? (totalRenewableGenerationKw / totalDemandKw) * 100.0 : 0.0;
    }

    public double getOverallBatteryStateOfChargePercent() {
        return totalBatteryCapacityKwh > 0 ? (currentBatteryStorageKwh / totalBatteryCapacityKwh) * 100.0 : 0.0;
    }
}
