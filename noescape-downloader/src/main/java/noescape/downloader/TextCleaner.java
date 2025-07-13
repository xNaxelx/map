package noescape.downloader;

import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.replaceIgnoreCase;

public class TextCleaner {

    public static String cleanText(String text) {
        text = text.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(TextCleaner::fixTypos)
                .map(Tokenizer::tokenizeToString)
                .collect(joining("\n"));
        text = text.replaceAll("c([а-яґєії])", "с$1"); // latin to cyrillic
        text = text.replaceAll("C([а-яґєіїА-ЯҐЄІЇ])", "С$1");
        text = text.replaceAll("(-?[А-ЯҐЄІЇ][а-яґєіїʼ]+) - ([А-ЯҐЄІЇ][а-яґєіїʼ]+)", "$1-$2"); // Remove spaces around dash in names
        text = text.replaceAll("([А-ЯҐЄІЇа-яґєіїʼ]+о) - ([А-ЯҐЄІЇа-яґєії][а-яґєіїʼ]+(:?ка|на|ий|ке|не|го|ої|их|ій|му)) ", "$1-$2 "); // Remove spaces around dash in compound words
        text = text.replaceAll("(\\d) - (\\d)", "$1-$2"); // Remove spaces around dash between digits
        text = text.replaceAll("(\\d) - ([IVX])", "$1-$2"); // Remove spaces around dash before roman numerals (law numbers)
        text = text.replaceAll("як([а-яґєії]*) - небудь", "як$1-небудь");
        text = text.replace("генерала - хоружного", "генерала-хоружного");
        text = text.replace("генерал - хоруж", "генерал-хоруж");
        text = text.replace("будь - як", "будь-як");
        text = text.replace(" р - н", " р-н");
        text = text.replace(" с - ще ", " с-ще ");
        text = text.replace(" с - ща ", " с-ща ");
        text = text.replace("по - батькові", "по-батькові");
        text = text.replace(" с - нт", " с-нт");
        text = text.replace(" 0 , 2 розмір", " 0,2 розмір");
        text = text.replace("/ РФ /", "/РФ/");
        text = text.replace("/ Укр. /", "/Укр./");
        text = text.replace("/ Україна /", "/Україна/");
        text = text.replace("/ Р. Молдова /", "/Р. Молдова/");
        text = text.replace("/ РМ /", "/РМ/");
        text = text.replace("/ ПМР /", "/ПМР/");
        text = text.replace("/ пмр /", "/пмр/");
        text = text.replace("/ РБ /", "/РБ/");
        text = text.replaceAll(" \" (ІНФОРМАЦІЯ_\\d) \" ", " «$1» ");
        text = text.replace(" п / нз ", " п/нз ");
        text = text.replace(" п / зн", " п/зн");
        text = text.replace(" п / з ", " п/з ");
        text = text.replace(" пп / зн", " пп/зн");
        text = text.replace(" пн / зн", " пн/зн");
        text = text.replace(" пп / з ", " пп/з ");
        text = text.replace(" П / ЗН ", " П/ЗН ");
        text = text.replaceAll("(\\d+) - го ", "$1-го ");
        text = text.replaceAll("(\\d+) - ти ", "$1-ти ");
        text = text.replaceAll("(\\d+) - и ", "$1-и ");
        text = text.replaceAll("(\\d+) - й ", "$1-й ");
        text = text.replaceAll("(\\d+) - ої ", "$1-ої ");
        text = text.replaceAll("(\\d+) - х ", "$1-х ");
        text = text.replaceAll("(\\d+) - річн", "$1-річн");
        text = text.replaceAll(" ([,.;:»)])", "$1");
        text = text.replaceAll("([«(]) ", "$1");
        text = text.replaceAll("([БВГДЖЗКМПРСТФХЧШ]) ?['’ʹʼ‘`?\"] ?([ЯЄЇЮ])", "$1ʼ$2"); // Replace with Ukrainian apostrophe U+02BC
        text = text.replaceAll("([БВГДЖЗКМПРСТФХЧШбвгджзкмпрстфхчш]) ?['’ʹʼ‘`?\"] ?([яєїю])", "$1ʼ$2"); // Replace with Ukrainian apostrophe U+02BC
        text = text.replaceAll(" UA (\\d{10,})", " UA$1"); // IBAN
        text = text.replaceAll(" (\\d+[,.]) (\\d\\d грн)", " $1$2"); // Space before decimals in UAH amounts
        text = text.replaceAll(" (\\d+[,.]) 00 ", " $100 ");  // Space before decimals in amounts
        text = text.replaceAll(" ([а-яґєії]) / ([а-яґєії]) ", " $1/$2 ");
        text = text.replaceAll(" ([А-ЯҐЄІЇ]) / ([А-ЯҐЄІЇ]) ", " $1/$2 ");
        text = text.replaceAll(" (\\d{1,2}), (\\d{1,2}) км ", " $1,$2 км ");

        text = text.lines().map(line -> {
//            if (StringUtils.countMatches(line, " \" ") == 2) {
//                line = line.replaceFirst(" \" ", " \"");
//                line = line.replaceFirst(" \" ", "\" ");
//            }
            if (StringUtils.countMatches(line, " / ") == 2) {
                line = line.replaceFirst(" / ", " /");
                line = line.replaceFirst(" / ", "/ ");
            }
            line = fixSpacingInsideDoubleQuotes(line);
            return line;
        }).collect(joining("\n"));
        text = text.replaceAll("\" (\\d\\d?) \"", "«$1»");

        text = replaceIgnoreCase(text, "постановив:", "ПОСТАНОВИВ:");
        text = replaceIgnoreCase(text, "І М Е Н Е М У К Р А Ї Н И", "ІМЕНЕМ УКРАЇНИ");

        text = replaceIgnoreCase(text, "В С Т А Н О В И Л А", "ВСТАНОВИЛА");
        text = replaceIgnoreCase(text, "В С Т А Н О В И ЛА", "ВСТАНОВИЛА");
        text = replaceIgnoreCase(text, "В С Т А Н О В ИЛ А", "ВСТАНОВИЛА");
        text = replaceIgnoreCase(text, "В С Т А Н О ВИ Л А", "ВСТАНОВИЛА");
        text = replaceIgnoreCase(text, "В С Т А Н ОВ И Л А", "ВСТАНОВИЛА");
        text = replaceIgnoreCase(text, "В С Т А НО В И Л А", "ВСТАНОВИЛА");
        text = replaceIgnoreCase(text, "В С Т АН О В И Л А", "ВСТАНОВИЛА");
        text = replaceIgnoreCase(text, "В С ТА Н О В И Л А", "ВСТАНОВИЛА");

        text = replaceIgnoreCase(text, "П О С Т А Н О В И Л А", "ПОСТАНОВИЛА");
        text = replaceIgnoreCase(text, "П О С Т А Н О В ИЛ А", "ПОСТАНОВИЛА");
        text = replaceIgnoreCase(text, "П О С Т А Н О ВИ Л А", "ПОСТАНОВИЛА");
        text = replaceIgnoreCase(text, "П О С Т А Н ОВ И Л А", "ПОСТАНОВИЛА");
        text = replaceIgnoreCase(text, "П О С Т А НО В И Л А", "ПОСТАНОВИЛА");
        text = replaceIgnoreCase(text, "П О С Т АН О В И Л А", "ПОСТАНОВИЛА");
        text = replaceIgnoreCase(text, "П О С ТА Н О В И Л А", "ПОСТАНОВИЛА");

        text = replaceIgnoreCase(text, "П О С Т А Н О В А", "ПОСТАНОВА");
        text = replaceIgnoreCase(text, "П О С Т А Н О ВА", "ПОСТАНОВА");
        text = replaceIgnoreCase(text, "П О С Т А Н ОВ А", "ПОСТАНОВА");
        text = replaceIgnoreCase(text, "П О С Т А НО В А", "ПОСТАНОВА");
        text = replaceIgnoreCase(text, "П О С Т АН О В А", "ПОСТАНОВА");
        text = replaceIgnoreCase(text, "П О С ТА Н О В А", "ПОСТАНОВА");

        text = replaceIgnoreCase(text, "В С Т А Н О В И В", "ВСТАНОВИВ");
        text = replaceIgnoreCase(text, "В С Т А Н О В ИВ", "ВСТАНОВИВ");
        text = replaceIgnoreCase(text, "В С Т А Н О ВИ В", "ВСТАНОВИВ");
        text = replaceIgnoreCase(text, "В С Т А Н ОВ И В", "ВСТАНОВИВ");
        text = replaceIgnoreCase(text, "В С Т А НО В И В", "ВСТАНОВИВ");
        text = replaceIgnoreCase(text, "В С Т АН О В И В", "ВСТАНОВИВ");
        text = replaceIgnoreCase(text, "В С ТА Н О В И В", "ВСТАНОВИВ");

        text = replaceIgnoreCase(text, "П О С Т А Н О В И В", "ПОСТАНОВИВ");
        text = replaceIgnoreCase(text, "П О С Т А Н О В ИВ", "ПОСТАНОВИВ");
        text = replaceIgnoreCase(text, "П О С Т А Н О ВИ В", "ПОСТАНОВИВ");
        text = replaceIgnoreCase(text, "П О С Т А Н ОВ И В", "ПОСТАНОВИВ");
        text = replaceIgnoreCase(text, "П О С Т А НО В И В", "ПОСТАНОВИВ");
        text = replaceIgnoreCase(text, "П О С Т АН О В И В", "ПОСТАНОВИВ");
        text = replaceIgnoreCase(text, "П О С ТА Н О В И В", "ПОСТАНОВИВ");

        text = replaceIgnoreCase(text, "У С Т А Н О В И В", "ВСТАНОВИВ");
        text = replaceIgnoreCase(text, "У С Т А Н О В И Л А", "ВСТАНОВИЛА");
        text = replaceIgnoreCase(text, "П О С Т А Н О В Л Я Ю", "ПОСТАНОВЛЯЮ");

        text = text.replace("С у д д я", "Суддя");
        text = text.replace("Р е н і й с ь к о г о", "Ренійського");
        text = text.replace("р а й о н н о г о", "районного");
        text = text.replace("с у д у", "суду");
        text = text.replace("С У Д Д Я", "СУДДЯ");
        text = text.replace("О д е с ь к о ї", "Одеської");
        text = text.replace("о б л а с т і", "області");
        text = text.replace("С о р о к і н К. В.", "Сорокін К. В.");
        text = text.replace("ГАРТ - 1 / П", "ГАРТ-1/П");
        text = text.replace("ГАРТ 1 / П", "ГАРТ 1/П");
        text = text.replace("Гарт 1 / П", "Гарт 1/П");
        text = text.replace("Гарт - 5", "Гарт-5");
        text = text.replace(" п / п", " п/п");
        text = text.replace("cm", "ст");

        text = text.replace("\" Про державний кордон України \"", "«Про державний кордон України»");
        text = text.replace("\" Про громадянство України \"", "«Про громадянство України»");
        text = text.replace("\" Прикордонний патруль \"", "«Прикордонний патруль»");
        text = text.replace("\" Пост спостереження \"", "«Пост спостереження»");
        text = text.replace("\" Група реагування \"", "«Група реагування»");
        text = text.replace("\" Контрольний пост \"", "«Контрольний пост»");
        text = text.replace("\" поза розумним сумнівом \"", "«поза розумним сумнівом»");
        text = text.replace("\" внутрішнього переконання \"", "«внутрішнього переконання»");
        text = text.replace("\" поза межами розумного сумніву \"", "«поза межами розумного сумніву»");
        text = text.replace("\" адміністративні штрафи \"", "«адміністративні штрафи»");
        text = text.replace("\" судовий збір \"", "«судовий збір»");
        text = text.replace("\" КрП \"", "«КрП»");


        text = restoreUnderlines(text);
        text = fixQuotes(text);
        return text;
    }

