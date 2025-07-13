package noescape.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public class ListUtils {

    public static <T> T firstMostFrequent(List<T> list) {
        List<T> mostFrequent = mostFrequent(list, element -> element);
        return mostFrequent.isEmpty() ? null : mostFrequent.getFirst();
    }

    public static <T> List<T> mostFrequent(List<T> list, Function<T, Object> valueExtractor) {
        if (list.isEmpty()) {
            return emptyList();
        }

        var counts = new int[list.size()];
        var values = new Object[list.size()];
        outer:
        for (int i = 0; i < counts.length; i++) {
            var value = valueExtractor.apply(list.get(i));
            values[i] = value;
            for (int j = 0; j < i; j++) {
                if (Objects.equals(values[j], value)) {
                    counts[j]++;
                    continue outer;
                }
            }
            counts[i] = 1;
        }

        int maxCount = 0;
        for (int count : counts) {
            if (count > maxCount) {
                maxCount = count;
            }
        }
        var result = new ArrayList<T>();
        for (int i = 0; i < counts.length; i++) {
            int count = counts[i];
            if (count == maxCount) {
                result.add(list.get(i));
            }
        }
        return result;
    }
}
