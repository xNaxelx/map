package noescape.service;

import noescape.model.Locality;
import org.apache.commons.lang3.StringUtils;

public class Lemmatizer {

    public static Locality lemmatizeLocality(Locality locality) {
        String settlement = locality.settlement();
        String gromada = lemmatizeGromada(locality.gromada());
        String rayon = lemmatizeRayon(locality.rayon());
        String oblast = lemmatizeOblast(locality.oblast());
        return new Locality(settlement, gromada, rayon, oblast);
    }

    public static String lemmatizeOblast(String oblast) {
        oblast = capitalizeName(oblast);
        return lemmatizeGromada(oblast);
    }

    public static String lemmatizeRayon(String rayon) {
        rayon = capitalizeName(rayon);
        return replacePostfix(rayon, "кого", "кий");
    }

    public static String lemmatizeGromada(String gromada) {
        gromada = capitalizeName(gromada);
        gromada = replacePostfix(gromada, "кій", "ка");
        gromada = replacePostfix(gromada, "кої", "ка");
        gromada = replacePostfix(gromada, "кою", "ка");
        gromada = replacePostfix(gromada, "ній", "на");
        return gromada;
    }

    private static String replacePostfix(String name, String postfix, String replacement) {
        if (name == null) return null;
        var result = StringUtils.removeEnd(name, postfix);
        return result.equals(name) ? result : result + replacement;
    }

    public static String capitalizeName(String name) {
        if (name == null) return null;
        name = StringUtils.capitalize(name.toLowerCase());
        int i = name.indexOf(' ');
        if (i > 0 && i < name.length() - 2) {
            name = name.substring(0, i) + ' ' + StringUtils.capitalize(name.substring(i + 1));
        }
        i = name.indexOf('-');
        if (i > 0 && i < name.length() - 2) {
            name = name.substring(0, i) + '-' + StringUtils.capitalize(name.substring(i + 1));
        }
        return name;
    }
}