    private static String fixSpacingInsideDoubleQuotes(String text) {
        char[] chars = text.toCharArray();
        int[] qPos = new int[chars.length];
        int count = 0;
        for (int i = 0, len = chars.length; i < len; i++) {
            char c = chars[i];
            if (c == '"') {
                qPos[count++] = i;
            }
        }
        if (count > 0 && count % 2 == 0) {
            var sb = new StringBuilder(text);
            for (int i = count - 1; i >= 0; i -= 2) {
                int start = qPos[i - 1] + 1;
                int end = qPos[i] - 1;
                if ((end - start) > 2 && Character.isUpperCase(chars[start + 1])) {
                    if (chars[end] == ' ') {
                        sb.deleteCharAt(end);
                    }
                    if (chars[start] == ' ') {
                        sb.deleteCharAt(start);
                    }
                }
            }
            return sb.toString();
        }
        return text;
    }

    private static String restoreUnderlines(String text) {
        var sb = new StringBuilder();
        sb.append(text.charAt(0));
        for (int i = 1; i < text.length() - 1; i++) {
            char ch = text.charAt(i);
            if (text.charAt(i - 1) == '_' && ch == ' ' && text.charAt(i + 1) == '_') {
                continue;
            }
            sb.append(ch);
        }
        sb.append(text.charAt(text.length() - 1));
        return sb.toString();
    }

