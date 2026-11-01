package kz.aitu.sdp.assignment6.visitor;

import kz.aitu.sdp.assignment6.visitor.elements.*;
import kz.aitu.sdp.assignment6.visitor.visitors.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Visitor Pattern Unit & Behavioral Tests")
class VisitorPatternTest {

    private Substation testSubstation;
    private DistributionLine lineA;
    private DistributionLine lineB;

    @BeforeEach
    void setUp() {
        testSubstation = new Substation("SUB-TEST", "Testing Substation", 25.0, 110.0, 10.0);

        // Line A (resistance 0 to isolate elemental math)
        lineA = new DistributionLine("LINE-A", "Feeder A", 0.0, 500.0, 10.0, 2.0);
        lineA.addElement(new ResidentialConsumer("RES-1", "House 1", "M-1", "1st St", 10.0, 0.95));
        lineA.addElement(new IndustrialConsumer("IND-1", "Factory 1", "Textile", 100.0, 80.0, 0.90));
        lineA.addElement(new SolarGenerator("SOL-1", "Solar Park 1", 30.0, 30.0, 0.98, true));
        lineA.addElement(new BatteryStorage("BAT-1", "Storage 1", 50.0, 25.0, 5.0, BatteryStorage.OperatingMode.CHARGING));

        // Line B
        lineB = new DistributionLine("LINE-B", "Feeder B", 0.0, 500.0, 10.0, 3.0);
        lineB.addElement(new ResidentialConsumer("RES-2", "House 2", "M-2", "2nd St", 15.0, 0.95));
        lineB.addElement(new SolarGenerator("SOL-2", "Solar Park 2", 20.0, 0.0, 0.98, false)); // Offline!
        lineB.addElement(new BatteryStorage("BAT-2", "Storage 2", 100.0, 10.0, 8.0, BatteryStorage.OperatingMode.DISCHARGING)); // 10% charge
        lineB.addElement(new EvChargingStation("EV-1", "EV Hub 1", 4, 25.0, true));

        testSubstation.addLine(lineA);
        testSubstation.addLine(lineB);
    }

    @Test
    @DisplayName("Test 6: Net Load Calculation Visitor against Hand-Calculated Values")
    void testNumericNetLoadCalculationWithHandCalculatedValues() {
        NetLoadCalculationVisitor visitor = new NetLoadCalculationVisitor();

        // Line A Hand Calculation:
        // Consumers = 10.0 + 80.0 = 90.0 kW
        // Solar Generation = -30.0 kW
        // Battery Charging = +5.0 kW
        // Line A Net Load = 90.0 - 30.0 + 5.0 = 65.0 kW
        double lineALoad = lineA.accept(visitor);
        assertEquals(65.0, lineALoad, 0.001, "Line A net load must match hand calculation");

        // Line B Hand Calculation:
        // Consumers = 15.0 kW
        // Solar Generation (offline) = 0.0 kW
        // Battery Discharging = -8.0 kW
        // EV Station = +25.0 kW
        // Line B Net Load = 15.0 - 8.0 + 25.0 = 32.0 kW
        double lineBLoad = lineB.accept(visitor);
        assertEquals(32.0, lineBLoad, 0.001, "Line B net load must match hand calculation");

        // Substation Total Hand Calculation: 65.0 + 32.0 = 97.0 kW
        double totalSubstationLoad = testSubstation.accept(visitor);
        assertEquals(97.0, totalSubstationLoad, 0.001, "Substation net load must equal exact sum of lines");
    }

    @Test
    @DisplayName("Test 7: Grid Health Audit Visitor accumulates correct state across whole tree")
    void testGridHealthAuditVisitorHandCalculatedReport() {
        GridHealthAuditVisitor visitor = new GridHealthAuditVisitor();
        GridHealthReport report = testSubstation.accept(visitor);

        // Assets Hand Count:
        // 1 Substation, 2 Lines, 2 Residential, 1 Industrial, 2 Solar, 2 Batteries, 1 EV = 11 assets
        assertEquals(1, report.totalSubstations());
        assertEquals(2, report.totalDistributionLines());
        assertEquals(2, report.totalResidentialConsumers());
        assertEquals(1, report.totalIndustrialConsumers());
        assertEquals(2, report.totalSolarGenerators());
        assertEquals(2, report.totalBatteryStorages());
        assertEquals(1, report.totalEvStations());
        assertEquals(11, report.getTotalAssets());

        // Demand Hand Calculation: 10 (RES-1) + 80 (IND-1) + 15 (RES-2) + 25 (EV-1) = 130.0 kW
        assertEquals(130.0, report.totalDemandKw(), 0.001);

        // Renewable Generation: SOL-1 (30.0) + SOL-2 (offline 0) = 30.0 kW
        assertEquals(30.0, report.totalRenewableGenerationKw(), 0.001);

        // Battery Capacity: 50 + 100 = 150 kWh; Stored: 25 + 10 = 35 kWh
        assertEquals(150.0, report.totalBatteryCapacityKwh(), 0.001);
        assertEquals(35.0, report.currentBatteryStorageKwh(), 0.001);

        // Anomalies: SOL-2 is offline (1), BAT-2 is at 10% < 20% (1)
        assertEquals(1, report.offlineGeneratorsCount());
        assertEquals(1, report.lowBatteryStorageCount());
    }

