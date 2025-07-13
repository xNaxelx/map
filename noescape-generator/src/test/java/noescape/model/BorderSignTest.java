package noescape.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.shuffle;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BorderSignTest {

    @Test
    void testCompare() {
        Position p = new Position(0, 0);
        BorderSign RO1 = new BorderSign("RO", "1", p, false);
        BorderSign RO99 = new BorderSign("RO", "99", p, false);
        BorderSign PL1 = new BorderSign("PL", "1", p, false);
        BorderSign PL99 = new BorderSign("PL", "99", p, false);
        BorderSign SK1 = new BorderSign("SK", "1", p, false);
        BorderSign SK99 = new BorderSign("SK", "99", p, false);
        BorderSign HU1 = new BorderSign("HU", "1", p, false);
        BorderSign HU99 = new BorderSign("HU", "99", p, false);
        BorderSign MD0001 = new BorderSign("MD", "0001", p, false);
        BorderSign MD0999 = new BorderSign("MD", "0099", p, false);

        assertEquals(0, RO1.compareTo(RO1));

        List<BorderSign> list = asList(RO1, RO99, PL1, PL99, SK1, SK99, MD0001, MD0999, HU1, HU99);
        shuffle(list);
        list.sort(BorderSign::compareTo);
        assertEquals(List.of(PL99, PL1, SK1, SK99, HU99, HU1, RO1, RO99, MD0001, MD0999), list);
    }
}