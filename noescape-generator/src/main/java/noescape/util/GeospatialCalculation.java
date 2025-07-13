package noescape.util;

import noescape.model.Position;

public class GeospatialCalculation {

    public static final double EARTH_RADIUS = 6371000;

    /**
     * Given geospatial coordinates of points A and B and the distance from B to C in meters. Angle ABC is right.
     * Compute the coordinates of point C.
     */
    public static Position computePerpendicularAtEnd(Position A, Position B, double distanceBC) {
        // Convert latitudes and longitudes from degrees to radians
        double latARad = Math.toRadians(A.lat());
        double lonARad = Math.toRadians(A.lng());
        double latBRad = Math.toRadians(B.lat());
        double lonBRad = Math.toRadians(B.lng());

        // Calculate the bearing (azimuth) from A to B
        double azimuthAB = calculateAzimuth(latARad, lonARad, latBRad, lonBRad);

        // Azimuth from B to C (90 degrees from AB, because ABC is a right angle)
        double azimuthBC = azimuthAB + Math.PI / 2.0; // Adding 90 degrees in radians

        // Now, compute the coordinates of C using geodesic formulas
        double[] C = computeDestinationPoint(latBRad, lonBRad, azimuthBC, distanceBC);

        // Return the coordinates of point C in degrees
        return new Position(Math.toDegrees(C[0]), Math.toDegrees(C[1]));
    }

    /**
     * Calculate azimuth (bearing) between two points (angle NAB, where N is the North Pole)
     * 0 degrees is North direction,
     * 90 degrees is East direction,
     * -90 degrees is West direction,
     * 180 degrees is South direction,
     *
     * @return angle in radians in the range from -pi to pi
     */
    public static double calculateAzimuth(Position A, Position B) {
        return calculateAzimuth(
                Math.toRadians(A.lat()), Math.toRadians(A.lng()),
                Math.toRadians(B.lat()), Math.toRadians(B.lng()));
    }

    // Calculate the azimuth (bearing) between two points
    // Result is in radians in the range from -pi to pi
    private static double calculateAzimuth(double lat1, double lon1, double lat2, double lon2) {
        double dLon = lon2 - lon1;

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        return Math.atan2(y, x);
    }

    // Compute the destination point given the start point, bearing, and distance
    private static double[] computeDestinationPoint(double lat, double lon, double azimuth, double distance) {
        double angularDistance = distance / EARTH_RADIUS; // Convert distance to angular distance in radians

        double newLat = Math.asin(Math.sin(lat) * Math.cos(angularDistance) +
                Math.cos(lat) * Math.sin(angularDistance) * Math.cos(azimuth));

        double newLon = lon + Math.atan2(Math.sin(azimuth) * Math.sin(angularDistance) * Math.cos(lat),
                Math.cos(angularDistance) - Math.sin(lat) * Math.sin(newLat));

        return new double[]{newLat, newLon};
    }
}
