package noescape;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        SpacyExtractor.main(args);
        Geocoder.main(args);
        SiteGenerator.main(args);
    }
}
