package noescape.downloader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static java.lang.Character.*;

public class Tokenizer implements Iterator<String> {

    private final String text;
    private int start = 0;
    private int end = 0;

    public Tokenizer(String text) {
        this.text = text;
        advance();
    }

    public static String tokenizeToString(String text) {
        var sb = new StringBuilder();
        new Tokenizer(text).forEachRemaining((token -> sb.append(token).append(' ')));
        if (sb.charAt(sb.length() - 1) == ' ') {
            return sb.substring(0, sb.length() - 1); // Trim last space
        }
        return sb.toString();
    }

    public static List<String> tokenize(String text) {
        var tokens = new ArrayList<String>();
        new Tokenizer(text).forEachRemaining(tokens::add);
        return tokens;
    }

    @Override
    public boolean hasNext() {
        return start < text.length();
    }

    @Override
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var next = text.substring(start, end);
        advance();
        return next;
    }

    private void advance() {
        int start = this.end;
        int end = start;
        char ch = ' ';
        for (; end < text.length(); end++) {
            char prev = ch;
            ch = text.charAt(end);
            if (isWhitespace(ch)) {
                if (isWhitespace(prev)) {
                    start++;
                    continue;
                } else {
                    break;
                }
            }

            if (isLetter(ch) && isDigit(prev) || isDigit(ch) && isLetter(prev)) {
                break;
            }

            if (isLetter(prev) && ch == '_') {
                continue;
            }

            if (prev == '_' && isDigit(ch)) {
                continue;
            }

            if (isLetterOrDigit(ch)) {
                continue;
            }

            if ((ch == '.' || ch == ':' || ch == '/') && isNumber(start, end)) {
                int numEnd = findNumberEnd(ch, end);
                if (numEnd != end) {
                    end = numEnd;
                    break;
                }
            }

            if (!isLetterOrDigit(prev)) {
                end++;
            }
            break;
        }
        this.start = start;
        this.end = end;
    }

    private int findNumberEnd(char separator, int end) {
        while (end < text.length() - 2 && text.charAt(end) == separator && isDigit(text.charAt(end + 1))) {
            end += 2;
            while (end < text.length() && isDigit(text.charAt(end))) {
                end++;
            }
        }
        return end;
    }

    private boolean isNumber(int start, int end) {
        if (start == end) {
            return false;
        }
        for (int i = start; i < end; i++) {
            if (!isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