    public static String fixQuotes(String text) {
        text = text.replaceAll(" \" ([А-ЯІЇЄ_1-9]+) \" ", " «$1» ");
        var sb = new StringBuilder(text);
        int open = -1;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if ("'’ʹ‘„`«»\"".indexOf(c) >= 0) {
                if (i == 0 || " .,;?!\n".indexOf(sb.charAt(i - 1)) >= 0) {
                    open = i;
                } else if (i == sb.length() - 1 || ".,;?! \n".indexOf(sb.charAt(i + 1)) >= 0) {
                    if (open >= 0 && open < i - 1 && "'’„ʹ‘`»\"".indexOf(sb.charAt(i - 1)) < 0) {
                        if (c == '»') {
                            sb.setCharAt(open, '«');
                        } else if (sb.charAt(open) == '«') {
                            sb.setCharAt(i, '»');
                        } else {
                            sb.setCharAt(open, '«');
                            sb.setCharAt(i, '»');
                        }
                    }
                    open = -1;
                }
            }
            if (c == '\n') open = -1;
        }
        return sb.toString();
    }

    public static String fixTypos(String text) {
        text = text.replace("п/зн 0 713", "п/зн 0713");
        text = text.replace("пп/зн №04.09", "пп/зн №0409");
        text = text.replace("гоидні", "годині");
        text = text.replace("Украйни", "України");
        text = text.replace("Украяїни", "України");
        text = text.replace("обʼєднано ", "обʼєднаної ");
        text = text.replace("обʼєднаною територіальної", "обʼєднаної територіальної");
        text = text.replace("00340/02", "0340/02");
        text = text.replace("Чадир - Лунга (РМ), в районі прикордонного знаку №0788", "Чадир - Лунга (РМ), в районі прикордонного знаку №0738");
        text = text.replace("Підгірне (України, в районі прикордонного знаку №0774/05", "Підгірне (України, в районі прикордонного знаку №0729/05");
        text = text.replace("№0763 на відстані 50 метрів до лінії державного кордону Україна Республіка Молдова на напрямку с. Нові Трояни м. Чадир Лунга", "№0740 на відстані 50 метрів до лінії державного кордону Україна Республіка Молдова на напрямку с. Нові Трояни м. Чадир Лунга");
        return text;
    }
}
