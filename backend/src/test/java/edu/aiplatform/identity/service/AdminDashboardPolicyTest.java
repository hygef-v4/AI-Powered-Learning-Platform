package edu.aiplatform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import edu.aiplatform.identity.contract.*;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminDashboardPolicyTest {
    @Test void dashboardReadsRequireCurrentActiveAdminWithoutAcademicScope() {
        AccountRepository accounts = mock(AccountRepository.class);
        ISubjectScope subjects = mock(ISubjectScope.class);
        IClassScope classes = mock(IClassScope.class);
        AuthorizationService service = new AuthorizationService(accounts, subjects, classes);
        UUID id = UUID.randomUUID();
        Account account = mock(Account.class);
        when(accounts.findById(id)).thenReturn(Optional.of(account));
        for (Role role : Role.values()) {
            for (AccountStatus status : AccountStatus.values()) {
                when(account.role()).thenReturn(role);
                when(account.status()).thenReturn(status);
                var decision = service.authorize(new Actor(id, Role.ADMIN), "ADMIN_DASHBOARD_VIEW", ResourceRef.none());
                assertThat(decision.allowed()).as("%s/%s", role, status)
                        .isEqualTo(role == Role.ADMIN && status == AccountStatus.ACTIVE);
            }
        }
        verifyNoInteractions(subjects, classes);
    }
}
