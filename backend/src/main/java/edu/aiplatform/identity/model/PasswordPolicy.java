package edu.aiplatform.identity.model;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Chính sách mật khẩu (BR-U01-30, 31; NFR-U01-14): tối thiểu 8 ký tự, có chữ cái và chữ số, không chứa phần tên
 * trong email, tối đa 72 byte UTF-8 (giới hạn của bcrypt, từ chối thay vì cắt ngầm). Không kiểm mật khẩu bị lộ
 * và không kiểm trùng mật khẩu cũ.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;
    /** Phần tên email ngắn hơn mức này (ví dụ "a") không đủ ý nghĩa để chặn. */
    static final int MIN_EMAIL_NAME_LENGTH = 3;

    private PasswordPolicy() {
    }

    /** Trả các vi phạm; rỗng là hợp lệ. {@code schoolEmail} đã chuẩn hóa. */
    public static Set<Violation> check(String password, String schoolEmail) {
        Set<Violation> violations = EnumSet.noneOf(Violation.class);
        String value = password == null ? "" : password;
        if (value.codePointCount(0, value.length()) < MIN_LENGTH) {
            violations.add(Violation.TOO_SHORT);
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            violations.add(Violation.TOO_LONG);
        }
        if (value.codePoints().noneMatch(Character::isLetter)) {
            violations.add(Violation.NEED_LETTER);
        }
        if (value.chars().noneMatch(c -> c >= '0' && c <= '9')) {
            violations.add(Violation.NEED_DIGIT);
        }
        String name = schoolEmail == null ? "" : EmailPolicy.localPart(schoolEmail);
        if (name.length() >= MIN_EMAIL_NAME_LENGTH && value.toLowerCase(Locale.ROOT).contains(name)) {
            violations.add(Violation.CONTAINS_EMAIL_NAME);
        }
        return violations;
    }

    public enum Violation {
        TOO_SHORT, TOO_LONG, NEED_LETTER, NEED_DIGIT, CONTAINS_EMAIL_NAME
    }
}
