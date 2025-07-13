package noescape;

import noescape.model.BorderSign;
import noescape.model.Position;
import noescape.repository.BorderSignRepository;

import static noescape.util.GeospatialCalculation.calculateAzimuth;
import static noescape.util.GeospatialCalculation.computePerpendicularAtEnd;

public class ArrestPositionCalculator {

    private final BorderSignRepository borderSignRepository;

    public ArrestPositionCalculator(BorderSignRepository borderSignRepository) {
        this.borderSignRepository = borderSignRepository;
    }

    public Position computeSector(double distanceToBorder, BorderSign targetBorderSign, Position preferredDirection) {
        // target is the position of the border sign where the person was heading
        Position target = targetBorderSign.position();

        // Let's loop through all border signs starting from target in the clockwise direction
        BorderSign leftBorderSign = borderSignRepository.getPrev(targetBorderSign.key());

        // The azimuth of the border segment adjacent to target
        double initialAzimuth = calculateAzimuth(target, leftBorderSign.position());
        double initialAzimuthDegrees = Math.toDegrees(initialAzimuth);
        double sectorAngle = 0;

        double leftAzimuth = initialAzimuth;
        Position leftAzimuthPosition = leftBorderSign.position();
        while (leftBorderSign != null) {
            // Position at distance measured from the end of border segment
            // That is potentially the closest possible point to the border
            Position positionAtDistanceFromBorder = computePerpendicularAtEnd(target, leftBorderSign.position(), distanceToBorder);

            // Azimuth from target border sign to the just computed point
            double positionAzimuth = calculateAzimuth(target, positionAtDistanceFromBorder);

            // Check if this is the maximum possible azimuth and the closest position to the border
            double azimuthDiff = positionAzimuth - initialAzimuth;
            if (azimuthDiff > leftAzimuth - initialAzimuth) {
                leftAzimuth = positionAzimuth;
                leftAzimuthPosition = positionAtDistanceFromBorder;
            }

            // Move on to the next border sign in the clockwise direction
            leftBorderSign = borderSignRepository.getPrev(leftBorderSign.key());
        }

        // Now, loop through all border signs starting from target in the counter-clockwise direction
        BorderSign rightBorderSign = borderSignRepository.getNext(targetBorderSign.key());

        double rightAzimuth = calculateAzimuth(target, rightBorderSign.position());
        Position rightAzimuthPosition = rightBorderSign.position();
        while (rightBorderSign != null) {
            // Position at distance measured from the end of border segment
            // That is potentially the closest possible point to the border
            Position positionAtDistanceFromBorder = computePerpendicularAtEnd(target, rightBorderSign.position(), -distanceToBorder);

            // Azimuth from target border sign to the just computed point
            double positionAzimuth = calculateAzimuth(target, positionAtDistanceFromBorder);

            // Check if this is the minimum possible azimuth and the closest position to the border
            double azimuthDiff = positionAzimuth - initialAzimuth;
            if (azimuthDiff < sectorAngle) {
                sectorAngle = azimuthDiff;
                rightAzimuthPosition = positionAtDistanceFromBorder;
            }

            // Move on to the next border sign in the counter-clockwise direction
            rightBorderSign = borderSignRepository.getNext(rightBorderSign.key());
        }

        if (preferredDirection != null) {
            double preferredAzimuth = calculateAzimuth(target, preferredDirection);
            if (preferredAzimuth - initialAzimuth < sectorAngle) {
                // Preferred direction is not within the possible sector - return closest possible position
                return rightAzimuthPosition;
            }
        }

        return null;
    }
}
