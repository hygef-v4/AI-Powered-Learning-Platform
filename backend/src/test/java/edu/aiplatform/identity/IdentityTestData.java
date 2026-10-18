package edu.aiplatform.identity;

import edu.aiplatform.identity.application.IdentityProperties;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Dữ liệu dùng chung cho test U01. */
public final class IdentityTestData {

    public static final Instant NOW = Instant.parse("2026-10-17T08:00:00Z");
    public static final String SECRET = "0123456789abcdef0123456789abcdef";

    private IdentityTestData() {
    }

    public static IdentityProperties properties() {
        return new IdentityProperties(List.of("fpt.edu.vn"), SECRET, Duration.ofMinutes(15), Duration.ofHours(2),
                Duration.ofDays(7), 4, Duration.ofMinutes(10), 5, 5, Duration.ofMinutes(15), true,
                "no-reply@fpt.edu.vn", "", "Quản trị viên",
                new IdentityProperties.RateLimit(5, Duration.ofSeconds(60), 20, 30, Duration.ofMinutes(5)));
    }

    public static Account account(Role role, AccountStatus status, String passwordHash) {
        return new Account(UUID.randomUUID(), "taivm@fpt.edu.vn", role, status, passwordHash,
                passwordHash == null ? null : NOW, "Vũ Minh Tài", null, 0, null, NOW, NOW, 3);
    }

    public static Account locked(Account a, Instant until) {
        return new Account(a.id(), a.schoolEmail(), a.role(), a.status(), a.passwordHash(), a.passwordChangedAt(),
                a.displayName(), a.phoneNumber(), 0, until, a.createdAt(), a.updatedAt(), a.version());
    }
}
