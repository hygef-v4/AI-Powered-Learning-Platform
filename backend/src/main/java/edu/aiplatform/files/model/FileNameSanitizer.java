package edu.aiplatform.files.model;

/** Làm sạch tên tệp gốc: bỏ đường dẫn, ký tự điều khiển, cắt còn 255 ký tự (BR-U03-08). */
public final class FileNameSanitizer {

    static final int MAX_LENGTH = 255;
    static final String FALLBACK = "tep";

    private FileNameSanitizer() {
    }

    public static String sanitize(String name) {
        if (name == null) {
            return FALLBACK;
        }
        String base = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        String cleaned = base.codePoints()
                .filter(cp -> !Character.isISOControl(cp))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString().strip();
        if (cleaned.isEmpty() || cleaned.equals(".") || cleaned.equals("..")) {
            return FALLBACK;
        }
        if (cleaned.length() > MAX_LENGTH) {
            int end = Character.isHighSurrogate(cleaned.charAt(MAX_LENGTH - 1)) ? MAX_LENGTH - 1 : MAX_LENGTH;
            cleaned = cleaned.substring(0, end);
        }
        return cleaned;
    }
}
