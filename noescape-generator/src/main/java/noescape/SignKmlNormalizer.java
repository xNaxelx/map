package noescape;

import noescape.repository.BorderSignRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SignKmlNormalizer {

    public static void main(String[] args) throws IOException {
        var borderSigns = BorderSignRepository.readBorderSignsFromKlm();
        var kml = KmlGenerator.toKml(borderSigns);
        Files.writeString(Path.of("kml/signs.kml"), kml);
    }
}
