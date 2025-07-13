package noescape.model;

import noescape.util.CoordUtils;

import java.math.BigDecimal;

import static java.lang.Double.parseDouble;
import static java.math.RoundingMode.HALF_DOWN;

public record Position(double lat, double lng) {

    public static Position parse(String s) {
        if (s == null) {
            return null;
        }
        String[] parts = s.split(",");
        return new Position(parseDouble(parts[0]), parseDouble(parts[1]));
    }

    public Position {
        if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
            throw new IllegalArgumentException("Not a finite number: " + lat + "," + lng);
        }
    }

    public Position middle(Position other) {
        return new Position(this.lat + (other.lat - this.lat) / 2, this.lng + (other.lng - this.lng) / 2);
    }

    public Position round(int scale) {
        return new Position(
                BigDecimal.valueOf(lat).setScale(scale, HALF_DOWN).doubleValue(),
                BigDecimal.valueOf(lng).setScale(scale, HALF_DOWN).doubleValue());
    }

    public double distanceTo(Position position) {
        return CoordUtils.calculateDistance(this, position);
    }

    @Override
    public String toString() {
        return lat + "," + lng;
    }
}
