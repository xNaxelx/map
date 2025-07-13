package noescape.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

public record CourtCase(
        String caseId,
        String caseNumber,
        boolean appeal,
        String country,
        String borderSign,
        LocalDate arrestDate,
        LocalTime arrestTime,
        LocalDate registrationDate,
        LocalDate publicationDate,
        String court,
        Locality locality,
        String fine,
        Integer distance,
        String guard,
        Boolean group,
        Position position) {

    public CourtCase {
        if (country != null && !BorderSign.Key.COUNTRIES.contains(country)) {
            throw new IllegalArgumentException(country);
        }
        if (caseNumber != null && !Pattern.compile("\\d+/\\d+/\\d+").matcher(caseNumber).matches()) {
            throw new IllegalArgumentException(caseNumber);
        }
    }

    public CourtCase withCaseNumber(String caseNumber) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withCountry(String country) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withBorderSign(String borderSign) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withArrestDate(LocalDate arrestDate) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withArrestTime(LocalTime arrestTime) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withRegistrationDate(LocalDate registrationDate) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withPublicationDate(LocalDate publicationDate) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withCourt(String court) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withLocality(Locality locality) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withFine(String fine) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withDistance(Integer distance) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withGroup(Boolean group) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }

    public CourtCase withPosition(Position position) {
        return new CourtCase(caseId, caseNumber, appeal, country, borderSign, arrestDate, arrestTime, registrationDate, publicationDate, court, locality, fine, distance, guard, group, position);
    }
}
