package noescape.repository;

import noescape.model.Gromada;
import noescape.model.Rayon;
import noescape.model.Settlement;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.nio.file.Files.readAllLines;
import static java.util.Objects.requireNonNull;
import static noescape.service.Lemmatizer.*;
import static org.apache.commons.lang3.StringUtils.*;

public class AdministrativeAreaRepository {
    private final Map<String, String> gromadaOblastCountries = new HashMap<>();
    private final Map<String, String> gromadaCountries = new HashMap<>();
    public final List<Map<String, String>> gromadaList = new ArrayList<>();
    private final List<Map<String, String>> rayonList = new ArrayList<>();

    private final Map<String, String> oblasts = new HashMap<>();
    private final Map<String, String> rayons = new HashMap<>();
    private final Map<String, String> gromadas = new HashMap<>();
    private final Map<String, String> settlements = new HashMap<>();
    private final Map<String, TreeMap<String, String>> oblastRayons = new TreeMap<>();
    private final Map<String, TreeMap<String, String>> oblastGromadas = new TreeMap<>();
    private final Map<String, TreeMap<String, String>> oblastSettlements = new TreeMap<>();


    private final Map<String, List<String>> levenshteinCache = new HashMap<>();


    public AdministrativeAreaRepository() throws IOException {
        var lines = readAllLines(Path.of("gromada.tsv"));
        var iterator = lines.iterator();
        iterator.next(); // skip header
        while (iterator.hasNext()) {
            String line = iterator.next();
            String[] values = Arrays.copyOf(splitPreserveAllTokens(line, '\t'), 6);
            String gromada = values[0];
            String center = values[2].replace("місто ", "").replace("с-ще ", "").replace("село ", "");
            String rayon = values[3];
            String oblast = values[4];
            String country = trimToNull(values[5]);
            if (country != null) {
                gromadaOblastCountries.put(gromada + "," + oblast, country);
                gromadaCountries.put(gromada, country);
            }
            gromadaList.add(Map.of(
                    "gromada", gromada,
                    "rayon", rayon,
                    "oblast", oblast,
                    "center", substringAfter(values[2], " ")
            ));
            register(oblast, oblasts);
            register(rayon, rayons);
            register(gromada, gromadas);
            register(center, settlements);
            registerOblastGromada(oblast, gromada);
        }

        lines = readAllLines(Path.of("gazetteer.tsv"));
        for (String line : lines) {
            String[] values = line.split("\t");
            String settlement = values[1];
            if (settlement.startsWith("с-ще") || settlement.startsWith("х.") || settlement.startsWith("хутір") ) {
                continue;
            }
            String oblast = values[5].replace(" обл.", "").replace("Автономна Республіка", "").trim();
            register(oblast, oblasts);
            if (values.length > 6) {
                String rayon = values[6].replace("район", "").replace("р-н", "").replace("р-он", "").trim();
                if (!rayon.startsWith("м. ") && !rayon.startsWith("міськрада") && rayon.endsWith("ий")) {
                    register(rayon, rayons);
                    registerOblastRayon(oblast, rayon);
                }
            }
            register(settlement, settlements);
            registerOblastSettlement(oblast, settlement);
        }

        for (String line : Files.readAllLines(Path.of("regions.tsv"))) {
            String[] values = splitPreserveAllTokens(line, "\t");
            String rayon = values[0];
            String center = values[1].replace("смт ", "");
            String oblast = values[2];
            rayonList.add(Map.of(
                    "rayon", rayon,
                    "center", center,
                    "oblast", oblast
            ));
            register(rayon, rayons);
            registerOblastSettlement(oblast, center);
            registerOblastRayon(oblast, rayon);
        }
    }

    private void registerOblastGromada(String oblast, String gromada) {
        oblast = requireNonNull(normalizeOblast(oblast), oblast);
        var map = oblastGromadas.computeIfAbsent(oblast, r -> new TreeMap<>());
        register(gromada, map);
    }

