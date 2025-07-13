package noescape.repository;

import noescape.model.BorderSign;
import noescape.model.Position;
import noescape.service.BorderSignKmlParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.sort;

public class BorderSignRepository {
    private static final File FILE = new File("kml/signs.kml");

    private final List<BorderSign> signs;
    private final BorderSign.Key[] signKeys;

    public BorderSignRepository() {
        this(readBorderSignsFromKlm());
    }

    public BorderSignRepository(List<BorderSign> signs) {
        this.signs = new ArrayList<>(signs);
        this.signs.addAll(computeMissingBorderSigns(signs));
        sort(this.signs);
        signKeys = this.signs.stream().map(BorderSign::key).toArray(BorderSign.Key[]::new);
        checkDuplicates(signKeys);
    }

    private void checkDuplicates(BorderSign.Key[] signKeys) {
        HashSet<BorderSign.Key> keys = new HashSet<>();
        for (BorderSign.Key key : signKeys) {
            if (!keys.add(key)) {
                throw new IllegalStateException("Duplicate border sign " + key);
            }
        }
    }

    public static List<BorderSign> readBorderSignsFromKlm() {
        try {
            return BorderSignKmlParser.parse(Files.readAllBytes(FILE.toPath()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<BorderSign> getBorderSigns() {
        return signs;
    }

    public BorderSign getBorderSign(String country, String borderSign) {
        if (country == null || borderSign == null) return null;
        int pos = Arrays.binarySearch(signKeys, BorderSign.key(country, borderSign));
        if (pos < 0) {
            return null;
        }
        return signs.get(pos);
    }

    public BorderSign getPrev(BorderSign.Key key) {
        int pos = Arrays.binarySearch(signKeys, key);
        return pos < 1 ? null : signs.get(pos - 1);
    }

    public BorderSign getNext(BorderSign.Key key) {
        int pos = Arrays.binarySearch(signKeys, key);
        return pos < signs.size() - 1 ? signs.get(pos + 1) : null;
    }

    private static List<BorderSign> computeMissingBorderSigns(List<BorderSign> borderSigns) {
        sort(borderSigns);
        var computed = new ArrayList<BorderSign>();
        for (int i = 1; i < borderSigns.size(); i++) {
            var prev = borderSigns.get(i - 1);
            var curr = borderSigns.get(i);

            if (!prev.country().equals(curr.country())) continue;
            if ("BY".equals(prev.country())) continue; // BY signs is a mess

            var prevNum = prev.key().num();
            var nextNum = curr.key().num();

            if (prevNum == nextNum) {
                var prevExt = prev.key().ext();
                var nextExt = curr.key().ext();
                var title = prev.country().equals("MD") ? String.format("%04d", prevNum) : String.valueOf(prevNum);
                for (int ext = prevExt + 1; ext < nextExt; ext++) {
                    var k = (double) (ext - prevExt) / (nextExt - prevExt);
                    var position = getPositionBetween(prev.position(), curr.position(), k);
                    computed.add(new BorderSign(prev.country(), title + "/" + ext, position, true));
                }
            }


            var numDiff = nextNum - prevNum;
            if (numDiff < 20) {
                for (int num = prevNum + 1; num < nextNum; num++) {
                    var title = prev.country().equals("MD") ? String.format("%04d", num) : String.valueOf(num);
                    var k = (double) (num - prevNum) / numDiff;
                    var position = getPositionBetween(prev.position(), curr.position(), k);
                    computed.add(new BorderSign(prev.country(), title, position, true));
                }
            }
        }
        return computed;
    }

    private static Position getPositionBetween(Position prev, Position next, double k) {
        var lat = prev.lat() + k * (next.lat() - prev.lat());
        var lng = prev.lng() + k * (next.lng() - prev.lng());
        return new Position(lat, lng);
    }

    public static void main(String[] args) {
        BorderSignRepository repository = new BorderSignRepository();
        repository.getBorderSigns().forEach(System.out::println);
    }
}
