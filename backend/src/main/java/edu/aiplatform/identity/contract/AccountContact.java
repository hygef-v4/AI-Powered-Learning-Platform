package edu.aiplatform.identity.contract;

import java.util.UUID;

/**
 * Thông tin liên hệ tối thiểu; không có mật khẩu hay số điện thoại. {@code studentCode} là MSSV của tài khoản
 * {@code STUDENT}, rỗng khi chưa có; chỉ hiện trên màn vốn đã hiện người đó, không đưa vào AI, thông báo hay email
 * (BR-U01-113).
 */
public record AccountContact(UUID accountId, String email, String displayName, Role role, AccountStatus status,
                             String studentCode) {

    /** Liên hệ không có MSSV. */
    public AccountContact(UUID accountId, String email, String displayName, Role role, AccountStatus status) {
        this(accountId, email, displayName, role, status, null);
    }
}
