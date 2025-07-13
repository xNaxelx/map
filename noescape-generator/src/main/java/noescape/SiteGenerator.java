package noescape;

import noescape.data.Arrest;
import noescape.model.BorderSign;
import noescape.model.CourtCase;
import noescape.model.Locality;
import noescape.model.Position;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Comparator.*;
import static noescape.data.SignGenerator.generateSignsTxt;
import static noescape.data.StatsGenerator.generateStats;
import static noescape.data.TsvGenerator.generateArrestsTxt;
import static noescape.data.TsvGenerator.generateCasesTxt;
import static org.apache.commons.lang3.StringUtils.splitPreserveAllTokens;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class SiteGenerator {
    private static final double LAT_PER_METER = 1.0 / 111320;
    private static final double LNG_PER_METER = 1.0 / 71700;

    private static final Logger log = LoggerFactory.getLogger(SiteGenerator.class);

    public static void main(String[] args) throws IOException {
        var allCourtCases = readCourtCases();
        var geocodedCases = allCourtCases.stream()
                .filter(courtCase -> courtCase.arrestDate() != null && courtCase.position() != null)
                .toList();

        var lastPublicationDate = allCourtCases.stream().map(CourtCase::publicationDate).max(LocalDate::compareTo).get();
        var latestCases = geocodedCases.stream()
                .filter(courtCase -> courtCase.arrestDate().isAfter(lastPublicationDate.minusYears(1)))
                .sorted(comparing(CourtCase::arrestDate).reversed())
                .limit(9990)
                .toList();

        var latestArrests = collectArrests(latestCases);

        Files.createDirectories(Path.of("target/data"));
        Files.writeString(Path.of("target/data/arrests.txt"), generateArrestsTxt(latestArrests));
        Files.writeString(Path.of("target/data/cases.txt"), generateCasesTxt(latestArrests));

        var signs = getBorderSigns(geocodedCases);

        Files.writeString(Path.of("target/data/signs.txt"), generateSignsTxt(signs));
        Files.writeString(Path.of("target/data/stats.tsv"), generateStats(allCourtCases));

        log.info("Site data generated");
    }

    private static Set<BorderSign.Key> getBorderSigns(List<CourtCase> courtCases) {
        return courtCases.stream()
                .filter(courtCase -> courtCase.country() != null)
                .filter(courtCase -> courtCase.borderSign() != null)
                .flatMap(courtCase -> Stream.of(StringUtils.split(courtCase.borderSign(), '-'))
                        .map(sign -> BorderSign.key(courtCase.country(), sign)))
                .collect(Collectors.toSet());
    }

    private static List<Arrest> collectArrests(List<CourtCase> courtCases) {
        record ArrestKey(LocalDate arrestDate, LocalTime arrestTime, Position arrestPosition) {
        }
        var arrests = new HashMap<ArrestKey, Arrest>();
        for (var courtCase : courtCases) {
            var arrestKey = new ArrestKey(courtCase.arrestDate(), courtCase.arrestTime(), courtCase.position());
            arrests.computeIfAbsent(arrestKey, k -> new Arrest(k.arrestDate, k.arrestTime, k.arrestPosition, new ArrayList<>())).cases().add(courtCase);
        }
        return new ArrayList<>(arrests.values());
    }

    private static List<CourtCase> readCourtCases() throws IOException {
        var lines = Files.readAllLines(Path.of("data-geocoded.tsv"));
        var iterator = lines.iterator();
        var headerLine = iterator.next();
        var headers = asList(headerLine.split("\t"));
        int caseIdIndex = headers.indexOf("caseId");
        int caseNumberIndex = headers.indexOf("caseNumber");
        int countryIndex = headers.indexOf("country");
        int borderSignIndex = headers.indexOf("borderSign");
        int arrestDateIndex = headers.indexOf("arrestDate");
        int arrestTimeIndex = headers.indexOf("arrestTime");
        int publicationDateIndex = headers.indexOf("publicationDate");
        int distanceIndex = headers.indexOf("distance");
        int guardTypeIndex = headers.indexOf("guard");
        int groupIndex = headers.indexOf("group");
        int fineIndex = headers.indexOf("fine");
        int settlementIndex = headers.indexOf("settlement");
        int gromadaIndex = headers.indexOf("gromada");
        int rayonIndex = headers.indexOf("rayon");
        int oblastIndex = headers.indexOf("oblast");
        int positionIndex = headers.indexOf("position");

        List<CourtCase> result = new ArrayList<>();
        while (iterator.hasNext()) {
            var line = iterator.next();
            var values = Arrays.copyOf(splitPreserveAllTokens(line, '\t'), 18);
            var caseId = values[caseIdIndex];
            var caseNumber = trimToNull(values[caseNumberIndex]);
            var country = trimToNull(values[countryIndex]);
            var borderSign = trimToNull(values[borderSignIndex]);
            var arrestDateStr = values[arrestDateIndex];
            var arrestDate = arrestDateStr.isEmpty() ? null : LocalDate.parse(values[arrestDateIndex]);
            var arrestTimeStr = values[arrestTimeIndex];
            var arrestTime = arrestTimeStr.isEmpty() ? null : LocalTime.parse(values[arrestTimeIndex]);
            var publicationDate = LocalDate.parse(values[publicationDateIndex]);
            var distanceStr = values[distanceIndex];
            var distance = distanceStr.isEmpty() ? null : Integer.valueOf(distanceStr);
            var guardType = values[guardTypeIndex];
            var groupStr = values[groupIndex];
            var group = groupStr.isEmpty() ? null : Boolean.valueOf(groupStr);
            var fine = values[fineIndex];
            var settlement = trimToNull(values[settlementIndex]);
            var gromada = trimToNull(values[gromadaIndex]);
            var rayon = trimToNull(values[rayonIndex]);
            var oblast = trimToNull(values[oblastIndex]);
            var position = Position.parse(trimToNull(values[positionIndex]));
            if (position != null) {
                position = position.round(6);
            }
            var locality = new Locality(settlement, gromada, rayon, oblast);
            result.add(new CourtCase(caseId, caseNumber, false, country, borderSign, arrestDate, arrestTime, null, publicationDate, null, locality, fine, distance, guardType, group, position));
        }
        return result;
    }


    public static List<Arrest> distributeSamePositions(List<Arrest> arrests) {
        Map<Position, List<Arrest>> positionMap = new HashMap<>();
        for (var arrest : arrests) {
            if (arrest.arrestPosition() != null) {
                positionMap.computeIfAbsent(arrest.arrestPosition(), k -> new ArrayList<>()).add(arrest);
            }
        }
        List<Arrest> result = new ArrayList<>();
        for (var entry : positionMap.entrySet()) {
            var position = entry.getKey();
            var positionArrests = entry.getValue();
            int side = (int) Math.ceil(Math.sqrt(positionArrests.size()));
            for (int i = 0; i < positionArrests.size(); i++) {
                var arrest = positionArrests.get(i);
                int row = i / side;
                int col = i % side;
                double lat = position.lat() + (row - side / 2.0) * LAT_PER_METER * 20;
                double lng = position.lng() + (col - side / 2.0) * LNG_PER_METER * 20;
                Position newPosition = new Position(lat, lng).round(6);
                result.add(arrest.withPosition(newPosition));
            }
        }
        Comparator<Arrest> comparator = comparing(Arrest::arrestDate)
                .thenComparing(Arrest::arrestTime, nullsFirst(naturalOrder()))
                .thenComparing(arrest -> arrest.cases().getFirst().caseId());
        result.sort(comparator);
        return result;
    }
}
