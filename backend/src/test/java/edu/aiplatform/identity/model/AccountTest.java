package edu.aiplatform.identity.model;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final Instant NOW = Instant.parse("2026-10-17T08:00:00Z");

    private static Account pending() {
        return Account.newPending(UUID.randomUUID(), "taivm@fpt.edu.vn", Role.STUDENT, "Tài", null, NOW);
    }

    private static Account withPasswordAndLock(Instant lockedUntil) {
        Account a = pending();
        return new Account(a.id(), a.schoolEmail(), a.role(), AccountStatus.DISABLED, "$2a$12$hash", NOW,
                a.displayName(), null, 0, lockedUntil, NOW, NOW, 2);
    }

    @Test
    void newAccountIsPendingWithoutPassword() {
        Account account = pending();
        assertThat(account.status()).isEqualTo(AccountStatus.PENDING);
        assertThat(account.hasPassword()).isFalse();
        assertThat(account.version()).isEqualTo(1);
    }

    @Test
    void reopenReturnsToActiveOnlyWhenPasswordExists() {
        assertThat(pending().statusAfterReopen()).isEqualTo(AccountStatus.PENDING);
        assertThat(withPasswordAndLock(null).statusAfterReopen()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void temporaryLockExpiresByItself() {
        Account locked = withPasswordAndLock(NOW.plusSeconds(60));
        assertThat(locked.isLocked(NOW)).isTrue();
        assertThat(locked.isLocked(NOW.plusSeconds(60))).isFalse();
        assertThat(withPasswordAndLock(null).isLocked(NOW)).isFalse();
    }
}
