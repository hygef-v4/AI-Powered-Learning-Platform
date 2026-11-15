package edu.aiplatform.identity.contract;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Định dạng MSSV dùng chung cho các unit (BR-U01-100): đúng 2 chữ cái Latin và 6 chữ số, lưu chữ in hoa. U05 dùng để
 * nhận biết dòng ghi danh là MSSV (BR-U05-56); U01 là nơi kiểm cuối cùng.
 */
public final class StudentCodeFormat {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2}[0-9]{6}$");

    private StudentCodeFormat() {
    }

    /** Cắt khoảng trắng hai đầu, đổi in hoa; {@code null} khi rỗng. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.strip().toUpperCase(Locale.ROOT);
    }

    /** Nhận giá trị đã chuẩn hóa. */
    public static boolean isValid(String normalizedCode) {
        return normalizedCode != null && FORMAT.matcher(normalizedCode).matches();
    }
}
