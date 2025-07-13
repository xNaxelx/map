package noescape.model;

import java.util.List;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

public final class SpacyDocument {

    public record Entity(String label, String text, int line, int col, int start, int end) {

        public static Predicate<Entity> havingLabel(String label) {
            return entity -> entity.label.equals(label);
        }
    }

    // The labels usually present in the protocol excerpt
    public static final List<String> PROTOCOL_LABELS = List.of(
            "PROTOCOL",
            "PROTO_DATE",
            "ARR_DATE",
            "ARR_TIME",
            "ARR_PLACE",
            "ARR_SETTL",
            "ARR_OKRUG",
            "ARR_GROMADA",
            "ARR_RAYON",
            "ARR_OBLAST",
            "DIST",
            "SETTL_FROM",
            "SETTL_TO",
            "SETTL_CNTRY",
            "BORDER_CNTRY",
            "GUARD",
            "VPS",
            "ZNAK");

    private final String id;
    private final String text;
    private final String lowerText;

    private final List<Entity> entities;
    private final List<Entity> introduction;
    private final List<Entity> motivation;
    private final List<Entity> resolution;

    private final List<Entity> protocolLine;

    public SpacyDocument(String id, String text, List<Entity> entities) {
        this.id = id;
        this.text = text;
        this.lowerText = text.toLowerCase();
        this.entities = entities;
        var motivationStart = getMotivationStart(lowerText, text);
        var resolutionStart = getResolutionStart(lowerText, text);
        introduction = entities.stream().filter(entity -> (motivationStart < 0 || entity.end < motivationStart) && (resolutionStart < 0 || entity.end < resolutionStart)).toList();
        motivation = entities.stream().filter(entity -> entity.start >= motivationStart && (resolutionStart < 0 || entity.end < resolutionStart)).toList();
        resolution = resolutionStart < 0 ? motivation : entities.stream().filter(entity -> entity.start >= resolutionStart).toList();
        protocolLine = findProtocolLine(entities); // FIXME
    }

    public String id() {
        return id;
    }

    public String text() {
        return text;
    }

    public String lowerText() {
        return lowerText;
    }

    public List<Entity> entities() {
        return entities;
    }

    public List<Entity> introduction() {
        return introduction;
    }

    public List<Entity> motivation() {
        return motivation;
    }

    public List<Entity> resolution() {
        return resolution;
    }

    public List<Entity> protocolLine() {
        return protocolLine;
    }

    @Override
    public String toString() {
        return "SpacyDocument[" +
                "id=" + id + ", " +
                "text=" + text + ", " +
                "entities=" + entities + ']';
    }

    private static List<Entity> findProtocolLine(List<Entity> documentEntities) {
        // Entities extracted from each line
        var entitiesByLineNumber = documentEntities.stream().collect(groupingBy(Entity::line));

        // Find the line containing most of protocol entities
        List<Entity> protocolLine = emptyList();
        long maxCount = 0;
        for (var lineEntities : entitiesByLineNumber.values()) {
            var count = lineEntities.stream().filter(entity -> PROTOCOL_LABELS.contains(entity.label())).count();
            if (count > maxCount) {
                maxCount = count;
                protocolLine = lineEntities;
            }
        }
        return protocolLine;
    }

    public static int getMotivationStart(String lowerText, String text) {
        int index = indexAfter(lowerText, "встановив:\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "встановив\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "установив:\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "установив\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "встановила:\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "встановила\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "установила:\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "установила\n");
        if (index >= 0) return index;
        index = indexAfter(text, "ВСТАНОВИВ:");
        if (index >= 0) return index;
        index = indexAfter(text, "ВСТАНОВИЛА:");
        if (index >= 0) return index;
        index = indexAfter(text, "УСТАНОВИВ:");
        if (index >= 0) return index;
        index = indexAfter(text, "УСТАНОВИЛА:");
        if (index >= 0) return index;
        index = indexAfter(text, "ВСТАНОВИЛА.");
        if (index >= 0) return index;

        var lines = lowerText.lines().toList();
        index = 0;
        for (String line : lines) {
            index += line.length() + 1;
            if (line.endsWith(":")) {
                line = line.substring(0, line.length() - 1);
            }
            line = line.replace(" ", "");
            if (line.endsWith("встановив") || line.endsWith("встановила") || line.endsWith("установив") || line.endsWith("установила") || line.startsWith("і. опис обставин")) {
                return index;
            }
        }

        return -1;
    }

    private static int getResolutionStart(String lowerText, String text) {
        int index = indexAfter(lowerText, "постановив:\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "постановив\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "постановила:\n");
        if (index >= 0) return index;
        index = indexAfter(lowerText, "постановила\n");
        if (index >= 0) return index;
        index = indexAfter(text, "ПОСТАНОВИВ:");
        if (index >= 0) return index;
        index = indexAfter(text, "ПОСТАНОВИЛА:");
        if (index >= 0) return index;
        return -1;
    }

    private static int indexAfter(String str, String substr) {
        int index = str.indexOf(substr);
        return index < 0 ? -1 : index + substr.length();
    }
}
