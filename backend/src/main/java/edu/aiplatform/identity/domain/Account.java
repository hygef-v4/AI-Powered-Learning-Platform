package edu.aiplatform.identity.domain;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng {@code accounts} (domain-entities U01 mục 2–5). {@code passwordHash} rỗng khi chưa kích hoạt;
 * {@code failedLoginCount}, {@code lockedUntil} là khóa tạm do nhập sai, không phải trạng thái (không có LOCKED).
 */
public record Account(UUID id, String schoolEmail, Role role, AccountStatus status, String passwordHash,
                      Instant passwordChangedAt, String displayName, String phoneNumber, int failedLoginCount,
                      Instant lockedUntil, Instant createdAt, Instant updatedAt, long version) {

    /** Tài khoản do Admin tạo hoặc nhập: {@code PENDING}, không mật khẩu (BR-U01-10). */
    public static Account newPending(UUID id, String schoolEmail, Role role, String displayName, String phoneNumber,
                                     Instant now) {
        return new Account(id, schoolEmail, role, AccountStatus.PENDING, null, null, displayName, phoneNumber, 0,
                null, now, now, 1);
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
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
