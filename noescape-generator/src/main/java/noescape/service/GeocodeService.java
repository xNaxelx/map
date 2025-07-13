package noescape.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import noescape.GoogleClient;
import noescape.model.Locality;
import noescape.model.Position;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static noescape.GoogleClient.GEOCODE_DIR;

public class GeocodeService {

    public static void main(String[] args) throws IOException {
        new GeocodeService();
    }

    private final GoogleClient googleClient = new GoogleClient();
    private Map<Locality, Set<Position>> places = new HashMap<>();


    public GeocodeService() throws IOException {
        var objectMapper = new ObjectMapper();
        for (Path file : listGeocodeDir()) {
            var json = Files.readString(file);
            var resultsNode = objectMapper.readTree(json).get("results");
            result: for (JsonNode resultNode : resultsNode) {
                var locality = new Locality(null, null, null, null);
                boolean city = false;
                for (JsonNode addressComponent : resultNode.get("address_components")) {
                    String longName = addressComponent.get("long_name").asText().replaceAll("['’ʹ‘`]", "ʼ");
                    String types = addressComponent.get("types").toString();
                    if (types.contains("street_number") || types.contains("route")) {
                        // Too detailed position, we only interested in the center of the settlement
                        System.err.println(file);
                        continue result;
                    }
                    boolean isLocality = types.contains("\"locality\"");
                    boolean isRayon = types.contains("administrative_area_level_2");
                    boolean isOblast = types.contains("administrative_area_level_1");
                    city |= types.contains("administrative_area_level_3");
                    if (isLocality) {
                        if (isRayon || isOblast) throw new IllegalStateException(addressComponent.toString());
                        locality = locality.withSettlement(longName);
                    } else if (isRayon) {
                        if (isOblast) throw new IllegalStateException(addressComponent.toString());
                        if (longName.contains("район")) {
                            locality = locality.withRayon(longName.replace(" район", ""));
                        }
                    } else if (isOblast) {
                        if (longName.equals("місто Київ")) {
                            locality = locality.withOblast("Київська");
                        } else {
                            locality = locality.withOblast(longName.replace(" область", ""));
                        }
                    }
                }
                if (locality.settlement() != null || locality.rayon() != null) {
                    var locationNode = resultNode.get("geometry").get("location");
                    JsonNode lat = locationNode.get("lat");
                    JsonNode lng = locationNode.get("lng");
                    var position = new Position(lat.asDouble(), lng.asDouble());
                    registerLocalityPosition(locality, position);
                    if (city) {
                        // Cities sometimes have oblast and rayon omitted
                        places.computeIfAbsent(locality.withRayon(null).withOblast(null), p -> new HashSet<>()).add(position);
                    }
                }
            }
        }
//        places.keySet().stream().sorted().forEach(System.out::println);
    }

    public List<Position> geocode(Locality locality) {
        return places.getOrDefault(locality.withGromada(null), emptySet()).stream().toList();
    }

    public Position geocodeByDistance(Locality locality, Position anchor, int preferedDistance) {
        var position = searchInCache(locality, anchor, preferedDistance);
        if (position == null && locality.settlement() != null) {
            var geocodeResult = googleClient.geocode(locality);
            if (geocodeResult.isPresent()) {
                registerLocalityPosition(locality, geocodeResult.get().position());
                position = searchInCache(locality, anchor, preferedDistance);
            }
        }
        return position;
    }

    private void registerLocalityPosition(Locality locality, Position position) {
        places.computeIfAbsent(locality, p -> new HashSet<>()).add(position);
        places.computeIfAbsent(locality.withRayon(null), p -> new HashSet<>()).add(position);
        places.computeIfAbsent(locality.withOblast(null), p -> new HashSet<>()).add(position);
    }

    private Position searchInCache(Locality locality, Position closestTo, int distance) {
        return geocode(locality).stream()
                .min(comparing(position -> Math.abs(position.distanceTo(closestTo) - distance)))
                .orElse(null);
    }

    private static List<Path> listGeocodeDir() throws IOException {
        try (Stream<Path> stream = Files.list(GEOCODE_DIR).filter(Files::isRegularFile)) {
            return stream.toList();
        }
    }
}
