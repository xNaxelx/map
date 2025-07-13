package noescape.data;

import noescape.model.CourtCase;
import noescape.model.Position;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record Arrest(LocalDate arrestDate, LocalTime arrestTime, Position arrestPosition, List<CourtCase> cases) {
    public Arrest withPosition(Position arrestPosition) {
        return new Arrest(arrestDate, arrestTime, arrestPosition, cases);
    }
}
