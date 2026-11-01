package kz.aitu.sdp.assignment6.visitor.visitors;

import kz.aitu.sdp.assignment6.visitor.GridVisitor;
import kz.aitu.sdp.assignment6.visitor.elements.*;

import java.util.List;
import java.util.Locale;

/**
 * Visitor 4 (Hand-written JSON Export with Correct Nesting):
 * Serializes any grid composite node or entire grid into syntactically valid,
 * properly indented JSON without using any third-party reflection or JSON libraries.
 *
 * Traversal Justification: Visitor-driven traversal is essential here because the visitor
 * must render parent container opening tags, indent all child elements, insert commas between
 * sibling array elements (without trailing commas), and close brackets properly.
 */
public class GridJsonExportVisitor implements GridVisitor<String> {

    private int indentLevel = 0;

    private String indent() {
        return "  ".repeat(indentLevel);
    }

    private String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    @Override
    public String visit(PowerGrid grid) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("{\n");
        indentLevel++;
        sb.append(indent()).append("\"type\": \"PowerGrid\",\n");
        sb.append(indent()).append("\"id\": \"").append(escape(grid.getId())).append("\",\n");
        sb.append(indent()).append("\"name\": \"").append(escape(grid.getName())).append("\",\n");
        sb.append(indent()).append("\"region\": \"").append(escape(grid.getRegion())).append("\",\n");
        sb.append(indent()).append("\"substations\": [\n");

        indentLevel++;
        List<Substation> substations = grid.getSubstations();
        for (int i = 0; i < substations.size(); i++) {
            sb.append(substations.get(i).accept(this));
            if (i < substations.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        indentLevel--;

        sb.append(indent()).append("]\n");
        indentLevel--;
        sb.append(indent()).append("}");
        return sb.toString();
    }

    @Override
    public String visit(Substation substation) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("{\n");
        indentLevel++;
        sb.append(indent()).append("\"type\": \"Substation\",\n");
        sb.append(indent()).append("\"id\": \"").append(escape(substation.getId())).append("\",\n");
        sb.append(indent()).append("\"name\": \"").append(escape(substation.getName())).append("\",\n");
        sb.append(indent()).append(String.format(Locale.US, "\"transformerCapacityMva\": %.2f,\n", substation.getTransformerCapacityMva()));
        sb.append(indent()).append(String.format(Locale.US, "\"primaryVoltageKv\": %.2f,\n", substation.getPrimaryVoltageKv()));
        sb.append(indent()).append(String.format(Locale.US, "\"secondaryVoltageKv\": %.2f,\n", substation.getSecondaryVoltageKv()));
        sb.append(indent()).append("\"lines\": [\n");

        indentLevel++;
        List<DistributionLine> lines = substation.getLines();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i).accept(this));
            if (i < lines.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        indentLevel--;

        sb.append(indent()).append("]\n");
        indentLevel--;
        sb.append(indent()).append("}");
        return sb.toString();
    }

    @Override
    public String visit(DistributionLine line) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("{\n");
        indentLevel++;
        sb.append(indent()).append("\"type\": \"DistributionLine\",\n");
        sb.append(indent()).append("\"id\": \"").append(escape(line.getId())).append("\",\n");
        sb.append(indent()).append("\"name\": \"").append(escape(line.getName())).append("\",\n");
        sb.append(indent()).append(String.format(Locale.US, "\"resistanceOhms\": %.4f,\n", line.getResistanceOhms()));
        sb.append(indent()).append(String.format(Locale.US, "\"maxCurrentAmps\": %.1f,\n", line.getMaxCurrentAmps()));
        sb.append(indent()).append(String.format(Locale.US, "\"nominalVoltageKv\": %.2f,\n", line.getNominalVoltageKv()));
        sb.append(indent()).append(String.format(Locale.US, "\"lengthKm\": %.2f,\n", line.getLengthKm()));
        sb.append(indent()).append("\"elements\": [\n");

        indentLevel++;
        List<GridElement> elements = line.getElements();
        for (int i = 0; i < elements.size(); i++) {
            sb.append(elements.get(i).accept(this));
            if (i < elements.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        indentLevel--;

        sb.append(indent()).append("]\n");
        indentLevel--;
        sb.append(indent()).append("}");
        return sb.toString();
    }

    @Override
    public String visit(ResidentialConsumer consumer) {
        return indent() + String.format(Locale.US,
                "{\"type\": \"ResidentialConsumer\", \"id\": \"%s\", \"name\": \"%s\", \"meterId\": \"%s\", \"activeDemandKw\": %.2f, \"powerFactor\": %.2f}",
                escape(consumer.getId()), escape(consumer.getName()), escape(consumer.getMeterId()),
                consumer.getActiveDemandKw(), consumer.getPowerFactor());
    }

    @Override
    public String visit(IndustrialConsumer consumer) {
        return indent() + String.format(Locale.US,
                "{\"type\": \"IndustrialConsumer\", \"id\": \"%s\", \"name\": \"%s\", \"sector\": \"%s\", \"activeDemandKw\": %.2f, \"contractedDemandKw\": %.2f}",
                escape(consumer.getId()), escape(consumer.getName()), escape(consumer.getIndustrySector()),
                consumer.getActiveDemandKw(), consumer.getContractedDemandKw());
    }

    @Override
    public String visit(SolarGenerator generator) {
        return indent() + String.format(Locale.US,
                "{\"type\": \"SolarGenerator\", \"id\": \"%s\", \"name\": \"%s\", \"ratedCapacityKw\": %.2f, \"currentGenerationKw\": %.2f, \"online\": %b}",
                escape(generator.getId()), escape(generator.getName()),
                generator.getRatedCapacityKw(), generator.getCurrentGenerationKw(), generator.isOnline());
    }

    @Override
    public String visit(BatteryStorage storage) {
        return indent() + String.format(Locale.US,
                "{\"type\": \"BatteryStorage\", \"id\": \"%s\", \"name\": \"%s\", \"capacityKwh\": %.2f, \"currentChargeKwh\": %.2f, \"mode\": \"%s\", \"activePowerRateKw\": %.2f}",
                escape(storage.getId()), escape(storage.getName()),
                storage.getCapacityKwh(), storage.getCurrentChargeKwh(), storage.getMode(), storage.getActivePowerRateKw());
    }


    @Override
    public String visit(EvChargingStation evStation) {
        return indent() + String.format(Locale.US,
                "{\"type\": \"EvChargingStation\", \"id\": \"%s\", \"name\": \"%s\", \"bays\": %d, \"activeDrawKw\": %.2f, \"fastCharging\": %b}",
                escape(evStation.getId()), escape(evStation.getName()),
                evStation.getChargingBays(), evStation.getActiveDrawKw(), evStation.isFastChargingEnabled());
    }
}
