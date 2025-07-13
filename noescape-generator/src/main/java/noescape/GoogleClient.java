package noescape;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import noescape.model.Locality;
import noescape.model.Position;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

public class GoogleClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        GoogleClient googleClient = new GoogleClient();
        Locality locality = new Locality("семенівка", null, "подільський", "київська");
        var result = googleClient.geocode(locality);
         System.out.println(result);
    }


    public record GeocodeResult(Locality locality, Position position) {
    }


    public static final Path GEOCODE_DIR = Path.of("geocode");

    public static String API_KEY = "AIZasY5nI6m0FpADhsznt7bcryWACrdyR8a-8Pk";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleClient() throws IOException {
        Files.createDirectories(GEOCODE_DIR);
    }

    public Optional<GeocodeResult> geocode(Locality locality) {
        requireNonNull(locality.oblast());
        requireNonNull(locality.settlement());
        try {
            String json = getJson(locality);
            var geocodeResults = parseJson(json);
            var match = findMatch(locality, geocodeResults);
            if (match == null) {
                // Rayon might have been renamed
                match = findMatch(locality.withRayon(null), geocodeResults);
            }
            return Optional.ofNullable(match);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static GeocodeResult findMatch(Locality locality, List<GeocodeResult> geocodeResults) {
        for (var geocodeResult : geocodeResults) {
            Locality geocoded = geocodeResult.locality();
            if (locality.oblast().equals(geocoded.oblast())
                    && locality.settlement().equals(geocoded.settlement())) {
                if (locality.rayon() == null) {
                    return geocodeResult;
                }
                if (locality.rayon().equals(geocoded.rayon())) {
                    return geocodeResult;
                }
            }
        }
        return null;
    }

    private String getJson(Locality locality) throws IOException, InterruptedException {
        String fileName = fileName(locality);
        Path file = GEOCODE_DIR.resolve(fileName);
        if (Files.exists(file)) {
            return Files.readString(file);
        }
        if (locality.rayon() == null && locality.settlement() != null) {
//            return "{\"results\": []}";
        }

        Thread.sleep(50); // Reduce request per minute rate to avoid additional charges
        var sb = new StringBuilder();
        sb.append("country:UA");
        if (locality.settlement() != null) {
            sb.append("|locality:").append(locality.settlement());
        }
        if (locality.rayon() != null) {
            sb.append("|administrative_area:").append(locality.rayon()).append(" район");
        }
        if (locality.oblast() != null) {
            sb.append("|administrative_area:").append(locality.oblast()).append(" область");
        }
        String components = sb.toString();

        URI uri = URI.create("https://maps.googleapis.com/maps/api/geocode/json?&language=uk&key=" + API_KEY + "&components=" + URLEncoder.encode(components, UTF_8));
        System.out.println("Geocode with google: " + sb);
        var request = HttpRequest.newBuilder().uri(uri).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println(response.statusCode());
            System.err.println(response.body());
            throw new RuntimeException("Bad request " + response.statusCode() + "\n" + response.body());
        }
        String json = response.body();
        if (json.contains("REQUEST_DENIED")) {
            throw new RuntimeException(objectMapper.readTree(json).get("error_message").asText());
        }
        Files.writeString(file, json);
        return json;
    }

    public List<GeocodeResult> parseJson(String json) throws JsonProcessingException {
        List<GeocodeResult> geocodeResults = new ArrayList<>();
        var resultsNode = objectMapper.readTree(json).get("results");

        for (JsonNode resultNode : resultsNode) {
            Locality geocodedLocality = new Locality(null, null, null, null);
            for (JsonNode addressComponent : resultNode.get("address_components")) {
                String longName = addressComponent.get("long_name").asText();
                String types = addressComponent.get("types").toString();
                if (longName.contains("область")) {
                    String oblast = substringBeforeLast(longName, " область").replace("'", "ʼ");
                    geocodedLocality = geocodedLocality.withOblast(oblast);
                } else if (longName.contains("район")) {
                    String rayon = substringBeforeLast(longName, " район").replace("'", "ʼ");
                    geocodedLocality = geocodedLocality.withRayon(rayon);
                } else if (types.contains("locality")) {
                    String settlement = longName.replace("'", "ʼ");
                    geocodedLocality = geocodedLocality.withSettlement(settlement);
                }
            }
            var locationNode = resultNode.get("geometry").get("location");
            JsonNode lat = locationNode.get("lat");
            JsonNode lon = locationNode.get("lng");
            geocodeResults.add(new GeocodeResult(geocodedLocality, new Position(lat.asDouble(), lon.asDouble())));
        }
        return geocodeResults;
    }

    private static String fileName(Locality locality) {
        var key = locality.oblast() + "_" + locality.rayon() + "_" + locality.settlement();
        key = key.toLowerCase().replace(' ', '-');
        return key + ".json";
    }
}
