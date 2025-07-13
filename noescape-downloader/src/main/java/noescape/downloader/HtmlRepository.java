package noescape.downloader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.removeEnd;

public class HtmlRepository {

    private final Path htmlDir;

    public HtmlRepository(Path htmlDir) {
        this.htmlDir = htmlDir;
        if (!Files.exists(htmlDir)) {
            throw new IllegalStateException(htmlDir + " does not exist");
        }
    }

    public void saveHtml(String caseId, String html) {
        try {
            Files.writeString(getPath(caseId), html);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getHtml(String caseId) {
        try {
            return Files.readString(getPath(caseId));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<String> getCaseIds() {
        try (var pathStream = Files.walk(htmlDir)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(fileName -> removeEnd(fileName, ".html"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists(String caseId) {
        return Files.exists(getPath(caseId));
    }


    private Path getPath(String caseId) {
        var lastDigit = caseId.substring(caseId.length() - 1);
        return htmlDir.resolve(lastDigit).resolve(caseId + ".html");
    }
}
