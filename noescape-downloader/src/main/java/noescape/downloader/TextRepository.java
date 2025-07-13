package noescape.downloader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextRepository {

    private final Path textDir;

    public TextRepository(Path textDir) throws IOException {
        this.textDir = textDir;
        if (!Files.exists(textDir)) {
            throw new IllegalStateException(textDir.toAbsolutePath() + " does not exist");
        }
        for (int i = 0; i < 10; i++) {
            Files.createDirectories(textDir.resolve(String.valueOf(i)));
        }
    }

    public boolean fileExists(String caseId) {
        return Files.exists(filePath(caseId));
    }

    public void saveText(String caseId, String text) {
        var textFile = filePath(caseId);
        try {
            Files.writeString(textFile, text);
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing file " + textFile, e);
        }
    }

    private Path filePath(String caseId) {
        String lastDigit = caseId.substring(caseId.length() - 1);
        return textDir.resolve(lastDigit).resolve(caseId + ".txt");
    }
}
