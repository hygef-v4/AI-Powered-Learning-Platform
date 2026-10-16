package edu.aiplatform.identity.contract;

import java.util.UUID;

/** Thông tin liên hệ tối thiểu; không có mật khẩu hay số điện thoại. */
public record AccountContact(UUID accountId, String email, String displayName, Role role, AccountStatus status) {
}
