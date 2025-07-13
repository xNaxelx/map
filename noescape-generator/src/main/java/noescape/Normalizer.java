package noescape;

import noescape.model.BorderSign;
import noescape.model.CourtCase;
import noescape.model.Locality;
import noescape.model.Position;
import noescape.repository.AdministrativeAreaRepository;
import noescape.repository.BorderSignRepository;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static java.util.Comparator.comparingDouble;
import static noescape.util.CoordUtils.calculateDistance;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public class Normalizer {

    private final BorderSignRepository borderSignRepository = new BorderSignRepository();
    private final AdministrativeAreaRepository administrativeAreaRepository = new AdministrativeAreaRepository();
    private final GoogleClient googleClient = new GoogleClient();

    public Normalizer() throws IOException {
    }

    public CourtCase normalize(CourtCase courtCase) {
        String borderSign = substringBefore(courtCase.borderSign(), '-');
        String country = courtCase.country();
        String court = courtCase.court();
        String settlement = courtCase.locality().settlement();
        String gromada = courtCase.locality().gromada();
        String rayon = courtCase.locality().rayon();
        String oblast = courtCase.locality().oblast();
        if ("Франківська".equals(oblast)) {
            oblast = "Івано-Франківська";
        }
        if ("Дністровський".equals(rayon) && "Одеська".equals(oblast)) {
            rayon = "Білгород-Дністровський";
        }
        if ("Дністровський".equals(court) && "Одеська".equals(oblast)) {
            court = "Білгород-Дністровський";
        }

        if (gromada != null && rayon == null) {
            rayon = administrativeAreaRepository.getRayonByGromada(gromada);
        }

        if (gromada != null && rayon == null && oblast != null) {
            rayon = administrativeAreaRepository.getRayonByGromadaAndOblast(gromada, oblast);
        }

        if (gromada != null && rayon != null && oblast == null) {
            oblast = administrativeAreaRepository.getOblastByGromadaAndRayon(gromada, rayon);
        }

        if (settlement != null && gromada == null) {
            if (rayon != null) {
                gromada = administrativeAreaRepository.getGromadaByCenterAndRayon(settlement, rayon);
            }
            if (gromada == null && administrativeAreaRepository.rayonExists(court, oblast)) {
                gromada = administrativeAreaRepository.getGromadaByCenterAndRayon(settlement, court);
            }
        }

        if (rayon == null && settlement != null && oblast != null) {
            rayon = administrativeAreaRepository.getRayonByCenterAndOblast(settlement, oblast);
        }

        if (rayon == null && court != null) {
            if (oblast != null) {
                if (administrativeAreaRepository.rayonExists(court, oblast)) {
                    rayon = court;
                }
            } else {
                if (administrativeAreaRepository.rayonUnique(court)) {
                    rayon = court;
                }
            }
        }

        if (oblast == null && rayon != null) {
            oblast = administrativeAreaRepository.getOblastByRayon(rayon);
        }

        String[] gromadaCountries = administrativeAreaRepository.getCountryByGromadaAndOblast(gromada, oblast);

        if (gromadaCountries.length == 1) {
            country = gromadaCountries[0];
        }

        if (country == null) {
            var target = settlement == null ? administrativeAreaRepository.getGromadaCenter(gromada, oblast) : settlement;
            if (target == null) {
                target = administrativeAreaRepository.getRayonCenter(rayon, oblast);
            }
            if (target != null) {
                var geocodeResult = googleClient.geocode(new Locality(target, gromada, rayon, oblast));
                if (geocodeResult.isPresent()) {
                    var localityPosition = geocodeResult.get().position();
                    List<BorderSign> borderSigns;
                    if (borderSign == null || gromadaCountries.length == 0) {
                        borderSigns = borderSignRepository.getBorderSigns();
                    } else {
                        var sign = borderSign;
                        borderSigns = Stream.of(gromadaCountries)
                                .map(ctry -> borderSignRepository.getBorderSign(ctry, sign))
                                .filter(Objects::nonNull)
                                .toList();
                    }
                    var closestBorderSign = getClosestBorderSign(localityPosition, borderSigns);
                    if (closestBorderSign != null && calculateDistance(closestBorderSign.position(), localityPosition) < 100000) {
                        country = closestBorderSign.country();
                    }
                }
            }
        }

        if (country == null) {
            country = administrativeAreaRepository.getCountryByGromada(gromada);
        }
        if (country == null) {
            country = getCountryByRayon(rayon);
        }
        if ("Луганська".equals(oblast)) {
            country = "RU";
        }
        if ("Донецька".equals(oblast)) {
            country = "RU";
        }
        if ("Сумська".equals(oblast)) {
            country = "RU";
        }
        if ("Вінницька".equals(oblast)) {
            country = "MD";
        }
        if ("Одеська".equals(oblast) && borderSign != null) {
            country = parseInt(substringBefore(borderSign, '/')) >= 790 ? "RO" : "MD";
        }
        if ("MD".equals(country) && borderSign != null && parseInt(substringBefore(borderSign, '/')) >= 790) {
            country = "RO";
        }

        if (contains(country, ',')) country = null;

        if (borderSign != null) {
            String borderSignCountry = country;
            // BY signs at Chernigiv oblast are numbered differently from the rest of BY border
            if ("Чернігівська".equals(oblast) && "BY".equals(country)) {
                borderSignCountry = "BY2";
            }
            borderSign = BorderSign.Key.formatTitle(borderSignCountry, borderSign);
        }

        courtCase = normalizeArrestDate(courtCase);
        Locality locality = new Locality(settlement, gromada, rayon, oblast);
        return courtCase.withCountry(country).withBorderSign(borderSign).withCourt(court).withLocality(locality);
    }

    private static CourtCase normalizeArrestDate(CourtCase courtCase) {
        if (courtCase.arrestDate() != null) {
            var registrationDate = courtCase.registrationDate();
            if (registrationDate == null) {
                registrationDate = courtCase.publicationDate();
            }
            if (courtCase.arrestDate().isAfter(registrationDate)) {
                if (courtCase.arrestDate().getYear() > registrationDate.getYear()) {
                    courtCase = courtCase.withArrestDate(courtCase.arrestDate().withYear(registrationDate.getYear()));
                } else if (courtCase.arrestDate().getMonthValue() > registrationDate.getMonthValue()) {
                    courtCase = courtCase.withArrestDate(courtCase.arrestDate().withMonth(registrationDate.getMonthValue()));
                    if (courtCase.arrestDate().isAfter(registrationDate)) {
                        courtCase = courtCase.withArrestDate(courtCase.arrestDate().minusMonths(1));
                    }
                }
            }
        }
        return courtCase;
    }

    private static BorderSign getClosestBorderSign(Position position, List<BorderSign> borderSigns) {
        return borderSigns.stream().min(comparingDouble(s -> calculateDistance(s.position(), position))).orElse(null);
    }

    public static String getCountryByRayon(String rayon) {
        if (rayon == null) return null;
        switch (rayon) {
            case "Берегометський":
                return "RO";
            case "Боринський":
                return "PL";
            case "Бородінський":
                return "MD";
            case "Вараський":
                return "BY";
            case "Великомихайлівський":
                return "MD";
            case "Верховинський":
                return "RO";
            case "Вижницький":
                return "RO";
            case "Герцаївський":
                return "RO";
            case "Глибоцький":
                return "RO";
            case "Жовківський":
                return "PL";
            case "Заставнівський":
                return "RO";
            case "Зарічненський":
                return "BY";
            case "Фрунзівський": // former захарівської
                return "MD";
            case "Кельменецький":
                return "MD";
            case "Кодимський":
                return "MD";
            case "Кілійський":
                return "RO";
            case "Кіцманський":
                return "RO";
            case "Лівинецький":
                return "MD";
            case "Любомильський": // любомльського
            case "Любомльський":
                return "PL";
            case "Мамалигівський":
                return "MD";
            case "Надвірнянський":
                return "RO";
            case "Новодністровський":
                return "MD";
            case "Красноокнянський": // former окнянського
            case "Окнянський":
                return "MD";
            case "Олевський":
                return "BY";
            case "Перечинський":
                return "SK";
            case "Піщанський":
                return "MD";
            case "Рахівський":
                return "RO";
            case "Роздільнянський":
                return "MD";
            case "Саратський":
                return "MD";
            case "Сокальський":
                return "PL";
            case "Сокирянський":
                return "MD";
            case "Солотвинська":
                return "RO";
            case "Старосамбірський":
                return "PL";
            case "Чернігівський":
                return "BY";
            case "Сторожинецький":
                return "RO";
            case "Яворівський":
                return "PL";
            default:
                return null;
        }
    }
}
