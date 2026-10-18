package edu.aiplatform.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccountLookupAndPolicyTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AccountLookupService lookup = new AccountLookupService(accounts);

    @Test
    void studentsAreActiveOnlyAndAtMostTwenty() {
        Account student = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "hash");
        when(accounts.findForLookup("tai", Set.of(Role.STUDENT), AccountStatus.ACTIVE, 20)).thenReturn(List.of(student));

        List<AccountContact> found = lookup.findStudents("tai", 500);

        assertThat(found).singleElement().satisfies(c -> {
            assertThat(c.email()).isEqualTo("taivm@fpt.edu.vn");
            assertThat(c.status()).isEqualTo(AccountStatus.ACTIVE);
        });
    }

    @Test
    void emptyRolesFindNothing() {
        lookup.findAccounts("a", null, 5);
        verify(accounts).findForLookup(eq("a"), eq(Set.of()), eq(null), anyInt());
    }

    @Test
    void emailsAreNormalizedBeforeLookup() {
        lookup.findByEmails(List.of(" TaiVM@FPT.edu.vn", "taivm@fpt.edu.vn", " "));
        verify(accounts).findByEmails(List.of("taivm@fpt.edu.vn"));

        assertThat(lookup.findByEmails(List.of())).isEmpty();
        verify(accounts, never()).findForLookup(anyString(), any(), any(), anyInt());
    }

    /** Mọi action khai báo cho unit khác phải có dòng trong bảng quyền, nếu không sẽ bị từ chối mặc định (Q14). */
    @Test
    void everyDeclaredActionHasAPolicyRule() throws IllegalAccessException {
        for (Field field : Actions.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                String action = (String) field.get(null);
                assertThat(AccessPolicy.ruleFor(action)).as(action).isPresent();
            }
        }
    }

    @Test
    void adminActionsAreAdminOnlyAndUploadExcludesAdmin() {
        Arrays.stream(Actions.class.getDeclaredFields())
                .filter(f -> f.getName().startsWith("ACCOUNT_") || f.getName().startsWith("SETTINGS_")
                        || f.getName().startsWith("SUBJECT_")
                        || f.getName().equals("AUDIT_READ"))
                .forEach(f -> assertThat(AccessPolicy.ruleFor(readName(f)).orElseThrow().roles())
                        .containsExactly(Role.ADMIN));
        assertThat(AccessPolicy.ruleFor(Actions.FILE_UPLOAD).orElseThrow().roles()).doesNotContain(Role.ADMIN);
    }

    private static String readName(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
