package edu.aiplatform.identity.model;

import edu.aiplatform.identity.contract.AccountStatus;

/**
 * Mục đích OTP (domain-entities U01 mục 8). Mỗi mục đích chỉ dùng cho tài khoản ở đúng trạng thái: kích hoạt cần
 * {@code PENDING}, đặt lại mật khẩu cần {@code ACTIVE}. OTP của mục đích này không dùng được cho mục đích kia (BR-U01-26).
 */
public enum OtpPurpose {
    ACTIVATION(AccountStatus.PENDING),
    PASSWORD_RESET(AccountStatus.ACTIVE);

    private final AccountStatus requiredStatus;

    OtpPurpose(AccountStatus requiredStatus) {
        this.requiredStatus = requiredStatus;
    }

    public boolean appliesTo(Account account) {
        return account.status() == requiredStatus;
    }
}
