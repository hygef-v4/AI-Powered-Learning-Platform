package edu.aiplatform.identity.service;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.model.Account;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Dữ liệu tài khoản trả cho Admin (UC 08, 10); không bao giờ có hash, OTP hay token. */
public final class AccountViews {

    private AccountViews() {
    }

    /** Một dòng Account List (design a02): tên, email, MSSV, role, trạng thái, ngày tạo. */
    public record Summary(UUID id, String email, String displayName, Role role, AccountStatus status,
                          Instant createdAt, Instant updatedAt, String studentCode) {

        static Summary of(Account a) {
            return new Summary(a.id(), a.schoolEmail(), a.displayName(), a.role(), a.status(), a.createdAt(),
                    a.updatedAt(), a.studentCode());
        }
    }

    public record Page(List<Summary> items, long total, int page, int size) {
    }

    /**
     * Account Detail: thêm số điện thoại, MSSV, thời điểm tạo và {@code version} để sửa (UC 10, 11).
     * {@code studentCodeEditable}: tài khoản sinh viên còn {@code PENDING} (BR-U01-105).
     */
    public record Detail(UUID id, String email, String displayName, String phoneNumber, Role role,
                         AccountStatus status, Instant createdAt, Instant updatedAt, long version,
                         String studentCode, boolean studentCodeEditable) {

        static Detail of(Account a) {
            boolean editable = a.role() == Role.STUDENT && a.status() == AccountStatus.PENDING;
            return new Detail(a.id(), a.schoolEmail(), a.displayName(), a.phoneNumber(), a.role(), a.status(),
                    a.createdAt(), a.updatedAt(), a.version(), a.studentCode(), editable);
        }
    }
}
