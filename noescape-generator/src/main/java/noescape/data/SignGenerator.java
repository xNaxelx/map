package noescape.data;

import noescape.model.BorderSign;
import noescape.repository.BorderSignRepository;

import java.util.Set;

public class SignGenerator {
    public static String generateSignsTxt(Set<BorderSign.Key> signs) {
        var borderSignRepository = new BorderSignRepository();
        var borderSigns = borderSignRepository.getBorderSigns();

        var sb = new StringBuilder();
        for (var borderSign : borderSigns) {
            if (signs.contains(borderSign.key())) {
                var position = borderSign.position().round(5);
                sb.append(borderSign.country().replace("BY2", "BY")).append('\t')
                        .append(borderSign.title()).append('\t')
                        .append(position.lat()).append('\t')
                        .append(position.lng()).append('\t')
                        .append(borderSign.generated()).append('\n');
            }
        }
        return sb.toString();
    }
}
