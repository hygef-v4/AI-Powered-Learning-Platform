package edu.aiplatform.identity.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.identity.model.*;
import edu.aiplatform.identity.repository.*;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
class NotificationAccountLookupTest {
    @Test void adminLookupDoesNotInherit20ResultSearchLimit(){var repository=mock(AccountRepository.class);var rows=IntStream.range(0,35).mapToObj(i->{var a=mock(Account.class);when(a.id()).thenReturn(new UUID(0,i+1));when(a.role()).thenReturn(Role.ADMIN);when(a.status()).thenReturn(AccountStatus.ACTIVE);return a;}).toList();when(repository.listActiveAdmins(null,100)).thenReturn(rows);var service=new AccountLookupService(repository);assertThat(service.listActiveAdmins(null,1000)).hasSize(35);verify(repository).listActiveAdmins(null,100);}
    @Test void cursorForwardedAndExistingSearchLimitUntouched(){var repository=mock(AccountRepository.class);var service=new AccountLookupService(repository);var cursor=UUID.randomUUID();service.listActiveAdmins(cursor,0);verify(repository).listActiveAdmins(cursor,1);assertThat(AccountLookupService.MAX_RESULTS).isEqualTo(20);}
    @Test void announcementRulesDenyAdminAndAllowOnlyClassScopedStaffManagement(){var manage=AccessPolicy.ruleFor(Actions.ANNOUNCEMENT_MANAGE).orElseThrow();assertThat(manage.roles()).containsExactlyInAnyOrder(Role.TEACHER,Role.SUBJECT_MANAGER);assertThat(manage.scope()).isEqualTo(ScopeKind.CLASS);assertThat(AccessPolicy.ruleFor(Actions.NOTIFICATION_SELF).orElseThrow().scope()).isEqualTo(ScopeKind.ACCOUNT);}
}
