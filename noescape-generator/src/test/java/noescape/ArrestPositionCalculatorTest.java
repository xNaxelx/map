package noescape;

import noescape.model.BorderSign;
import noescape.model.Position;
import noescape.repository.BorderSignRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;

class ArrestPositionCalculatorTest {
    private static final int ONE_DEGREE_IN_METERS_AT_EQUATOR = 111190;


    private static ArrestPositionCalculator calculator;
    private static BorderSignRepository borderSignRepository;

    @Test
    void testThreeBorderSigns() {
        setBorderSigns(
                sign("4", 5, 2),
                sign("5", 2, 6),
                sign("6", 4, 10));
        var targetSign = getSign("5");
        int distance = 4 * ONE_DEGREE_IN_METERS_AT_EQUATOR;
        var arrestPosition = calculator.computeSector(distance, targetSign, null);
        assertPositionEquals(new Position(6.7, 6.4), arrestPosition);
    }

    private static void setBorderSigns(BorderSign... borderSigns) {
        borderSignRepository = new BorderSignRepository(asList(borderSigns));
        calculator = new ArrestPositionCalculator(borderSignRepository);
    }

    private static BorderSign getSign(String number) {
        return borderSignRepository.getBorderSign("RO", number);
    }

    private static BorderSign sign(String number, double lat, double lng) {
        return new BorderSign("RO", number, new Position(lat, lng), false);
    }

    private static void assertPositionEquals(Position expected, Position actual) {
        Assertions.assertEquals(expected.lat(), actual.lat(), 0.001);
        Assertions.assertEquals(expected.lng(), actual.lng(), 0.001);
    }
}