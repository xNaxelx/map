package noescape.util;

import noescape.model.Position;
import org.junit.jupiter.api.Test;

import static noescape.util.GeospatialCalculation.calculateAzimuth;
import static noescape.util.GeospatialCalculation.computePerpendicularAtEnd;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GeospatialCalculationTest {

    private static final int ONE_DEGREE_IN_METERS_AT_EQUATOR = 111190;

    @Test
    void testCalcPerpendicularPositionAtLosAngeles() {
        Position A = new Position(34.052235, -118.243683);
        Position B = new Position(34.052000, -118.250000);
        double distanceBC = 1000.0;
        Position C = computePerpendicularAtEnd(A, B, distanceBC);
        assertEquals(34.060984, C.lat(), 0.000001);
        assertEquals(-118.250487, C.lng(), 0.000001);
    }

    @Test
    void testCalcPerpendicularPositionAtEquator() {
        Position A = new Position(0, 1);
        Position B = new Position(0, 0);
        Position C = computePerpendicularAtEnd(A, B, ONE_DEGREE_IN_METERS_AT_EQUATOR);
        assertEquals(1, C.lat(), 0.0001);
        assertEquals(0, C.lng(), 0.0001);
    }

    @Test
    void testCalcPerpendicularPosition() {
        Position A = new Position(2, 6);
        Position B = new Position(5, 2);
        Position C = computePerpendicularAtEnd(A, B, 4 * ONE_DEGREE_IN_METERS_AT_EQUATOR);
        assertEquals(8.2, C.lat(), 0.1);
        assertEquals(4.4, C.lng(), 0.1);
    }


    @Test
    void testCalculateAzimuth() {
        Position A = new Position(0, 0);
        Position B = new Position(0, 10);
        Position C = new Position(10, 10);

        // North
        assertEquals(0, calculateAzimuth(B, C), 0.0001);
        // South
        assertEquals(Math.PI, calculateAzimuth(C, B), 0.0001);
        // East
        assertEquals(Math.PI / 2, calculateAzimuth(A, B), 0.0001);
        // West
        assertEquals(-Math.PI / 2, calculateAzimuth(B, A), 0.0001);

        // The North Pole is a special case
        Position N = new Position(90, 0);
        assertEquals(Math.PI, calculateAzimuth(N, new Position(0, 0)), 0.0001);
        assertEquals(0, calculateAzimuth(N, new Position(0, 180)), 0.0001);
    }
}