package edu.aiplatform.identity.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuẩn hóa và kiểm email đăng nhập (BR-U01-02, 03). Chỉ kiểm định dạng và độ dài, chấp nhận mọi tên miền; tài khoản vẫn
 * do Admin tạo và chỉ kích hoạt được bằng OTP gửi tới chính email đó.
 */
public final class EmailPolicy {

    public static final int MAX_LENGTH = 254;

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailPolicy() {
    }

    /** Cắt khoảng trắng và đổi chữ thường trước mọi so sánh (BR-U01-02). */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
    }

    /** Phần trước {@code @}, dùng cho BR-U01-31. */
    public static String localPart(String email) {
        int at = email.indexOf('@');
        return at < 0 ? email : email.substring(0, at);
    }

    /** {@code true} khi email đã chuẩn hóa không rỗng, tối đa {@value #MAX_LENGTH} ký tự và đúng dạng {@code a@b.c}. */
    public static boolean isValid(String normalizedEmail) {
        return !normalizedEmail.isEmpty() && normalizedEmail.length() <= MAX_LENGTH
                && FORMAT.matcher(normalizedEmail).matches();
    }
}
