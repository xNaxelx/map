package noescape;

import noescape.model.*;
import noescape.model.SpacyDocument.Entity;
import noescape.repository.AdministrativeAreaRepository;
import noescape.repository.DataRepository;
import noescape.repository.HtmlRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static noescape.model.SpacyDocument.Entity.havingLabel;
import static noescape.util.ListUtils.firstMostFrequent;

public class SpacyExtractor {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d\\d?)\\D+(\\d\\d)\\D*");
    private static final String HOUR_PATTERN = "(\\d\\d) год.*";
    private static final Pattern DISTANCE_PATTERN = Pattern.compile("([1-9]\\d*(?: \\d\\d\\d)?)(.*)");
    private static final Pattern FINE_PATTERN = Pattern.compile("([1-9]\\d*(?: \\d\\d\\d)?)(?:[,.]00)?");
    private static final Pattern CASE_NUMBER_PATTERN = Pattern.compile("\\d+/\\d+/\\d\\d");
    private static final Pattern[] BORDER_SIGN_PATTERNS = {
            Pattern.compile("\\d{1,4}(?:/\\d{1,3})?"),
            Pattern.compile("\\d(?:/\\d{1,3})?-\\d?\\d(?:/\\d{1,3})?"),
            Pattern.compile("\\d\\d(?:/\\d{1,3})?-\\d?\\d\\d(?:/\\d{1,3})?"),
            Pattern.compile("\\d\\d\\d(?:/\\d{1,3})?-\\d\\d\\d(?:/\\d{1,3})?"),
    };
    private static final Locale UK = Locale.of("uk");
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("\" d \" MMMM yyyy", UK),
            DateTimeFormatter.ofPattern("d MMMM yyyy", UK),
            DateTimeFormatter.ofPattern("dd MMMM yyyy", UK),
            DateTimeFormatter.ofPattern("d.MM.yyyy", UK),
            DateTimeFormatter.ofPattern("dd.MM.yyyy", UK),
            DateTimeFormatter.ofPattern("dd.MM.yy", UK),
    };

    private static final Position UNKNOWN = new Position(0, 0); 
    private static final Map<String, Position> STATIONS = Map.ofEntries(
            Map.entry("Абамелікове", new Position(47.97897506757566, 29.262880689715594)),
            Map.entry("Вапнярка", new Position(48.53363274338267, 28.74541620310008)),
            Map.entry("Вендичани", new Position(48.60674303659835, 27.78030336207645)),
            Map.entry("Веселий Кут", new Position(47.1063510885119, 29.99216380139991)),
            Map.entry("Затишшя", new Position(47.33675299906959, 29.878663299071842)),
            Map.entry("Камʼянка", new Position(46.80547802705657, 30.135080962975216)),
            Map.entry("Карпати", new Position(48.5229, 22.8786)),
            Map.entry("Кваси", new Position(48.1567, 24.2797)),
            Map.entry("Кодима", new Position(48.0912, 29.1214)),
            Map.entry("Кострино", new Position(48.945, 22.6008)),
            Map.entry("Крижопіль", new Position(48.3797, 28.8642)),
            Map.entry("Лисогірка", UNKNOWN),
            Map.entry("Мигаєве", new Position(46.9869, 30.0484)),
            Map.entry("Немерче", new Position(48.6823, 27.7582)),
            Map.entry("Нижня Апша", new Position(47.987, 23.8172)),
            Map.entry("Овруч", new Position(51.3248, 28.818)),
            Map.entry("Олександрівка", UNKNOWN),
            Map.entry("Перехрестове", new Position( 47.43868849268468, 29.79555377237737)),
            Map.entry("Петрівка", UNKNOWN),
            Map.entry("Пиріжна", UNKNOWN),
            Map.entry("Побережжя", UNKNOWN),
            Map.entry("Подільськ", UNKNOWN),
            Map.entry("Рахів", UNKNOWN),
            Map.entry("Роздільна", UNKNOWN),
            Map.entry("Слобідка", UNKNOWN),
            Map.entry("Сокиряни", UNKNOWN),
            Map.entry("Солотвино", UNKNOWN),
            Map.entry("Щербин", UNKNOWN),
            Map.entry("Ясіня", UNKNOWN));

    private static final Path TEXT_DIR = Path.of("../noescape-text/text");
    private static final Pattern ENTITY_PATTERN = Pattern.compile("^(\\d+):(\\d+)\\s+([A-Z_]+)\\s+(.+)$");
    private static final String ENTITIES_SEPARATOR = "\n=== ENTITIES ===\n";

    private static final AdministrativeAreaRepository administrativeAreaRepository;
    private static final HtmlRepository htmlRepository;

    static {
        try {
            administrativeAreaRepository = new AdministrativeAreaRepository();
            htmlRepository = new HtmlRepository();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        var courtCases = new TreeMap<String, CourtCase>();
        DataRepository.getCourtCases().forEach(courtCase -> courtCases.put(courtCase.caseId(), courtCase));
        try (var fileStream = Files.walk(TEXT_DIR)) {
            fileStream
                    .parallel()
                    .filter(Files::isRegularFile)
                    .filter(file -> !courtCases.containsKey(file.getFileName().toString().replace(".txt", "")))
                    .map(SpacyExtractor::parseFile)
                    .filter(Objects::nonNull)
                    .map(SpacyExtractor::extractData)
                    .forEach(courtCase -> courtCases.put(courtCase.caseId(), courtCase));

            DataRepository.saveCourtCases(new ArrayList<>(courtCases.values()));
        }
    }

    private static String readFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static SpacyDocument parseFile(Path file) {
        var id = file.getFileName().toString().replace(".txt", "");
//        if (!id.equals("123234037")) return null;
        var content = readFile(file);
        var parts = content.split(ENTITIES_SEPARATOR);
        if (parts.length != 2) {
            if (!content.contains(ENTITIES_SEPARATOR)) {
                System.err.println(file + " is not annotated");
            }
            return null;
        }

        var lines = parts[0].lines().toList();
        var offsets = new int[lines.size()];
        var offset = 0;
        int i = 0;
        for (var line : lines) {
            offsets[i++] = offset;
            offset += line.length() + 1;
        }

        var entities = parts[1].lines()
                .map(ENTITY_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> {
                    var lineNum = Integer.parseInt(matcher.group(1));
                    var colNum = Integer.parseInt(matcher.group(2));
                    var label = matcher.group(3);
                    var entText = matcher.group(4);
                    var start = offsets[lineNum - 1] + colNum - 1;
                    var end = start + entText.length();
                    if (!content.substring(start, end).equals(entText)) {
                        throw new IllegalStateException(label + ":" + entText);
                    }
                    return new Entity(label, entText, lineNum, colNum, start, end);
                })
                .toList();

        return new SpacyDocument(id, parts[0], entities);
    }

    private static CourtCase extractData(SpacyDocument document) {
        var caseId = document.id();
        var caseNumber = getCaseNumber(document.introduction());
        var appeal = isAppeal(document);
        var country = getCountry(document.protocolLine());
        if (country == null) {
            country = getCountry(document.motivation());
        }
        var borderSign = getBorderSign(document);
        var arrestDate = getArrestDate(document.protocolLine());
        var protocolDate = getProtocolDate(document.protocolLine());
        if (arrestDate == null) {
            arrestDate = getArrestDate(document.motivation());
        }
        if (arrestDate != null && protocolDate != null) {
            arrestDate = arrestDate.isAfter(protocolDate) ? protocolDate : arrestDate;
        }
        if (arrestDate == null) {
            arrestDate = protocolDate;
        }
        if (arrestDate == null) {
            arrestDate = getReceiveDate(document.protocolLine());
        }
        if (arrestDate == null) {
            protocolDate = getProtocolDate(document.entities());
            var hearingDate = getHearingDate(document.introduction());
            if (protocolDate != null && hearingDate != null && protocolDate.isBefore(hearingDate) && protocolDate.isAfter(hearingDate.minusMonths(1)) ) {
                arrestDate = protocolDate;
            }
        }
        var arrestTime = getArrestTime(document);
        if (arrestTime == null) {
            arrestTime = getDetentionTime(document);
        }

        var protocolArrestOblasts = getArrestOblasts(document.protocolLine());

        var courtOblasts = getCourtOblasts(document.introduction());
        if (courtOblasts.isEmpty()) {
            courtOblasts = getCourtOblasts(document.resolution());
        }
        if (courtOblasts.isEmpty()) {
            courtOblasts = protocolArrestOblasts;
        }
        var uniqueCourtOblasts = new LinkedHashSet<>(courtOblasts);
        var courtRayons = getCourtRayons(document.introduction(), uniqueCourtOblasts);
        if (courtRayons.isEmpty()) {
            courtRayons = getCourtRayons(document.resolution(), uniqueCourtOblasts);
        }
        var courtRayon = firstMostFrequent(courtRayons);
        var courtOblast = courtRayon == null ? firstMostFrequent(courtOblasts) : courtRayon.oblast();

        var documentArrestOblasts = getArrestOblasts(document.motivation());
        var uniqueProtocolArrestOblasts = new LinkedHashSet<>(protocolArrestOblasts);
        var uniqueDocumentArrestOblasts = new LinkedHashSet<>(documentArrestOblasts);

        Gromada arrGromada = null;

        /*
         * Protocol line may mention two places - one where the person was arrested, another one is where the person has illegally crossed the border earlier.
         * Gromada from the court rayon most likely is the arrest gromada.
         */
        var protocolGromadas = getArrestGromadas(document.protocolLine(), uniqueCourtOblasts);
        if (protocolGromadas.size() == 1) {
            arrGromada = protocolGromadas.getFirst();
        }

        if (arrGromada == null) {
            protocolGromadas = getArrestGromadas(document.protocolLine(), uniqueProtocolArrestOblasts);
            if (protocolGromadas.size() == 1) {
                arrGromada = protocolGromadas.getFirst();
            }
        }

        if (arrGromada == null) {
            protocolGromadas = getArrestGromadas(document.protocolLine(), uniqueDocumentArrestOblasts);
            if (protocolGromadas.size() == 1) {
                arrGromada = protocolGromadas.getFirst();
            }
        }

        if (arrGromada == null) {
            var documentGromadas = getArrestGromadas(document.motivation(), uniqueCourtOblasts);
            Gromada mostFrequentGromada = firstMostFrequent(documentGromadas);
            if (mostFrequentGromada != null) {
                arrGromada = documentGromadas.getFirst();
            }
        }

        if (arrGromada == null) {
            var documentGromadas = getArrestGromadas(document.motivation(), uniqueProtocolArrestOblasts);
            Gromada mostFrequentGromada = firstMostFrequent(documentGromadas);
            if (mostFrequentGromada != null) {
                arrGromada = documentGromadas.getFirst();
            }
        }

        if (arrGromada == null) {
            var documentGromadas = getArrestGromadas(document.motivation(), uniqueDocumentArrestOblasts);
            Gromada mostFrequentGromada = firstMostFrequent(documentGromadas);
            if (mostFrequentGromada != null) {
                arrGromada = documentGromadas.getFirst();
            }
        }

        Rayon arrRayon = null;

        var protocolRayons = getArrestRayons(document.protocolLine(), uniqueCourtOblasts);
        if (protocolRayons.size() == 1) {
            arrRayon = protocolRayons.getFirst();
        }

        if (arrRayon == null) {
            protocolRayons = getArrestRayons(document.protocolLine(), uniqueProtocolArrestOblasts);
            if (protocolRayons.size() == 1) {
                arrRayon = protocolRayons.getFirst();
            }
        }

        if (arrRayon == null) {
            protocolRayons = getArrestRayons(document.protocolLine(), uniqueDocumentArrestOblasts);
            if (protocolRayons.size() == 1) {
                arrRayon = protocolRayons.getFirst();
            }
        }

        if (arrRayon == null) {
            var documentRayons = getArrestRayons(document.motivation(), uniqueCourtOblasts);
            arrRayon = firstMostFrequent(documentRayons);
        }

        if (arrRayon == null) {
            var documentRayons = getArrestRayons(document.motivation(), uniqueProtocolArrestOblasts);
            arrRayon = firstMostFrequent(documentRayons);
        }

        if (arrRayon == null) {
            var documentRayons = getArrestRayons(document.motivation(), uniqueDocumentArrestOblasts);
            arrRayon = firstMostFrequent(documentRayons);
        }


        Settlement arrSettlement = null;

        var protocolSettlements = getArrestSettlements(document.protocolLine(), uniqueCourtOblasts);
        if (protocolSettlements.size() == 1) {
            arrSettlement = protocolSettlements.getFirst();
        }

        if (arrSettlement == null) {
            protocolSettlements = getArrestSettlements(document.protocolLine(), uniqueProtocolArrestOblasts);
            if (protocolSettlements.size() == 1) {
                arrSettlement = protocolSettlements.getFirst();
            }
        }

        if (arrSettlement == null) {
            protocolSettlements = getArrestFromSettlements(document.protocolLine(), uniqueCourtOblasts);
            if (protocolSettlements.size() == 1) {
                arrSettlement = protocolSettlements.getFirst();
            }
        }

        if (arrSettlement == null) {
            protocolSettlements = getArrestFromSettlements(document.protocolLine(), uniqueProtocolArrestOblasts);
            if (protocolSettlements.size() == 1) {
                arrSettlement = protocolSettlements.getFirst();
            }
        }

        if (arrSettlement == null) {
            protocolSettlements = getArrestToSettlements(document.protocolLine(), uniqueCourtOblasts);
            if (protocolSettlements.size() == 1) {
                arrSettlement = protocolSettlements.getFirst();
            }
        }

        if (arrSettlement == null) {
            protocolSettlements = getArrestToSettlements(document.protocolLine(), uniqueProtocolArrestOblasts);
            if (protocolSettlements.size() == 1) {
                arrSettlement = protocolSettlements.getFirst();
            }
        }

        Position position = null;
        if (arrSettlement == null) {
            var arrPlaces = getEntityValues(document.protocolLine(), "ARR_PLACE", s -> s);
            outer:
            for (String arrPlace : arrPlaces) {
                if (arrPlace.contains("залізничн") && arrPlace.contains("станці")) {
                    for (var entry : STATIONS.entrySet()) {
                        String station = entry.getKey();
                        if (arrPlace.contains(station)) {
                            arrSettlement = administrativeAreaRepository.normalizeSettlement(station, uniqueProtocolArrestOblasts);
                            if (arrSettlement == null) {
                                arrSettlement = administrativeAreaRepository.normalizeSettlement(station, uniqueCourtOblasts);
                            }
                            if (!entry.getValue().equals(UNKNOWN)) {
                                position = entry.getValue();
                            }
                            if (arrSettlement != null) {
                                break outer;
                            }
                        }
                    }
                }
            }
        }

        if (arrSettlement == null) {
            var documentSettlements = getArrestSettlements(document.motivation(), uniqueCourtOblasts);
            arrSettlement = firstMostFrequent(documentSettlements);
        }

        if (arrSettlement == null) {
            var documentSettlements = getArrestSettlements(document.motivation(), uniqueProtocolArrestOblasts);
            arrSettlement = firstMostFrequent(documentSettlements);
        }

        if (arrSettlement == null) {
            var documentSettlements = getArrestSettlements(document.motivation(), uniqueDocumentArrestOblasts);
            arrSettlement = firstMostFrequent(documentSettlements);
        }

        String arrOblast;
        if (arrRayon != null) {
            arrOblast = arrRayon.oblast();
        } else if (protocolArrestOblasts.size() == 1) {
            arrOblast = protocolArrestOblasts.getFirst();
        } else if (courtRayon != null) {
            arrOblast = courtRayon.oblast();
        } else if (courtOblast != null) {
            arrOblast = courtOblast;
        } else {
            arrOblast = firstMostFrequent(documentArrestOblasts);
        }

        var fine = getFine(document);
        var distance = getDistance(document);
        var guardType = getGuardType(document);
        var group = getGroup(document.lowerText());
        if (arrestDate == null) {
            System.out.println(caseId);
        }
        return new CourtCase(
                caseId,
                caseNumber,
                appeal,
                country,
                borderSign,
                arrestDate,
                arrestTime,
                getRegistrationDate(caseId),
                getPublicationDate(caseId),
                courtRayon == null ? null : courtRayon.rayon(),
                new Locality(arrSettlement == null ? null : arrSettlement.settlement(), arrGromada == null ? null : arrGromada.gromada(), arrRayon == null ? null : arrRayon.rayon(), arrOblast),
                fine,
                distance,
                guardType,
                group,
                position);
    }

    private static String getCaseNumber(List<Entity> entities) {
        var caseNumbers = getEntityValues(entities, "CASE_NUMBER", SpacyExtractor::caseNumber);
        return caseNumbers.isEmpty() ? null : caseNumbers.getFirst();
    }

    private static boolean isAppeal(SpacyDocument document) {
        var lowerCaseBody = document.lowerText();
        if (lowerCaseBody.contains("апеляційний суд у складі")) {
            return true;
        }
        if (lowerCaseBody.contains("апеляційний суд в складі")) {
            return true;
        }
        if (lowerCaseBody.contains("апеляційний суд в особі судді")) {
            return true;
        }
        if (lowerCaseBody.contains("суддя судової палати")) {
            return true;
        }
        if (lowerCaseBody.lines().anyMatch(line -> line.endsWith(" апеляційний суд"))) {
            return true;
        }
        if (Pattern.compile("суддя [а-яґєіїʼ-]+ апеляційного суду").matcher(lowerCaseBody).find()) {
            return true;
        }
        return false;
    }

    private static String getCountry(List<Entity> entities) {
        var countryEntities = new ArrayList<Entity>();
        entities.stream()
                .filter(havingLabel("BORDER_CNTRY"))
                .forEach(countryEntities::add);
        entities.stream()
                .filter(havingLabel("SETTL_CNTRY"))
                .forEach(countryEntities::add);
        var countries = countryEntities.stream()
                .map(Entity::text)
                .map(SpacyExtractor::countryCode)
                .filter(Objects::nonNull) // Skip unrecognized
                .toList();
        return firstMostFrequent(countries);
    }

    private static String countryCode(String text) {
        text = text.toLowerCase();
        text = text.replaceAll("^респуб[а-яіїґє]+", "");
        text = text.replaceAll("р\\.", "");
        text = text.trim();
        if (text.startsWith("слова")) {
            return "SK";
        }
        if (text.startsWith("угор")) {
            return "HU";
        }
        if (text.startsWith("поль") || text.equals("рп")) {
            return "PL";
        }
        if (text.startsWith("румун")) {
            return "RO";
        }
        if (text.startsWith("білор") || text.equals("рб")) {
            return "BY";
        }
        if (text.startsWith("росі") || text.equals("рф")) {
            return "RU";
        }
        if (text.startsWith("молд") || text.equals("рм") || text.equals("пмр")) {
            return "MD";
        }
        return null;
    }

    private static String getBorderSign(SpacyDocument document) {
        // Protocol line has the highest precedence
        var borderSign = getBorderSign(document.protocolLine());
        if (borderSign != null) {
            return borderSign;
        }

        // To reduce chance of false positives, consider ZNAK only if there are other "protocol" entities on the same line
        Set<Integer> arrestLines = new HashSet<>();
        for (var label : SpacyDocument.PROTOCOL_LABELS) {
            if (!label.equals("ZNAK")) {
                document.motivation().stream()
                        .filter(havingLabel(label))
                        .map(Entity::line)
                        .forEach(arrestLines::add);
            }
        }
        var borderSigns = document.motivation().stream()
                .filter(havingLabel("ZNAK"))
                .filter(entity -> arrestLines.contains(entity.line()))
                .map(Entity::text)
                .map(SpacyExtractor::borderSign)
                .filter(Objects::nonNull) // Skip unrecognized
                .toList();
        return firstMostFrequent(borderSigns);
    }

    private static String getBorderSign(List<Entity> entities) {
        var borderSigns = getEntityValues(entities, "ZNAK", SpacyExtractor::borderSign);
        return firstMostFrequent(borderSigns);
    }

    private static String borderSign(String text) {
        for (var pattern : BORDER_SIGN_PATTERNS) {
            if (pattern.matcher(text).matches()) {
                return text;
            }
        }
        return null;
    }

    private static LocalDate getArrestDate(List<Entity> entities) {
        var arrestDates = getEntityValues(entities, "ARR_DATE", SpacyExtractor::date);
        return firstMostFrequent(arrestDates);
    }

    private static LocalDate getProtocolDate(List<Entity> entities) {
        var protoDates = getEntityValues(entities, "PROTO_DATE", SpacyExtractor::date);
        return firstMostFrequent(protoDates);
    }

    private static LocalDate getReceiveDate(List<Entity> entities) {
        var arrestDates = getEntityValues(entities, "RECEIVE_DATE", SpacyExtractor::date);
        return firstMostFrequent(arrestDates);
    }

    private static LocalDate getHearingDate(List<Entity> entities) {
        var hearingDates = getEntityValues(entities, "HEARING_DATE", SpacyExtractor::date);
        return firstMostFrequent(hearingDates);
    }

    private static LocalDate date(String text) {
        for (var dateFormat : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, dateFormat);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static LocalTime getArrestTime(SpacyDocument document) {
        // Protocol line has the highest precedence
        var arrestTime = getArrestTime(document.protocolLine());
        if (arrestTime != null) {
            return arrestTime;
        }
        // Check the whole document
        return getArrestTime(document.motivation());
    }

    private static LocalTime getArrestTime(List<Entity> entities) {
        var arrestTimes = getEntityValues(entities, "ARR_TIME", SpacyExtractor::arrestTime);
        return firstMostFrequent(arrestTimes);
    }

    private static LocalTime arrestTime(String text) {
        Matcher matcher = TIME_PATTERN.matcher(text);
        if (matcher.matches()) {
            try {
                return LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
            } catch (DateTimeException ignored) {
            }
        }
        matcher = Pattern.compile(HOUR_PATTERN).matcher(text);
        if (matcher.matches()) {
            int hour = Integer.parseInt(matcher.group(1));
            if (hour >= 0 && hour <= 24) {
                return LocalTime.of(hour, 0);
            }
        }
        return null;
    }

    private static LocalTime getDetentionTime(SpacyDocument document) {
        // Sometimes arrest time is recognized as detention time
        return getDetentionTime(document.protocolLine());
    }

    private static LocalTime getDetentionTime(List<Entity> entities) {
        var detentionTimes = getEntityValues(entities, "DET_TIME", SpacyExtractor::time);
        return firstMostFrequent(detentionTimes);
    }

    private static LocalTime time(String text) {
        Matcher matcher = TIME_PATTERN.matcher(text);
        if (matcher.matches()) {
            try {
                return LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
            } catch (DateTimeException ignored) {
            }
        }
        matcher = Pattern.compile(HOUR_PATTERN).matcher(text);
        if (matcher.matches()) {
            int hour = Integer.parseInt(matcher.group(1));
            if (hour >= 0 && hour <= 24) {
                return LocalTime.of(hour, 0);
            }
        }
        return null;
    }

    private static List<String> getArrestOblasts(List<Entity> entities) {
        return getEntityValues(entities, "ARR_OBLAST", administrativeAreaRepository::normalizeOblast);
    }

    private static List<Rayon> getArrestRayons(List<Entity> entities, Collection<String> oblasts) {
        return getEntityValues(entities, "ARR_RAYON", rayon -> administrativeAreaRepository.normalizeRayon(rayon, oblasts));
    }

    private static List<Gromada> getArrestGromadas(List<Entity> entities, Collection<String> oblasts) {
        return getEntityValues(entities, "ARR_GROMADA", gromada -> administrativeAreaRepository.normalizeGromada(gromada, oblasts));
    }

    private static List<Settlement> getArrestSettlements(List<Entity> entities, Collection<String> oblasts) {
        return getEntityValues(entities, "ARR_SETTL", settlement -> administrativeAreaRepository.normalizeSettlement(settlement, oblasts));
    }

    private static List<Settlement> getArrestFromSettlements(List<Entity> entities, Collection<String> oblasts) {
        return getEntityValues(entities, "SETTL_FROM", settlement -> administrativeAreaRepository.normalizeSettlement(settlement, oblasts));
    }

    private static List<Settlement> getArrestToSettlements(List<Entity> entities, Collection<String> oblasts) {
        return getEntityValues(entities, "SETTL_TO", settlement -> administrativeAreaRepository.normalizeSettlement(settlement, oblasts));
    }

    private static List<String> getCourtOblasts(List<Entity> entities) {
        return getEntityValues(entities, "COURT_OBLAST", administrativeAreaRepository::normalizeOblast);
    }

    private static List<Rayon> getCourtRayons(List<Entity> entities, Collection<String> courtOblasts) {
        return getEntityValues(entities, "COURT_RAYON", rayon -> administrativeAreaRepository.normalizeRayon(rayon, courtOblasts));
    }

    private static String getFine(SpacyDocument document) {
        var resolutions = getEntityValues(document.resolution(), "RESOLUTION", SpacyExtractor::resolution);
        var fine = getFine(document.resolution());
        if (fine == null && resolutions.isEmpty()) {
            resolutions = getEntityValues(document.motivation(), "RESOLUTION", SpacyExtractor::resolution);
        }
        if (fine != null && resolutions.contains("штраф")) {
            return fine;
        }
        if (resolutions.isEmpty()) {
            return fine;
        }
        var resolution = firstMostFrequent(resolutions);
        return "штраф".equals(resolution) ? null : resolution;
    }

    private static String getFine(List<Entity> entities) {
        var fines = entities.stream()
                .filter(havingLabel("FINE"))
                .map(Entity::text)
                .map(SpacyExtractor::getFine)
                .filter(Objects::nonNull) // Skip unrecognized
                .toList();
        return firstMostFrequent(fines);
    }

    private static String getFine(String text) {
        Matcher matcher = FINE_PATTERN.matcher(text);
        if (matcher.matches()) {
            String fine = matcher.group(1).replace(" ", "");
            long value = Long.parseLong(fine);
            if (value > 1000 && (value < 10000 && value % 5 == 0 || value < 50000 && value % 10 == 0)) {
                return String.valueOf(value);
            }
        }
        System.err.println("Bad FINE: " + text);
        return null;
    }

    private static Integer getDistance(SpacyDocument document) {
        // Protocol line has the highest precedence
        var distance = getDistance(document.protocolLine());
        if (distance != null) {
            return distance;
        }
        // Check the whole document
        return getDistance(document.motivation());
    }

    private static Integer getDistance(List<Entity> entities) {
        var distances = getEntityValues(entities, "DIST", SpacyExtractor::getDistance);
        if (distances.size() > 1) {
            // FIXME: workaround. big distance likely mean river or road milestone
            var distance = distances.stream().filter(dist -> dist < 30000).findFirst().orElse(null);
            if (distance != null) return distance;
        }
        return distances.stream().findFirst().orElse(null);
    }

    private static Integer getDistance(String text) {
        var matcher = DISTANCE_PATTERN.matcher(text.toLowerCase());
        if (matcher.matches()) {
            String value = matcher.group(1).replace(" ", "").replace(',', '.');
            String units = matcher.group(2).replace(" ", "");
            double dist = Double.parseDouble(value);
            if (units.contains("км") || units.contains("кілом")) {
                if (dist < 90) {
                    dist *= 1000;
                }
            }
            return (int) dist;
        }
        System.err.println("Bad DIST: " + text);
        return null;
    }

    private static String getGuardType(SpacyDocument document) {
        var guards = getEntityValues(document.motivation(), "GUARD", Function.identity());
        return firstMostFrequent(guards);
    }

    private static LocalDate getRegistrationDate(String caseId) {
        String html = htmlRepository.getHtml(caseId);
        Pattern registrationPattern = Pattern.compile("Надіслано для оприлюднення: <b>(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d)");
        Matcher matcher = registrationPattern.matcher(html);
        if (matcher.find()) {
            return LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        registrationPattern = Pattern.compile("Зареєстровано: <b>(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d)");
        matcher = registrationPattern.matcher(html);
        if (matcher.find()) {
            return LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        return null;
    }

    private static LocalDate getPublicationDate(String caseId) {
        String html = htmlRepository.getHtml(caseId);
        Pattern publicationPattern = Pattern.compile("Забезпечено надання загального доступу: <b>(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d)");
        Matcher matcher = publicationPattern.matcher(html);
        if (matcher.find()) {
            return LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        publicationPattern = Pattern.compile("Оприлюднено: <b>(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d)");
        matcher = publicationPattern.matcher(html);
        if (matcher.find()) {
            return LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        return null;
    }

    private static Boolean getGroup(String lowerCaseBody) {
        boolean group2 = lowerCaseBody.contains("складі групи") || lowerCaseBody.contains(" групою осіб") || lowerCaseBody.contains(" групі осіб") || lowerCaseBody.contains(" групі з ")
                || Pattern.compile("\\b(:?ч\\.|ч|частини|частиною|частина)\\s*(:?2|друга|другою|другої)\\s*(:?за\\s*)?(:?ст\\.|ст|статті|статтею|стаття)\\s*204-1\\b").matcher(lowerCaseBody).find()
                || Pattern.compile("\\b(:?2|друга|другою|другої)\\s*(:?ч\\.|ч|частини|частиною|частина)\\s*(:?ст\\.|ст|статті|статтею|стаття)\\s*204-1\\b").matcher(lowerCaseBody).find()
                || Pattern.compile("\\b(:?ст\\.|ст|статті|статтею|стаття)\\s*204-1\\s*(:?2|друга|другою|другої)\\s*(:?ч\\.|ч|частини|частиною|частина)\\b").matcher(lowerCaseBody).find()
                || Pattern.compile("\\b(:?ст\\.|ст|статті|статтею|стаття)\\s*204-1\\s*(:?ч\\.|ч|частини|частиною|частина)\\s*(:?2|друга|другою|другої)\\b").matcher(lowerCaseBody).find();
        boolean group1 = Pattern.compile("\\b(:?ч\\.|ч|частини|частиною|частина)\\s*(:?1|перша|першою|першої)\\s*(:?за\\s*)?(:?ст\\.|ст|статті|статтею|стаття)\\s*204-1\\b").matcher(lowerCaseBody).find()
                || Pattern.compile("\\b(:?1|перша|першою|першої)\\s*(:?ч\\.|ч|частини|частиною|частина)\\s*(:?ст\\.|ст|статті|статтею|стаття)\\s*204-1\\b").matcher(lowerCaseBody).find()
                || Pattern.compile("\\b(:?ст\\.|ст|статті|статтею|стаття)\\s*204-1\\s*(:?1|перша|першою|першої)\\s*(:?ч\\.|ч|частини|частиною|частина)\\b").matcher(lowerCaseBody).find()
                || Pattern.compile("\\b(:?ст\\.|ст|статті|статтею|стаття)\\s*204-1\\s*(:?ч\\.|ч|частини|частиною|частина)\\s*(:?1|перша|першою|першої)\\b").matcher(lowerCaseBody).find();
        return group1 ^ group2 ? group2 : null;
    }

    private static String caseNumber(String text) {
        Matcher matcher = CASE_NUMBER_PATTERN.matcher(text);
        return matcher.matches() ? text : null;
    }

    private static <T> List<T> getEntityValues(Collection<Entity> entities, String label, Function<String, T> mapper) {
        return entities.stream()
                .filter(havingLabel(label))
                .map(Entity::text)
                .filter(value -> !value.startsWith("ІНФОРМАЦІЯ"))
                .map(mapper)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String resolution(String value) {
        value = value.toLowerCase();
        if (value.contains("арешт")) return "арешт";
        if (value.contains("повернути")) return "-1";
        if (value.contains("закрити")) return "0";
        if (value.contains("штраф")) return "штраф";
        return null;
    }
}
