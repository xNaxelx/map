package noescape;

import noescape.model.BorderSign;
import noescape.model.CourtCase;
import noescape.model.Locality;
import noescape.model.Position;
import noescape.repository.AdministrativeAreaRepository;
import noescape.repository.BorderSignRepository;
import noescape.repository.DataRepository;
import noescape.service.GeocodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static noescape.util.CoordUtils.*;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public class Geocoder {

    public static void main(String[] args) throws IOException {
        new Geocoder().geocodeAll();
    }

    public static final Path INPUT_FILE = Path.of("data.tsv");
    public static final Path OUTPUT_FILE = Path.of("data-geocoded.tsv");

    private static final Logger log = LoggerFactory.getLogger(Geocoder.class);
    private final GeocodeService geocodeService = new GeocodeService();
    private final GoogleClient googleClient = new GoogleClient();
    private final AdministrativeAreaRepository administrativeAreaRepository = new AdministrativeAreaRepository();
    private final BorderSignRepository borderSignRepository = new BorderSignRepository();
    private final Normalizer normalizer = new Normalizer();

    public Geocoder() throws IOException {
    }

    public void geocodeAll() throws IOException {
        var cases = DataRepository.getCourtCases().stream()
                .filter(courtCase -> !courtCase.appeal())
                .filter(courtCase -> courtCase.arrestDate() == null || courtCase.arrestDate().getYear() >= 2022)
//                .filter(courtCase -> "118164734".equals(courtCase.caseId()))
                .map(normalizer::normalize)
                .map(this::geocode)
                .toList();

        String tsv = toTsv(cases);
        Files.writeString(OUTPUT_FILE, tsv);
        log.info("Geocoding complete");
    }

    private CourtCase geocode(CourtCase courtCase) {
        if (courtCase.position() != null) {
            return courtCase;
        }
        String borderSignCountry = courtCase.country();
        if ("Чернігівська".equals(courtCase.locality().oblast()) && "BY".equals(borderSignCountry)) {
            borderSignCountry = "BY2";
        }
        var borderSign = borderSignRepository.getBorderSign(borderSignCountry, substringBefore(courtCase.borderSign(), '-'));
        if (borderSign == null && contains(courtCase.borderSign(), '/')) {
//            throw new RuntimeException(courtCase.caseId() + ' ' + courtCase.country() + ' ' + courtCase.borderSign());
            borderSign = borderSignRepository.getBorderSign(courtCase.country(), substringBefore(courtCase.borderSign(), '/'));
        }
        var position = getArrestPosition(courtCase.locality(), courtCase.distance(), borderSign);
        return courtCase.withPosition(position);
    }


    private Position getArrestPosition(Locality locality, Integer distance, BorderSign borderSign) {
        Position settlementPosition = null;
        int minDistance = 0;

        if (borderSign != null && locality.settlement() != null && distance != null) {
            settlementPosition = geocodeService.geocodeByDistance(locality, borderSign.position(), distance);
            if (settlementPosition == null && ("BY".equals(borderSign.country()) || "RU".equals(borderSign.country()))) {
                borderSign = null; // FIXME Border signs on BY and RU borders is a mess
            } else {
                minDistance = 10000;
                if (settlementPosition != null) {
                    // sanity check: if the found place is too faraway from requested
                    double diff = Math.abs(settlementPosition.distanceTo(borderSign.position()) - distance);
                    if (diff > 10000 && diff > distance * 0.3)  {
                        settlementPosition = null;
                    }
                }
            }
        }


            if (borderSign != null && locality.settlement() != null) {
//            // if border sign is given, search the locality near the border sign
//            int maxDistance = distance == null ? 50000 : Math.max(50000, distance * 2); // search within at least 50km
//            settlementPosition = geocodeService.geocode(locality, borderSign.position(), maxDistance);
//
//            // Maybe rayon has been renamed
//            if (settlementPosition == null) {
////                settlementPosition = geocodeService.geocode(locality.withRayon(null), borderSign.position(), maxDistance);
//            }
        } else {
//            // Geocode by locality only
//            List<Position> geocoded = geocodeService.geocode(locality);
//            if (geocoded.size() == 1) {
//                settlementPosition = geocoded.getFirst();
//            }
//            // Maybe rayon has been renamed
//            geocoded = geocodeService.geocode(locality.withRayon(null));
//            if (geocoded.size() == 1) {
//                settlementPosition = geocoded.getFirst();
//            }
        }

        if (settlementPosition == null && locality.oblast() != null) {
            if (locality.settlement() != null) {
                settlementPosition = googleClient.geocode(locality).map(GoogleClient.GeocodeResult::position).orElse(null);
                minDistance = 10000;
            } else if (locality.gromada() != null) { // Gromada is typically small enough, so we can take its center is approx position
                String gromadaCenter = administrativeAreaRepository.getGromadaCenter(locality.gromada(), locality.oblast());
                if (gromadaCenter != null) {
                    settlementPosition = googleClient.geocode(locality.withSettlement(gromadaCenter)).map(GoogleClient.GeocodeResult::position).orElse(null);
                    minDistance = 15000;
                }
            }
        }

        // If we don't know the distance, the settlement position is all we have
        if (distance == null) return settlementPosition;

        // After 10/15 km we don't trust the `distance` value and use the settlement position as the arrest position
        if (settlementPosition != null && distance >= minDistance) {
            return settlementPosition;
        }

        // If we don't know the border sign, we can't calculate position. Settlement position is all we have.
        if (borderSign == null) return settlementPosition;


        // If we don't know neither settlement nor gromada, use rayon center as direction
        if (settlementPosition == null && locality.rayon() != null && distance > 10000) {
            String rayonCenter = administrativeAreaRepository.getRayonCenter(locality.rayon(), locality.oblast());
            if (rayonCenter != null) {
                settlementPosition = googleClient.geocode(locality.withSettlement(rayonCenter)).map(GoogleClient.GeocodeResult::position).orElse(null);
            }
        }

        // We don't trust big distances, so the settlement position is required
        if (distance > 50000 && settlementPosition == null) {
            return null;
        }

        // Lets compute the direction automatically
        var directionPosition = computeDirectionPosition(borderSign, distance);

        // If we failed to compute the direction and we don't have information about arrest place, there is nothing we can do
        if (directionPosition == null && settlementPosition == null) return null;

        // If we failed to compute direction, all we know is just settlement position
        if (directionPosition == null) {
            return calcPositionAtDistance(borderSign.position(), settlementPosition, distance);
        }

        // If distance is too short, use computed direction.
        if (distance < 500) {
            return calcPositionAtDistance(borderSign.position(), directionPosition, distance);
        }

        if (settlementPosition != null) {
            // If the settlement is far away, use it as direction
            double distanceToSettlement = calculateDistance(borderSign.position(), settlementPosition);
            if (distanceToSettlement > distance * distance) {
                return calcPositionAtDistance(borderSign.position(), settlementPosition, distance);
            }

            // If settlement is father to the border than double distance, correct the direction towards the settlement
            if (getDistanceToBorder(settlementPosition, borderSignRepository.getBorderSigns()) > distance * 2) {
                directionPosition = directionPosition.middle(directionPosition.middle(settlementPosition));
                return calcPositionAtDistance(borderSign.position(), directionPosition, distance);
            }
        }

        // Distance and computed direction is all we have. This is the least truthy position.
        return calcPositionAtDistance(borderSign.position(), directionPosition, distance);
    }


    private Position computeDirectionPosition(BorderSign from, int distance) {
        var prev = borderSignRepository.getPrev(from.key());
        var next = borderSignRepository.getNext(from.key());
        if (prev == null || next == null) return null;
        var minPrev = prev;
        var minNext = next;
        double minAngle = calcAngle(prev.position(), from.position(), next.position());

        while (prev != null && calculateDistance(from.position(), prev.position()) <= distance) {
            double angle = calcAngle(prev.position(), from.position(), minNext.position());
            if (angle < minAngle) {
                minAngle = angle;
                minPrev = prev;
            }
            prev = borderSignRepository.getPrev(prev.key());
        }

        while (next != null && calculateDistance(from.position(), next.position()) <= distance) {
            double angle = calcAngle(minPrev.position(), from.position(), next.position());
            if (angle < minAngle) {
                minAngle = angle;
                minNext = next;
            }
            next = borderSignRepository.getNext(next.key());
        }

        // if the line is nearly straight, return 90 degrees to left
        if (Math.abs(minAngle - Math.PI) < 0.001) {
            return getPerpendicularPosition(minPrev.position(), from.position());
        }

        Position sectorStart = minPrev.position();
        Position sectorEnd = minNext.position();

        if (minAngle > Math.PI) distance = -distance;

        Position prevDirection = calcPositionAtDistance(from.position(), sectorStart, distance);
        Position nextDirection = calcPositionAtDistance(from.position(), sectorEnd, distance);
        return prevDirection.middle(nextDirection);
    }

    public static String toTsv(List<CourtCase> courtCases) {
        StringBuilder csv = new StringBuilder();
        csv.append("caseId\tcaseNumber\tcountry\tborderSign\tarrestDate\tarrestTime\tpublicationDate\tdistance\tgroup\tfine\tguard\tcourt\tsettlement\tgromada\trayon\toblast\tposition\n");
        for (CourtCase courtCase : courtCases) {
            Locality locality = courtCase.locality();
            if (locality.settlement() == null && locality.gromada() == null && locality.rayon() == null && (courtCase.distance() == null || courtCase.borderSign() == null)) {
                continue;
            }
            csv.append(courtCase.caseId()).append("\t")
                    .append(Objects.toString(courtCase.caseNumber(), "")).append("\t")
                    .append(Objects.toString(courtCase.country(), "")).append("\t")
                    .append(Objects.toString(courtCase.borderSign(), "")).append("\t")
                    .append(Objects.toString(courtCase.arrestDate(), "")).append("\t")
                    .append(Objects.toString(courtCase.arrestTime(), "")).append("\t")
                    .append(Objects.toString(courtCase.publicationDate(), "")).append("\t")
                    .append(Objects.toString(courtCase.distance(), "")).append("\t")
                    .append(Objects.toString(courtCase.group(), "")).append("\t")
                    .append(Objects.toString(courtCase.fine(), "")).append("\t")
                    .append(Objects.toString(courtCase.guard(), "")).append("\t")
                    .append(Objects.toString(courtCase.court(), "")).append("\t")
                    .append(Objects.toString(locality.settlement(), "")).append("\t")
                    .append(Objects.toString(locality.gromada(), "")).append("\t")
                    .append(Objects.toString(locality.rayon(), "")).append("\t")
                    .append(Objects.toString(locality.oblast(), "")).append("\t")
                    .append(Objects.toString(courtCase.position(), "")).append("\n");
        }
        return csv.toString();
    }

    private static int getDistanceToBorder(Position position, List<BorderSign> borderSigns) {
        double min = Integer.MAX_VALUE;
        for (BorderSign borderSign : borderSigns) {
            double distance = calculateDistance(position, borderSign.position());
            min = Math.min(min, distance);
        }
        return (int) min;
    }


}
