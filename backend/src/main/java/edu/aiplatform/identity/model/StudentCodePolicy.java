package edu.aiplatform.identity.model;

import edu.aiplatform.identity.contract.StudentCodeFormat;

/**
 * Mã số sinh viên (BR-U01-100): đúng 2 chữ cái Latin và 6 chữ số, lưu ở dạng chữ in hoa. Chuẩn hóa chỉ cắt khoảng
 * trắng hai đầu và đổi sang in hoa; khoảng trắng hay ký tự khác ở giữa làm mã sai định dạng. Định dạng nằm ở
 * {@link StudentCodeFormat} để unit khác dùng qua contract.
 */
public final class StudentCodePolicy {

    private StudentCodePolicy() {
    }

    /** {@code null} khi đầu vào rỗng sau khi cắt khoảng trắng. */
    public static String normalize(String raw) {
        return StudentCodeFormat.normalize(raw);
    }

    /** Nhận giá trị đã chuẩn hóa. */
    public static boolean isValid(String normalizedCode) {
        return StudentCodeFormat.isValid(normalizedCode);
    }

    public static boolean looksLikeStudentCode(String raw) {
        return isValid(normalize(raw));
    }
}
