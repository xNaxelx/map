package noescape.model;

import java.util.List;

import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public record BorderSign(Key key, Position position, boolean generated) implements Comparable<BorderSign> {

    public record Key(String country, int num, int ext) implements Comparable<Key> {
        public static final List<String> COUNTRIES = List.of("RU", "BY", "BY2", "PL", "SK", "HU", "RO", "MD");

        // positive direction when border signs numerated along border counter-clockwise
        private static final int[] directions = {1, -1, 1, -1, 1, -1, 1, 1, 1};

        public Key {
            if (!COUNTRIES.contains(country)) {
                throw new IllegalArgumentException("Invalid country: '" + country + "'");
            }
        }

        public static String formatTitle(String country, String title) {
            var num = parseInt(substringBefore(title, '/'));
            var ext = title.contains("/") ? parseInt(substringAfter(title, '/')) : 0;
            return formatTitle(country, num, ext);
        }

        public static String formatTitle(String country, int num, int ext) {
            return ("MD".equals(country) || "BY".equals(country) ? String.format("%04d", num) : num) + (ext == 0 ? "" : "/" + ext);
        }

        @Override
        public int compareTo(Key that) {
            int thisCountryIndex = this.countryIndex();
            int thatCountryIndex = that.countryIndex();
            int countryResult = thisCountryIndex - thatCountryIndex;
            if (countryResult != 0) return countryResult;

            int direction = directions[thisCountryIndex];
            int thisNum = this.num * direction;
            int thatNum = that.num * direction;
            int numResult = thisNum - thatNum;
            if (numResult != 0) return numResult;

            int thisExt = this.ext * direction;
            int thatExt = that.ext * direction;
            return thisExt - thatExt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key that = (Key) o;
            return this.compareTo(that) == 0;
        }

        @Override
        public String toString() {
            return country + formatTitle(country, num, ext);
        }

        private int countryIndex() {
            if (country.equals("RO") && num() >= 1335) {
                return 7;
            }
            return COUNTRIES.indexOf(country);
        }
    }

    public static Key key(String country, String title) {
        var num = parseInt(substringBefore(title, '/'));
        var ext = title.contains("/") ? parseInt(substringAfter(title, '/')) : 0;
        return new Key(country, num, ext);
    }

    public BorderSign {
        requireNonNull(key);
        requireNonNull(position);
    }

    public BorderSign(String country, String title, Position position, boolean generated) {
        this(key(country, title), position, generated);
    }

    @Override
    public int compareTo(BorderSign that) {
        return this.key().compareTo(that.key());
    }

    public String country() {
        return key.country;
    }

    public String title() {
        return Key.formatTitle(key.country, key.num, key.ext);
    }
}