    private void registerOblastRayon(String oblast, String rayon) {
        oblast = requireNonNull(normalizeOblast(oblast), oblast);
        var map = oblastRayons.computeIfAbsent(oblast, r -> new TreeMap<>());
        register(rayon, map);
    }

    private void registerOblastSettlement(String oblast, String settlement) {
        oblast = requireNonNull(normalizeOblast(oblast), oblast);
        var map = oblastSettlements.computeIfAbsent(oblast, r -> new TreeMap<>());
        register(settlement, map);
    }

    private void register(String name, Map<String, String> map) {
        String fullKey = fullKey(name);
        map.put(fullKey, name);

        String abbrKey = abbrKey(name);
        if (abbrKey != null) {
            map.put(abbrKey, name);
        }
        String lastWordKey = lastWordKey(name);
        if (lastWordKey != null) {
            map.put(lastWordKey, name);
        }
    }

    public String normalizeOblast(String input) {
        if (input == null) return null;
        input = lemmatizeOblast(input);
        return findBestMatch(input, oblasts);
    }

    public Rayon normalizeRayon(String rayon, Collection<String> oblasts) {
        if (rayon == null) return null;
        rayon = lemmatizeRayon(rayon);

        // FIXME move it somewhere else
        if (rayon.equals("Володимирський") && oblasts.contains("Волинська")) {
            rayon = "Володимир-Волинський";
        }
            for (var oblast : oblasts) {
            var bestMatch = findBestMatch(rayon, oblastRayons.get(oblast));
            if (bestMatch != null) {
                return new Rayon(bestMatch, oblast);
            }
        }
        return null;
    }

    public String normalizeRayon(String input) {
        if (input == null) return null;
        input = lemmatizeRayon(input);
        return findBestMatch(input, rayons);
    }

    public Gromada normalizeGromada(String gromada, Collection<String> oblasts) {
        if (gromada == null) return null;
        gromada = lemmatizeGromada(gromada);
        for (var oblast : oblasts) {
            var bestMatch = findBestMatch(gromada, oblastGromadas.get(oblast));
            if (bestMatch != null) {
                return new Gromada(bestMatch, oblast);
            }
        }
        return null;
    }

    public Settlement normalizeSettlement(String settlement, Collection<String> oblasts) {
        if (settlement == null) return null;
        for (var oblast : oblasts) {
            var bestMatch = findBestMatch(settlement, oblastSettlements.get(oblast));
            if (bestMatch != null) {
                return new Settlement(bestMatch, oblast);
            }
        }
        return null;
    }

    public String normalizeSettlement(String input) {
        if (input == null) return null;
        return findBestMatch(input, settlements);
    }

    private String findBestMatch(String input, Map<String, String> map) {
        // Search by full key
        String fullKey = fullKey(input.trim());
        String result = map.get(fullKey);
        if (result != null) {
            return result;
        }

        // Search by abbreviated key
        String abbrKey = abbrKey(input);
        if (abbrKey != null) {
            result = map.get(abbrKey);
            if (result != null) {
                return result;
            }
        }

        // Search by abbreviated key
        String lastWordKey = lastWordKey(input);
        if (lastWordKey != null) {
            result = map.get(lastWordKey);
            if (result != null) {
                //return result;
            }
        }

        // The name may contain a typo, use Levenshtein Distance to find correct key
        String correctKey = findByLevenshteinDistance(fullKey, map);
        return correctKey == null ? null : map.get(correctKey);
    }

    private String findByLevenshteinDistance(String key, Map<String, String> map) {
        var levenshteinDistance = LevenshteinDistance.getDefaultInstance();
        List<String> result = levenshteinCache.get(key);
        if (result == null) {
            result = new ArrayList<>();
            for (String correctKey : map.keySet()) {
                int distance = levenshteinDistance.apply(key, correctKey);
                if (distance == 0) { // Exact match
                    return map.get(key);
                }
                if (distance == 1) { // Single typo
                    result.add(correctKey);
                }
            }
            levenshteinCache.put(key, result);
        }
        if (result.isEmpty()) {
            return null; // Not found
        }
        if (result.size() > 1) {
            return null; // Too many similar keys found - the result is inconclusive
        }
        return result.getFirst();
    }

