package edu.aiplatform.ai.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Chuẩn hóa chữ trước khi so quy tắc để kẻ tấn công không né được bằng chữ full-width, ký tự vô hình, bỏ dấu, leetspeak,
 * tách chữ ("i g n o r e"), mã URL hoặc thực thể HTML. Không đổi chữ gửi cho AI, chỉ dùng để so.
 */
final class PromptTextNormalizer {

    private PromptTextNormalizer() {
    }

    /** NFKC, bỏ ký tự định dạng vô hình (zero-width, soft hyphen), giữ xuống dòng và tab. */
    static String clean(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        StringBuilder out = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(cp -> {
            if (cp == '\n' || cp == '\t' || !(Character.getType(cp) == Character.FORMAT
                    || Character.getType(cp) == Character.CONTROL)) {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }

    /** Ký tự "tag" Unicode (U+E0000–E007F) hoặc ký tự đảo chiều bidi: dấu hiệu giấu lệnh, không có lý do dùng hợp lệ. */
    static boolean hasSmuggledCharacters(String text) {
        if (text == null) {
            return false;
        }
        return text.codePoints().anyMatch(cp -> cp >= 0xE0000 && cp <= 0xE007F
                || cp >= 0x202A && cp <= 0x202E || cp >= 0x2066 && cp <= 0x2069);
    }

    static int countCodePoints(String text) {
        return text == null ? 0 : text.codePointCount(0, text.length());
    }

    /** Chữ thường, bỏ dấu tiếng Việt ({@code đ} → {@code d}), gộp khoảng trắng; giữ xuống dòng. */
    static String fold(String text) {
        return fold(text, true);
    }

    static String fold(String text, boolean collapseSpaces) {
        String lower = clean(text).toLowerCase(Locale.ROOT).replace('đ', 'd');
        String stripped = Normalizer.normalize(lower, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(stripped.length());
        boolean space = false;
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            if (c == ' ' || c == '\t' || c == ' ') {
                if (!space || !collapseSpaces) {
                    out.append(' ');
                }
                space = true;
                continue;
            }
            space = false;
            out.append(c);
        }
        return out.toString();
    }

    /** Thay chữ số/ký hiệu leetspeak thông dụng bằng chữ cái. */
    static String deleet(String folded) {
        StringBuilder out = new StringBuilder(folded.length());
        for (int i = 0; i < folded.length(); i++) {
            char c = folded.charAt(i);
            out.append(switch (c) {
                case '0' -> 'o';
                case '1', '!', '|' -> 'i';
                case '3' -> 'e';
                case '4', '@' -> 'a';
                case '5', '$' -> 's';
                case '7' -> 't';
                default -> c;
            });
        }
        return out.toString();
    }

    /**
     * Gộp chữ bị tách từng ký tự ("i g n o r e" → "ignore"); nhiều khoảng trắng liền nhau là ranh giới từ, nên nhận
     * chữ chưa gộp khoảng trắng.
     */
    static String joinSpacedLetters(String foldedKeepingSpaces) {
        String[] words = foldedKeepingSpaces.split(" ", -1);
        StringBuilder out = new StringBuilder(foldedKeepingSpaces.length());
        StringBuilder run = new StringBuilder();
        int runLength = 0;
        for (String word : words) {
            if (word.isEmpty()) {
                flushRun(out, run, runLength);
                run.setLength(0);
                runLength = 0;
                continue;
            }
            if (word.length() == 1 && Character.isLetter(word.charAt(0))) {
                run.append(word);
                runLength++;
                continue;
            }
            flushRun(out, run, runLength);
            run.setLength(0);
            runLength = 0;
            appendWord(out, word);
        }
        flushRun(out, run, runLength);
        return out.toString();
    }

    private static void flushRun(StringBuilder out, StringBuilder run, int runLength) {
        if (runLength == 0) {
            return;
        }
        if (runLength >= 3) {
            appendWord(out, run.toString());
            return;
        }
        for (int i = 0; i < run.length(); i++) {
            appendWord(out, String.valueOf(run.charAt(i)));
        }
    }

    private static void appendWord(StringBuilder out, String word) {
        if (out.length() > 0) {
            out.append(' ');
        }
        out.append(word);
    }

    /** Bản giải mã URL ({@code %3C}) và thực thể HTML ({@code &lt;}, {@code &#60;}) để quét lại. */
    static String decodeMarkup(String text) {
        String decoded = text;
        if (text.indexOf('%') >= 0) {
            try {
                decoded = URLDecoder.decode(text.replace("+", "%2B"), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                decoded = text;
            }
        }
        if (decoded.indexOf('&') >= 0) {
            decoded = decodeEntities(decoded);
        }
        return decoded;
    }

    private static String decodeEntities(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            int end = c == '&' ? text.indexOf(';', i) : -1;
            if (end > i && end - i <= 10) {
                String entity = text.substring(i + 1, end).toLowerCase(Locale.ROOT);
                Integer cp = entityCodePoint(entity);
                if (cp != null) {
                    out.appendCodePoint(cp);
                    i = end + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static Integer entityCodePoint(String entity) {
        switch (entity) {
            case "lt":
                return (int) '<';
            case "gt":
                return (int) '>';
            case "quot":
                return (int) '"';
            case "apos":
                return (int) '\'';
            case "amp":
                return (int) '&';
            default:
                break;
        }
        try {
            if (entity.startsWith("#x")) {
                return Integer.parseInt(entity.substring(2), 16);
            }
            if (entity.startsWith("#")) {
                return Integer.parseInt(entity.substring(1));
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    /** Mọi biến thể dùng để so quy tắc (không trùng lặp, đã gập chữ). */
    static Set<String> matchVariants(String cleaned) {
        Set<String> variants = new LinkedHashSet<>();
        String folded = fold(cleaned);
        variants.add(folded);
        variants.add(joinSpacedLetters(fold(cleaned, false)));
        variants.add(deleet(folded));
        String decoded = decodeMarkup(cleaned);
        if (!decoded.equals(cleaned)) {
            variants.add(fold(decoded));
        }
        return variants;
    }
}
