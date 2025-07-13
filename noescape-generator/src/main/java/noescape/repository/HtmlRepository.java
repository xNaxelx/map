package noescape.repository;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HtmlRepository {

    public static final Path HTML_DIR = Path.of("../noescape-text/html");

    public HtmlRepository() throws IOException {
        Files.createDirectories(HtmlRepository.HTML_DIR);
    }

    public String getHtml(String caseId) {
        try {
            return Files.readString(getPath(caseId));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<String> getCaseIds() {
        try (var stream = Files.list(HTML_DIR)) {
            return stream.sorted().parallel()
                    .map(path -> StringUtils.removeEnd(path.getFileName().toString(), ".html"))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path getPath(String caseId) {
        var lastDigit = caseId.substring(caseId.length() - 1);
        return HTML_DIR.resolve(lastDigit).resolve(caseId + ".html");
    }
}
