package noescape.data;

import noescape.model.CourtCase;
import org.apache.commons.lang3.StringUtils;

import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;

public class StatsGenerator {
    public static String generateStats(List<CourtCase> courtCases) {
        Map<YearMonth, Map<String, LongAdder>> statistics = new TreeMap<>();
        for (CourtCase courtCase : courtCases) {
            if (courtCase.arrestDate() != null) {
                statistics
                        .computeIfAbsent(YearMonth.from(courtCase.arrestDate()), ym -> new HashMap<>())
                        .computeIfAbsent(StringUtils.replace(courtCase.country(), "BY2", "BY"), c -> new LongAdder())
                        .increment();
            }
        }

        var countries = statistics.values().stream().map(Map::keySet).flatMap(Collection::stream).filter(Objects::nonNull).distinct().sorted().toList();

        Map<String, LongAdder> accum = new HashMap<>();
        for (String country : countries) {
            accum.put(country, new LongAdder());
        }

        var sb = new StringBuilder();
        sb.append("month");
        for (String country : countries) {
            sb.append('\t').append(country);
        }
        sb.append('\n');
        for (var entry : statistics.entrySet()) {
            sb.append(entry.getKey());
            for (String country : countries) {
                LongAdder longAdder = accum.get(country);
                longAdder.add(entry.getValue().computeIfAbsent(country, s -> new LongAdder()).longValue());
                sb.append('\t').append(longAdder);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
