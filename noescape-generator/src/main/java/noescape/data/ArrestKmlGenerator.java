package noescape.data;

import noescape.model.CourtCase;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;

public class ArrestKmlGenerator {

    public static String generateArrestKlm(List<Arrest> arrests) {
        var groupedByMonth = new TreeMap<>(arrests.stream().collect(groupingBy(courtCase -> YearMonth.from(courtCase.arrestDate()))));
        var sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2" xmlns:atom="http://www.w3.org/2005/Atom">
                <Document>
                """);
        var yearMonths = new ArrayList<>(groupedByMonth.keySet());
        int count = 0;
        for (var yearMonth : yearMonths) {
            var monthArrests = groupedByMonth.get(yearMonth);
            count += monthArrests.size();
            String folder = yearMonth.toString();
            sb.append("\n    <Folder>");
            sb.append("\n        <name>").append(folder).append("</name>");
            sb.append("\n        <open>0</open>");
            if (yearMonth.isBefore(YearMonth.now().minusMonths(3))) {
                sb.append("\n        <visibility>0</visibility>");
            }
            for (var arrest : monthArrests) {
                sb.append("\n        <Placemark>");
                sb.append("<name>").append(arrest.arrestDate().format(DateTimeFormatter.ofPattern("dd.MM.yy"))).append(" ").append(arrest.arrestTime()).append("</name>");
                sb.append("<Style><LabelStyle><scale>0.8</scale></LabelStyle></Style>");
                sb.append("<description>");
                for (CourtCase courtCase : arrest.cases()) {
                    sb.append("\n<a href=\"https://reyestr.court.gov.ua/Review/").append(courtCase.caseId()).append("\">Справа №").append(courtCase.caseNumber()).append("</a><br/>");
                }
                sb.append("</description>");
                sb.append("<Point><coordinates>").append(arrest.arrestPosition().lng()).append(",").append(arrest.arrestPosition().lat()).append("</coordinates></Point>");
                sb.append("</Placemark>");
            }
            sb.append("\n    </Folder>\n");
        }
        sb.append("""
                </Document>
                </kml>
                """);
        return sb.toString();
    }

}
