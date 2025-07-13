package noescape.downloader;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

public class TextExtractor {

    public static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&#(\\d+);");

    public static String extractText(String html) {
        String content = StringUtils.substringBetween(html, "<body>", "</body>");
        content = content
                .replaceAll("<[^>]*>", " ")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replaceAll("[\t ]+", " ")
                .trim();
        content = unescape(content);
        return content.lines().map(String::trim).filter(line -> !line.isEmpty()).collect(joining("\n"));
    }

    private static String unescape(String html) {
        var sb = new StringBuilder();
        var matcher = HTML_ENTITY_PATTERN.matcher(html);
        while (matcher.find()) {
            // Get the numeric code and convert it to a Unicode character
            int charCode = Integer.parseInt(matcher.group(1));
            matcher.appendReplacement(sb, Character.toString((char) charCode));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