    @Test
    @DisplayName("Test 8: Overloaded Component Filter Visitor isolates degraded and hazardous assets")
    void testOverloadedComponentFilterVisitor() {
        OverloadedComponentFilterVisitor visitor = new OverloadedComponentFilterVisitor();
        List<GridElement> flagged = testSubstation.accept(visitor);

        // In test tree:
        // SOL-2 is offline -> flagged
        // BAT-2 has 10% charge (< 15%) -> flagged
        assertEquals(2, flagged.size());
        assertTrue(flagged.stream().anyMatch(e -> e.getId().equals("SOL-2")));
        assertTrue(flagged.stream().anyMatch(e -> e.getId().equals("BAT-2")));
    }

    @Test
    @DisplayName("Test 9: Hand-written JSON Export formats properly nested, syntactically valid JSON")
    void testHandWrittenJsonExportCorrectNestingAndFormat() {
        GridJsonExportVisitor visitor = new GridJsonExportVisitor();
        String json = testSubstation.accept(visitor);

        assertNotNull(json);
        assertTrue(json.startsWith("{\n"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("\"type\": \"Substation\""));
        assertTrue(json.contains("\"id\": \"SUB-TEST\""));
        assertTrue(json.contains("\"lines\": ["));
        assertTrue(json.contains("\"type\": \"DistributionLine\""));
        assertTrue(json.contains("\"elements\": ["));
        assertTrue(json.contains("\"type\": \"ResidentialConsumer\""));
        assertTrue(json.contains("\"type\": \"BatteryStorage\""));

        // Verify balanced curly braces and square brackets
        long openBraces = json.chars().filter(ch -> ch == '{').count();
        long closeBraces = json.chars().filter(ch -> ch == '}').count();
        assertEquals(openBraces, closeBraces, "JSON curly braces must be balanced");

        long openBrackets = json.chars().filter(ch -> ch == '[').count();
        long closeBrackets = json.chars().filter(ch -> ch == ']').count();
        assertEquals(openBrackets, closeBrackets, "JSON square brackets must be balanced");
    }

    @Test
    @DisplayName("Test 10: Carbon Emission Audit Visitor against Hand-Calculated Value")
    void testCarbonEmissionAuditVisitorHandCalculated() {
        CarbonEmissionAuditVisitor visitor = new CarbonEmissionAuditVisitor();
        double carbonRate = testSubstation.accept(visitor);

        // Hand calculation:
        // Demand from grid: (10 + 80 + 15) * 0.45 = 105 * 0.45 = 47.25
        // Solar displacement: 30 * (-0.45) = -13.50
        // Battery 1 charging: 5 * 0.45 = +2.25
        // Battery 2 discharging: -8 * 0.20 = -1.60
        // EV hub: 25 * 0.35 = 8.75
        // Total = 47.25 - 13.50 + 2.25 - 1.60 + 8.75 = 43.15 kg CO2/hour
        assertEquals(43.15, carbonRate, 0.001);
    }

    @Test
    @DisplayName("Test 11: Double Dispatch routes to correct visitor method without instanceof cascades")
    void testDoubleDispatchMechanism() {
        GridElement consumer = new ResidentialConsumer("R", "House", "M", "St", 5.0, 0.9);
        GridElement solar = new SolarGenerator("S", "Solar", 10.0, 8.0, 0.95, true);

        NetLoadCalculationVisitor visitor = new NetLoadCalculationVisitor();

        assertEquals(5.0, consumer.accept(visitor), 0.001);
        assertEquals(-8.0, solar.accept(visitor), 0.001);
    }
}
