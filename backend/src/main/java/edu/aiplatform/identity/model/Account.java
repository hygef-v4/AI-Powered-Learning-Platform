package edu.aiplatform.identity.model;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng {@code accounts} (domain-entities U01 mục 2–5, 15). {@code passwordHash} rỗng khi chưa kích hoạt;
 * {@code failedLoginCount}, {@code lockedUntil} là khóa tạm do nhập sai, không phải trạng thái (không có LOCKED).
 * {@code studentCode} chỉ có ở tài khoản {@code STUDENT}, có thể trống với tài khoản cũ (BR-U01-102, 104).
 */
public record Account(UUID id, String schoolEmail, Role role, AccountStatus status, String passwordHash,
                      Instant passwordChangedAt, String displayName, String phoneNumber, int failedLoginCount,
                      Instant lockedUntil, Instant createdAt, Instant updatedAt, long version, String studentCode) {

    /** Tài khoản không có MSSV; giữ cho chỗ gọi có trước khi thêm MSSV. */
    public Account(UUID id, String schoolEmail, Role role, AccountStatus status, String passwordHash,
                   Instant passwordChangedAt, String displayName, String phoneNumber, int failedLoginCount,
                   Instant lockedUntil, Instant createdAt, Instant updatedAt, long version) {
        this(id, schoolEmail, role, status, passwordHash, passwordChangedAt, displayName, phoneNumber,
                failedLoginCount, lockedUntil, createdAt, updatedAt, version, null);
    }

    /** Tài khoản do Admin tạo hoặc nhập: {@code PENDING}, không mật khẩu (BR-U01-10). */
    public static Account newPending(UUID id, String schoolEmail, Role role, String displayName, String phoneNumber,
                                     Instant now) {
        return newPending(id, schoolEmail, role, displayName, phoneNumber, null, now);
    }

    /** Như {@link #newPending(UUID, String, Role, String, String, Instant)} kèm MSSV đã chuẩn hóa (BR-U01-103). */
    public static Account newPending(UUID id, String schoolEmail, Role role, String displayName, String phoneNumber,
                                     String studentCode, Instant now) {
        return new Account(id, schoolEmail, role, AccountStatus.PENDING, null, null, displayName, phoneNumber, 0,
                null, now, now, 1, studentCode);
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public boolean hasStudentCode() {
        return studentCode != null;
    }

    /** Khóa tạm còn hiệu lực; hết hạn thì tự mở, không cần Admin (BR-U01-41, 42). */
    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** Mở lại: về {@code ACTIVE} nếu đã có mật khẩu, về {@code PENDING} nếu chưa (BR-U01-72). */
    public AccountStatus statusAfterReopen() {
        return hasPassword() ? AccountStatus.ACTIVE : AccountStatus.PENDING;
    }
}
