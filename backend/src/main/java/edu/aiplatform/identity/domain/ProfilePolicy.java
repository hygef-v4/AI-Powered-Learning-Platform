package edu.aiplatform.identity.domain;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Ràng buộc hồ sơ (domain-entities U01 mục 4): tên hiển thị 1–150 ký tự sau khi cắt khoảng trắng; số điện thoại
 * tùy chọn, chỉ chữ số, có thể có {@code +} ở đầu, 8–15 chữ số.
 */
public final class ProfilePolicy {

    public static final int DISPLAY_NAME_MAX = 150;

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{8,15}$");

    private ProfilePolicy() {
    }

    public static String normalizeDisplayName(String raw) {
        return raw == null ? "" : raw.strip();
    }

    /** Rỗng sau khi cắt khoảng trắng nghĩa là không có số điện thoại. */
    public static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.strip();
    }

    /** Mã lỗi nếu không hợp lệ; nhận giá trị đã chuẩn hóa. */
    public static Optional<String> checkDisplayName(String displayName) {
        int length = displayName.codePointCount(0, displayName.length());
        return length < 1 || length > DISPLAY_NAME_MAX ? Optional.of("INVALID_DISPLAY_NAME") : Optional.empty();
    }

    public static Optional<String> checkPhone(String phoneNumber) {
        return phoneNumber == null || PHONE.matcher(phoneNumber).matches()
                ? Optional.empty() : Optional.of("INVALID_PHONE_NUMBER");
    }
}
