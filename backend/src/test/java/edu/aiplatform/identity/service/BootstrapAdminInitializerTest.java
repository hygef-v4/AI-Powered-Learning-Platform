package edu.aiplatform.identity.service;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.config.IdentityProperties;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.repository.AccountRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class BootstrapAdminInitializerTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private final IAudit audit = audits::add;

    private void run(String email) {
        IdentityProperties base = IdentityTestData.properties();
        IdentityProperties properties = new IdentityProperties(base.jwtSecret(), base.accessTtl(),
                base.refreshIdleTtl(), base.refreshMaxTtl(), base.bcryptCost(), base.otpTtl(), base.otpMaxAttempts(),
                base.loginLockThreshold(), base.loginLockDuration(), base.cookieSecure(), base.mailFrom(), email,
                base.bootstrapAdminName(), base.rateLimit());
        new BootstrapAdminInitializer(accounts, audit, properties, Clock.fixed(NOW, ZoneOffset.UTC))
                .run(new DefaultApplicationArguments());
    }

    /** BR-U01-03: Admin đầu tiên dùng email thuộc tên miền nào cũng được. */
    @Test
    void createsPendingAdminForAnyEmailDomain() {
        run(" Admin@Gmail.com ");

        verify(accounts).insert(org.mockito.ArgumentMatchers.argThat(a -> a.schoolEmail().equals("admin@gmail.com")
                && a.role() == Role.ADMIN && a.status() == AccountStatus.PENDING && a.passwordHash() == null));
        assertThat(audits).extracting(AuditEvent::action).containsExactly("ACCOUNT_CREATED");
    }

    @Test
    void skipsMalformedEmailOrExistingAdmin() {
        run("admin@gmail");
        when(accounts.anyAdminExists()).thenReturn(true);
        run("admin@gmail.com");

        verify(accounts, never()).insert(any());
        assertThat(audits).isEmpty();
    }
}
