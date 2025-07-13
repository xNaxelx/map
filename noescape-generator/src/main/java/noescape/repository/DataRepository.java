package noescape.repository;

import noescape.Geocoder;
import noescape.model.CourtCase;
import noescape.model.Locality;
import noescape.model.Position;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.*;

public class DataRepository {

    public static List<CourtCase> getCourtCases() throws IOException {
        if (!Files.exists(Geocoder.INPUT_FILE)) return new ArrayList<>();

        var lines = Files.readAllLines(Geocoder.INPUT_FILE);

        var iterator = lines.iterator();
        var headerLine = iterator.next();
        var headers = asList(splitPreserveAllTokens(headerLine, "\t"));

        var cases = new ArrayList<CourtCase>(lines.size());
        while (iterator.hasNext()) {
            var line = iterator.next();
            var values = Arrays.copyOf(splitPreserveAllTokens(line, "\t"), headers.size());
            var caseId = values[headers.indexOf("caseId")];
            var caseNumber = trimToNull(values[headers.indexOf("caseNumber")]);
            var appeal = parseBoolean(values[headers.indexOf("appeal")]);
            var country = trimToNull(values[headers.indexOf("country")]);
            var borderSign = trimToNull(values[headers.indexOf("borderSign")]);
            var arrestDateStr = trimToNull(values[headers.indexOf("arrestDate")]);
            var arrestTimeStr = trimToNull(values[headers.indexOf("arrestTime")]);
            var registrationDateStr = trimToNull(values[headers.indexOf("registrationDate")]);
            var publicationDateStr = trimToNull(values[headers.indexOf("publicationDate")]);
            var distanceStr = trimToNull(values[headers.indexOf("distance")]);
            var guardType = trimToNull(values[headers.indexOf("guard")]);
            var groupStr = trimToNull(values[headers.indexOf("group")]);
            var fine = trimToNull(values[headers.indexOf("fine")]);
            var court = trimToNull(values[headers.indexOf("court")]);
            var settlement = trimToNull(values[headers.indexOf("settlement")]);
            var gromada = trimToNull(values[headers.indexOf("gromada")]);
            var rayon = trimToNull(values[headers.indexOf("rayon")]);
            var oblast = trimToNull(values[headers.indexOf("oblast")]);
            var position = Position.parse(trimToNull(values[headers.indexOf("position")]));
            var locality = new Locality(settlement, gromada, rayon, oblast);

            var arrestDate = arrestDateStr == null ? null : LocalDate.parse(arrestDateStr);
            var arrestTime = arrestTimeStr == null ? null : LocalTime.parse(arrestTimeStr);
            var registrationDate = registrationDateStr == null ? null : LocalDate.parse(registrationDateStr);
            var publicationDate = publicationDateStr == null ? null : LocalDate.parse(publicationDateStr);
            var distance = distanceStr == null ? null : Integer.valueOf(distanceStr);
            var group = groupStr == null ? null : Boolean.valueOf(groupStr);

            var courtCase = new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guardType, group, position);

            cases.add(courtCase);
        }
        return cases;
    }

    public static void saveCourtCases(List<CourtCase> courtCases) throws IOException {
        Files.writeString(Path.of("data.tsv"), toTsv(courtCases));
    }

    public static String toTsv(List<CourtCase> courtCases) {
        StringBuilder csv = new StringBuilder();
        csv.append("caseId\tcaseNumber\tappeal\tcountry\tborderSign\tarrestDate\tarrestTime\tregistrationDate\tpublicationDate\tdistance\tgroup\tfine\tguard\tcourt\tsettlement\tgromada\trayon\toblast\tposition\n");
        courtCases.stream().sorted(comparing(CourtCase::caseId)).forEach(courtCase -> {
            Locality locality = courtCase.locality();
            csv.append(courtCase.caseId()).append("\t")
                    .append(Objects.toString(courtCase.caseNumber(), "")).append("\t")
                    .append(Objects.toString(courtCase.appeal(), "")).append("\t")
                    .append(Objects.toString(courtCase.country(), "")).append("\t")
                    .append(Objects.toString(courtCase.borderSign(), "")).append("\t")
                    .append(Objects.toString(courtCase.arrestDate(), "")).append("\t")
                    .append(Objects.toString(courtCase.arrestTime(), "")).append("\t")
                    .append(Objects.toString(courtCase.registrationDate(), "")).append("\t")
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
        });
        return csv.toString();
    }
}
