package noescape.data;

import noescape.SiteGenerator;
import noescape.model.CourtCase;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class TsvGenerator {

    public static String generateArrestsTxt(List<Arrest> arrests) {
        arrests = SiteGenerator.distributeSamePositions(arrests);
        var sb = new StringBuilder();
        for (var arrest : arrests) {
            var distance = getDistance(arrest.cases());
            var country = getCountry(arrest.cases());
            var borderSign = getBorderSign(arrest.cases());
            var caseIds = arrest.cases().stream().map(CourtCase::caseId).collect(joining(","));
            sb.append(arrest.arrestDate()).append("\t");
            sb.append(Objects.toString(arrest.arrestTime(), "")).append('\t');
            sb.append(trimToEmpty(country)).append('\t');
            sb.append(trimToEmpty(borderSign).replace("BY2", "BY")).append('\t');
            sb.append(Objects.toString(distance, "")).append('\t');
            sb.append(arrest.arrestPosition().lat()).append('\t');
            sb.append(arrest.arrestPosition().lng()).append('\t');
            sb.append(caseIds).append('\n');
        }
        return sb.toString();
    }

    public static String generateCasesTxt(List<Arrest> arrests) {
        StringBuilder sb = new StringBuilder();
        for (Arrest arrest : arrests) {
            for (var courtCase : arrest.cases()) {
                sb.append(courtCase.caseId()).append('\t');
                sb.append(courtCase.publicationDate()).append('\t');
                sb.append(trimToEmpty(courtCase.fine())).append('\n');
            }
        }
        return sb.toString();
    }

    private static Integer getDistance(List<CourtCase> courtCases) {
        Map<Integer, Integer> distanceCounts = new HashMap<>();
        int maxCount = 0;
        Integer distance = null;
        for (var courtCase : courtCases) {
            int count = distanceCounts.computeIfAbsent(courtCase.distance(), k -> 0) + 1; // TODO k -> 1
            distanceCounts.put(courtCase.distance(), count);
            if (count > maxCount) {
                maxCount = count;
                distance = courtCase.distance();
            }
        }
        return distance;
    }

    private static String getCountry(List<CourtCase> courtCases) {
        Map<String, Integer> countryCounts = new HashMap<>();
        int maxCount = 0;
        String country = null;
        for (var courtCase : courtCases) {
            int count = countryCounts.computeIfAbsent(courtCase.country(), k -> 0) + 1;
            countryCounts.put(courtCase.country(), count);
            if (count > maxCount) {
                maxCount = count;
                country = courtCase.country();
            }
        }
        return StringUtils.replace(country, "BY2", "BY");
    }

    private static String getBorderSign(List<CourtCase> courtCases) {
        Map<String, Integer> borderSignCounts = new HashMap<>();
        int maxCount = 0;
        String borderSign = null;
        for (var courtCase : courtCases) {
            int count = borderSignCounts.computeIfAbsent(courtCase.borderSign(), k -> 0) + 1;
            borderSignCounts.put(courtCase.borderSign(), count);
            if (count > maxCount) {
                maxCount = count;
                borderSign = courtCase.borderSign();
            }
        }
        return borderSign;
    }

}
