package noescape.util;

import noescape.model.Position;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;

/**
 * Convert the desired distance from meters to degrees (approximation):
 * 1 degree of latitude is approximately 111,320 meters.
 * 1 degree of longitude varies with latitude, but for simplicity, we can use the average value around the latitude of Ukraine which is approximately 71,700 meters at 48°N.
 */
public class CoordUtils {

    // Earth's radius in meters
    public static final double EARTH_RADIUS = 6371000;
    public static final int LEFT = 1;
    public static final int RIGHT = -1;
    public static final int STRAIT = 0;

    public static double calculateDistance(Position p1, Position p2) {
        double lat1Rad = Math.toRadians(p1.lat());
        double lng1Rad = Math.toRadians(p1.lng());
        double lat2Rad = Math.toRadians(p2.lat());
        double lng2Rad = Math.toRadians(p2.lng());

        // Calculate the differences between the coordinates
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lng2Rad - lng1Rad;

        // Apply the Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Calculate the distance
        return EARTH_RADIUS * c;
    }

    public static Position calcPositionAtDistance(Position from, Position to, double distance) {
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(from.lat());
        double lng1Rad = Math.toRadians(from.lng());
        double lat2Rad = Math.toRadians(to.lat());
        double lng2Rad = Math.toRadians(to.lng());

        // Calculate the initial bearing
        double deltaLng = lng2Rad - lng1Rad;
        double y = Math.sin(deltaLng) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLng);
        double initialBearing = Math.atan2(y, x);

        // Convert distance to radians (distance divided by Earth's radius, in meters)
        double distanceRad = distance / EARTH_RADIUS;

        // Calculate the new coordinates
        double latNewRad = Math.asin(Math.sin(lat1Rad) * Math.cos(distanceRad) +
                Math.cos(lat1Rad) * Math.sin(distanceRad) * Math.cos(initialBearing));
        double lngNewRad = lng1Rad + Math.atan2(Math.sin(initialBearing) * Math.sin(distanceRad) * Math.cos(lat1Rad),
                Math.cos(distanceRad) - Math.sin(lat1Rad) * Math.sin(latNewRad));

