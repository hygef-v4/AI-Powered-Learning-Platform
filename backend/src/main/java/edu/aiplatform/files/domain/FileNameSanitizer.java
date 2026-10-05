package edu.aiplatform.files.domain;

/** Làm sạch tên tệp gốc: bỏ đường dẫn, ký tự điều khiển, cắt còn 255 ký tự (BR-U03-08). */
public final class FileNameSanitizer {

    public static final int MAX_LENGTH = 255;
    static final String FALLBACK = "file";

    private FileNameSanitizer() {}

    public static String sanitize(String name) {
        if (name == null) {
            return FALLBACK;
        }
        String base = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        String cleaned = base.replaceAll("\\p{Cntrl}", "").strip();
        if (cleaned.isEmpty() || cleaned.equals(".") || cleaned.equals("..")) {
            return FALLBACK;
        }
        if (cleaned.codePointCount(0, cleaned.length()) > MAX_LENGTH) {
            cleaned = cleaned.substring(0, cleaned.offsetByCodePoints(0, MAX_LENGTH));
        }
        return cleaned;
    }
}