    public String[] getCountryByGromadaAndOblast(String gromada, String oblast) {
        return StringUtils.split(gromadaOblastCountries.getOrDefault(gromada + "," + oblast, ""), ',');
    }

    public String getCountryByGromada(String gromada) {
        return gromadaCountries.get(gromada);
    }

    public boolean rayonExists(String rayon, String oblast) {
        for (var map : gromadaList) {
            if (map.get("rayon").equals(rayon) && map.get("oblast").equals(oblast)) {
                return true;
            }
        }
        for (var map : rayonList) {
            if (map.get("rayon").equals(rayon) && map.get("oblast").equals(oblast)) {
                return true;
            }
        }
        return false;
    }

    public boolean rayonUnique(String rayon) {
        int count = 0;
        for (var map : rayonList) {
            if (map.get("rayon").equals(rayon)) {
                count++;
            }
            if (count > 1) break;
        }
        return count == 1;
    }

    public String getRayonByCenterAndOblast(String settlement, String oblast) {
        String rayon = null;
        for (var map : rayonList) {
            if (map.get("center").equals(settlement) && map.get("oblast").equals(oblast)) {
                if (rayon != null) return null;
                rayon = map.get("rayon");
            }
        }
        return rayon;
    }

    public String getOblastByGromadaAndRayon(String gromada, String rayon) {
        for (var map : gromadaList) {
            if (map.get("gromada").equals(gromada) && map.get("rayon").equals(rayon)) {
                return map.get("oblast");
            }
        }
        return null;
    }

    public String getRayonByGromada(String gromada) {
        String rayon = null;
        for (var map : gromadaList) {
            if (map.get("gromada").equals(gromada)) {
                if (rayon != null) return null; // Multiple gromadas with the same name
                rayon = map.get("rayon");
            }
        }
        return rayon;
    }

    public String getOblastByRayon(String rayon) {
        String oblast = null;
        for (var map : rayonList) {
            if (map.get("rayon").equals(rayon)) {
                if (oblast != null) return null; // Multiple rayons with the same name
                oblast = map.get("oblast");
            }
        }
        return oblast;
    }

    public String getRayonByGromadaAndOblast(String gromada, String oblast) {
        for (var map : gromadaList) {
            if (map.get("gromada").equals(gromada) && map.get("oblast").equals(oblast)) {
                return map.get("rayon");
            }
        }
        return null;
    }

    public String getGromadaByCenterAndRayon(String settlement, String rayon) {
        for (var map : gromadaList) {
            if (map.get("center").equals(settlement) && map.get("rayon").equals(rayon)) {
                return map.get("gromada");
            }
        }
        return null;
    }

    public String getGromadaCenter(String gromada, String oblast) {
        for (var map : gromadaList) {
            if (map.get("gromada").equals(gromada) && map.get("oblast").equals(oblast)) {
                return map.get("center");
            }
        }
        return null;
    }

    public String getRayonCenter(String rayon, String oblast) {
        for (var map : rayonList) {
            if (map.get("rayon").equals(rayon) && map.get("oblast").equals(oblast)) {
                return map.get("center");
            }
        }
        return null;
    }

    private static String fullKey(String name) {
        return name.toLowerCase().replace('-', ' ');
    }

    private static String abbrKey(String name) {
        int space = name.indexOf(' ');
        if (space > 0) {
            String fullKey = fullKey(name);
            return fullKey.charAt(0) + ". " + fullKey.substring(space + 1);
        }
        return null;
    }

    private static String lastWordKey(String name) {
        int hyphen = name.lastIndexOf('-');
        if (hyphen > 0) {
            return fullKey(name.substring(hyphen + 1));
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        new AdministrativeAreaRepository();
//        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
//        Integer distance = levenshteinDistance.apply("бистрець", "бстрецька");
//        System.out.println(distance);
    }

}
