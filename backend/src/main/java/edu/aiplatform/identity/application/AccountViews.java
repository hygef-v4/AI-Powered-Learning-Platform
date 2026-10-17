package edu.aiplatform.identity.application;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Dữ liệu tài khoản trả cho Admin (UC 08, 10); không bao giờ có hash, OTP hay token. */
public final class AccountViews {

    private AccountViews() {
    }

    /** Một dòng Account List: tên, email, role, trạng thái, cập nhật lần cuối. */
    public record Summary(UUID id, String email, String displayName, Role role, AccountStatus status,
                          Instant updatedAt) {

        static Summary of(Account a) {
            return new Summary(a.id(), a.schoolEmail(), a.displayName(), a.role(), a.status(), a.updatedAt());
        }
    }

    public record Page(List<Summary> items, long total, int page, int size) {
    }

    /** Account Detail: thêm số điện thoại, thời điểm tạo và {@code version} để sửa (UC 10, 11). */
    public record Detail(UUID id, String email, String displayName, String phoneNumber, Role role,
                         AccountStatus status, Instant createdAt, Instant updatedAt, long version) {

        static Detail of(Account a) {
            return new Detail(a.id(), a.schoolEmail(), a.displayName(), a.phoneNumber(), a.role(), a.status(),
                    a.createdAt(), a.updatedAt(), a.version());
        }
    }
}
