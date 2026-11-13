package edu.aiplatform.ai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Phân tích chữ cho Tầng 2 bằng duyệt ký tự (không regex): tách phần trích dẫn khỏi phần chỉ dẫn, tách câu, tách từ.
 * Phần trích dẫn là chữ trong cặp ngoặc kép ({@code " "}, {@code “ ”}, {@code « »}), khối {@code ```} hoặc dòng bắt đầu
 * bằng {@code >}. Ngoặc mở không đóng thì phần sau vẫn tính là chỉ dẫn, để không giấu lệnh bằng một dấu ngoặc lẻ.
 */
final class PromptTextAnalysis {

    record Segment(String text, boolean quoted) {
    }

    /** Dòng từ có khoảng trắng hai đầu, so cụm từ bằng {@code contains(" cụm ")}. */
    record Tokens(String accented, String folded, boolean unaccentedInput) {

        boolean containsPhrase(String phrase) {
            Tokens wanted = tokenize(phrase);
            if (accented.contains(wanted.accented())) {
                return true;
            }
            return unaccentedInput && folded.contains(wanted.folded());
        }

        boolean isEmpty() {
            return accented.isBlank();
        }
    }

    private static final String FENCE = "```";

    private PromptTextAnalysis() {
    }

    static List<Segment> segments(String text) {
        List<Segment> out = new ArrayList<>();
        StringBuilder directive = new StringBuilder();
        int i = 0;
        boolean lineStart = true;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (text.startsWith(FENCE, i)) {
                int end = text.indexOf(FENCE, i + FENCE.length());
                if (end > 0) {
                    flush(out, directive);
                    out.add(new Segment(text.substring(i + FENCE.length(), end), true));
                    i = end + FENCE.length();
                    lineStart = false;
                    continue;
                }
            }
            if (lineStart && c == '>') {
                int end = text.indexOf('\n', i);
                end = end < 0 ? text.length() : end;
                flush(out, directive);
                out.add(new Segment(text.substring(i + 1, end), true));
                i = end;
                continue;
            }
            char close = closingQuote(c);
            if (close != 0) {
                int end = text.indexOf(close, i + 1);
                if (end > i) {
                    flush(out, directive);
                    out.add(new Segment(text.substring(i + 1, end), true));
                    i = end + 1;
                    lineStart = false;
                    continue;
                }
            }
            directive.append(c);
            lineStart = c == '\n' || lineStart && Character.isWhitespace(c);
            i++;
        }
        flush(out, directive);
        return out;
    }

    private static char closingQuote(char c) {
        switch (c) {
            case '"':
                return '"';
            case '“':
                return '”';
            case '«':
                return '»';
            default:
                return 0;
        }
    }

    private static void flush(List<Segment> out, StringBuilder directive) {
        if (directive.length() > 0) {
            out.add(new Segment(directive.toString(), false));
            directive.setLength(0);
        }
    }

    /** Ghép các phần chỉ dẫn, ngăn bằng xuống dòng để câu ở hai phía phần trích dẫn không dính nhau. */
    static String directiveText(List<Segment> segments) {
        StringBuilder out = new StringBuilder();
        for (Segment segment : segments) {
            if (!segment.quoted()) {
                out.append(segment.text()).append('\n');
            }
        }
        return out.toString();
    }

    static List<String> sentences(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean decimalPoint = c == '.' && i > 0 && i + 1 < text.length()
                    && Character.isDigit(text.charAt(i - 1)) && Character.isDigit(text.charAt(i + 1));
            if (!decimalPoint && (c == '.' || c == '!' || c == '?' || c == ';' || c == '\n')) {
                addSentence(out, current);
                continue;
            }
            current.append(c);
        }
        addSentence(out, current);
        return out;
    }

    private static void addSentence(List<String> out, StringBuilder current) {
        String sentence = current.toString().strip();
        if (!sentence.isEmpty()) {
            out.add(sentence);
        }
        current.setLength(0);
    }

    static Tokens tokenize(String text) {
        String accented = tokenLine(Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT));
        String folded = tokenLine(PromptTextNormalizer.fold(text == null ? "" : text));
        return new Tokens(accented, folded, accented.equals(folded));
    }

    private static String tokenLine(String text) {
        StringBuilder out = new StringBuilder(text.length() + 2).append(' ');
        boolean inWord = false;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            if (Character.isLetterOrDigit(cp) || Character.getType(cp) == Character.NON_SPACING_MARK) {
                out.appendCodePoint(cp);
                inWord = true;
            } else if (inWord) {
                out.append(' ');
                inWord = false;
            }
            i += Character.charCount(cp);
        }
        if (inWord) {
            out.append(' ');
        }
        return out.toString();
    }
}
