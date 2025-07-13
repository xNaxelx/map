package noescape.downloader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ApplicationConfig {

    private final Properties properties = new Properties();

    public ApplicationConfig() {
        Path file = Path.of("application.properties");
        if (Files.exists(file)) {
            try (var inputStream = Files.newInputStream(file)) {
                properties.load(inputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public String getProperty(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : properties.getProperty(key, defaultValue);
    }
}
