package noescape.repository;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextRepository {

    public static final Path TEXT_DIR = Path.of("../noescape-text/text");
    public static final String SEPARATOR = "=== ENTITIES ===";

    public String getPlainText(String caseId) {
        try {
            Path textFile = filePath(caseId);
            String text = Files.readString(textFile);
            return StringUtils.substringBefore(text, SEPARATOR);
        } catch (IOException e) {
            throw new UncheckedIOException("Error getting text of " + caseId, e);
        }
    }

    public static Path filePath(String caseId) {
        Path dir = TEXT_DIR.resolve(caseId.substring(caseId.length() - 1));
        return dir.resolve(caseId + ".txt");
    }
}
