package noescape.model;

import noescape.service.Lemmatizer;

import java.util.Comparator;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

public record Locality(String settlement, String gromada, String rayon, String oblast) implements Comparable<Locality> {

    public Locality {
        validate(settlement);
        validate(gromada);
        validate(rayon);
        validate(oblast);
    }

    @Override
    public int compareTo(Locality that) {
        return Comparator.comparing(Locality::oblast, nullsFirst(naturalOrder()))
                .thenComparing(Locality::rayon, nullsFirst(naturalOrder()))
                .thenComparing(Locality::gromada, nullsFirst(naturalOrder()))
                .thenComparing(Locality::settlement, nullsFirst(naturalOrder()))
                .compare(this, that);
    }

    public Locality withSettlement(String settlement) {
        return new Locality(settlement, gromada, rayon, oblast);
    }

    public Locality withGromada(String gromada) {
        return new Locality(settlement, gromada, rayon, oblast);
    }

    public Locality withRayon(String rayon) {
        return new Locality(settlement, gromada, rayon, oblast);
    }

    public Locality withOblast(String oblast) {
        return new Locality(settlement, gromada, rayon, oblast);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(settlement);
        if (gromada != null) {
            sb.append(", ").append(gromada).append(" отг");
        }
        if (rayon != null) {
            sb.append(", ").append(rayon).append(" рн.");
        }
        if (oblast != null) {
            sb.append(", ").append(oblast).append(" обл.");
        }
        return sb.toString();
    }

    private static void validate(String value) {
        if (value == null) return;

        if ("\"'’ʹ‘`".chars().mapToObj(b -> Character.toString((char) b)).anyMatch(value::contains)) {
            throw new IllegalArgumentException(value);
        }
        if (value.equals("отг")) {
            throw new IllegalArgumentException(value);
        }
        if (value.contains(" отг")) {
            throw new IllegalArgumentException(value);
        }
        if (value.contains(" громада")) {
            throw new IllegalArgumentException(value);
        }
        if (value.contains(" громади")) {
            throw new IllegalArgumentException(value);
        }
        if (value.contains("територі")) {
            throw new IllegalArgumentException(value);
        }
        if (value.contains("обʼєднан")) {
            throw new IllegalArgumentException(value);
        }
        if (value.contains("прикордонн")) {
            throw new IllegalArgumentException(value);
        }
        if (value.contains("контроль")) {
            throw new IllegalArgumentException(value);
        }
        if (value.contains("район")) {
            throw new IllegalArgumentException(value);
        }
        if (value.contains("область")) {
            throw new IllegalArgumentException(value);
        }
        if (!value.equals(Lemmatizer.capitalizeName(value))) {
            throw new IllegalArgumentException(value);
        }
    }
}