        // Convert new coordinates from radians to degrees
        double latNew = Math.toDegrees(latNewRad);
        double lngNew = Math.toDegrees(lngNewRad);
        return new Position(latNew, lngNew);
    }

    public static Position getIntersection(Position a1, Position a2, Position b1, Position b2) {
        double denominator = (a1.lat() - a2.lat()) * (b1.lng() - b2.lng()) - (a1.lng() - a2.lng()) * (b1.lat() - b2.lat());

        if (denominator == 0) {
            // The lines are parallel or coincident
            return null;
        }

        double numerator1 = (a1.lat() * a2.lng() - a1.lng() * a2.lat());
        double numerator2 = (b1.lat() * b2.lng() - b1.lng() * b2.lat());

        double x = (numerator1 * (b1.lat() - b2.lat()) - (a1.lat() - a2.lat()) * numerator2) / denominator;
        double y = (numerator1 * (b1.lng() - b2.lng()) - (a1.lng() - a2.lng()) * numerator2) / denominator;

        return new Position(x, y);
    }


    public static Position findGeoCircleCenter(Position A, Position B, Position C) {
        if (A.equals(B) || A.equals(C) || B.equals(C)) {
            throw new IllegalArgumentException("Positions must be different: " + A + "," + "B" + "," + C);
        }
        // Convert lat/lon to Cartesian coordinates
        double[] cartesianA = latLonToCartesian(A);
        double[] cartesianB = latLonToCartesian(B);
        double[] cartesianC = latLonToCartesian(C);

        // Calculate midpoints
        double[] midAB = midpoint(cartesianA, cartesianB);
        double[] midBC = midpoint(cartesianB, cartesianC);

        // Calculate normal vectors
        double[] normalAB = crossProduct(cartesianA, cartesianB);
        double[] normalBC = crossProduct(cartesianB, cartesianC);

        // Calculate the perpendicular bisectors
        double[] bisectorAB = crossProduct(midAB, normalAB);
        double[] bisectorBC = crossProduct(midBC, normalBC);

        // Calculate the center by solving the linear system of the two planes
        double[] centerCartesian = crossProduct(bisectorAB, bisectorBC);

        // Normalize the center
        double norm = Math.sqrt(centerCartesian[0] * centerCartesian[0] +
                centerCartesian[1] * centerCartesian[1] +
                centerCartesian[2] * centerCartesian[2]);
        centerCartesian[0] /= norm;
        centerCartesian[1] /= norm;
        centerCartesian[2] /= norm;

        // Convert back to lat/lon
        return cartesianToLatLon(centerCartesian);
    }

    public static double[] midpoint(double[] A, double[] B) {
        return new double[]{
                (A[0] + B[0]) / 2,
                (A[1] + B[1]) / 2,
                (A[2] + B[2]) / 2
        };
    }

    public static Position findCircleCenter(Position p1, Position p2, Position p3) {
        double x1 = p1.lat();
        double y1 = p1.lng();
        double x2 = p2.lat();
        double y2 = p2.lng();
        double x3 = p3.lat();
        double y3 = p3.lng();

        double x1Sq = x1 * x1, y1Sq = y1 * y1;
        double x2Sq = x2 * x2, y2Sq = y2 * y2;
        double x3Sq = x3 * x3, y3Sq = y3 * y3;

        double a = x1 * (y2 - y3) - y1 * (x2 - x3) + x2 * y3 - x3 * y2;
        double b = (x1Sq + y1Sq) * (y3 - y2) + (x2Sq + y2Sq) * (y1 - y3) + (x3Sq + y3Sq) * (y2 - y1);
        double c = (x1Sq + y1Sq) * (x2 - x3) + (x2Sq + y2Sq) * (x3 - x1) + (x3Sq + y3Sq) * (x1 - x2);

        double x = -b / (2 * a);
        double y = -c / (2 * a);

        return new Position(x, y);
    }

    public static double calcAngle(Position A, Position B, Position C) {
        // Convert the points to Cartesian coordinates
        double[] A_cart = latLonToCartesian(A.lat(), A.lng());
        double[] B_cart = latLonToCartesian(B.lat(), B.lng());
        double[] C_cart = latLonToCartesian(C.lat(), C.lng());

        // Calculate vectors AB and BC
        double[] AB = subtractVector(B_cart, A_cart);
        double[] BC = subtractVector(C_cart, B_cart);

        // Normalize the vectors
        AB = normalize(AB);
        BC = normalize(BC);

        // Calculate the dot product of AB and BC
        double dotProduct = dotProduct(AB, BC);

        // Calculate the angle in radians and then convert to degrees
        double theta = Math.acos(dotProduct);

        // Calculate the cross product to determine the direction
        int direction = determineDirection(A, B, C);

        // Determine the clockwise angle
        return Math.PI - theta * direction;
    }

    public static int determineDirection(Position a, Position b, Position center) {
        double crossProduct = (b.lng() - a.lng()) * (center.lat() - a.lat()) - (b.lat() - a.lat()) * (center.lng() - a.lng());
        // + is left
        // - is right
        return (int) Math.signum(crossProduct);
    }


    public static Position convert(String dms) {
        Pattern pattern = Pattern.compile("(\\d\\d)°(\\d\\d)′(\\d\\d)″ пн. ш. (\\d\\d)°(\\d\\d)′(\\d\\d)″ сх. д.");
        Matcher matcher = pattern.matcher(dms);
        if (matcher.find()) {
            double lat = parseDouble(matcher.group(1)) + parseDouble(matcher.group(2)) / 60 + parseDouble(matcher.group(3)) / 3600;
            double lng = parseDouble(matcher.group(4)) + parseDouble(matcher.group(5)) / 60 + parseDouble(matcher.group(6)) / 3600;
            return new Position(lat, lng);
        }
        throw new IllegalArgumentException("Invalid DMS format: " + dms);
    }

    // TODO: refactor to reuse existing code
    public static Position getPerpendicularPosition(Position A, Position b) {
        // Convert geographic coordinates (lat, lon) to Cartesian coordinates (x, y, z)
        double[] cartesianA = latLonToCartesian(A.lat(), A.lng());
        double[] cartesianB = latLonToCartesian(b.lat(), b.lng());

        double[] vectorAB = subtractVector(cartesianB, cartesianA);

        // Calculate vector BC, which is perpendicular to AB using cross product
        double[] vectorBC = crossProduct(cartesianB, vectorAB);

        // Find the coordinates of point C by adding BC to the Cartesian coordinates of B
        double[] cartesianC = new double[]{
                cartesianB[0] + vectorBC[0],
                cartesianB[1] + vectorBC[1],
                cartesianB[2] + vectorBC[2]
        };

       return toGeographic(cartesianC);
    }

    // Converts latitude and longitude to Cartesian coordinates
    public static double[] latLonToCartesian(double lat, double lon) {
        lat = Math.toRadians(lat);
        lon = Math.toRadians(lon);

        double x = Math.cos(lat) * Math.cos(lon);
        double y = Math.cos(lat) * Math.sin(lon);
        double z = Math.sin(lat);
        return new double[]{x, y, z};
    }

    private static Position toGeographic(double[] cartesian) {
        double lat = Math.asin(cartesian[2]);
        double lon = Math.atan2(cartesian[1], cartesian[0]);
        return new Position(Math.toDegrees(lat), Math.toDegrees(lon));
    }

    public static Position cartesianToLatLon(double[] xyz) {
        double x = xyz[0], y = xyz[1], z = xyz[2];
        double lat = Math.atan2(z, Math.sqrt(x * x + y * y));
        double lon = Math.atan2(y, x);

        return new Position(Math.toDegrees(lat), Math.toDegrees(lon));
    }

    public static double[] latLonToCartesian(Position latLon) {
        double lat = Math.toRadians(latLon.lat());
        double lon = Math.toRadians(latLon.lng());

        double x = Math.cos(lat) * Math.cos(lon);
        double y = Math.cos(lat) * Math.sin(lon);
        double z = Math.sin(lat);

        return new double[]{x, y, z};
    }

    // Calculates the cross product of two vectors
    public static double[] crossProduct(double[] v1, double[] v2) {
        return new double[]{
                v1[1] * v2[2] - v1[2] * v2[1],
                v1[2] * v2[0] - v1[0] * v2[2],
                v1[0] * v2[1] - v1[1] * v2[0]
        };
    }

    // Calculates the dot product of two vectors
    public static double dotProduct(double[] v1, double[] v2) {
        return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
    }

    // Subtracts two vectors
    public static double[] subtractVector(double[] v1, double[] v2) {
        return new double[]{v1[0] - v2[0], v1[1] - v2[1], v1[2] - v2[2]};
    }

    // Normalizes a vector
    public static double[] normalize(double[] v) {
        double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[]{v[0] / length, v[1] / length, v[2] / length};
    }
}