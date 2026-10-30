package edu.aiplatform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final ISubjectScope subjectScope = mock(ISubjectScope.class);
    private final IClassScope classScope = mock(IClassScope.class);
    private final AuthorizationService service = new AuthorizationService(accounts, subjectScope, classScope);

    private Actor actor(Role role, AccountStatus status) {
        Account account = IdentityTestData.account(role, status, "h");
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));
        return new Actor(account.id(), role);
    }

    @Test
    void unknownActionIsDenied() {
        Actor admin = actor(Role.ADMIN, AccountStatus.ACTIVE);
        assertThat(service.authorize(admin, "ANY", ResourceRef.none()).allowed()).isFalse();
        assertThat(service.authorize(admin, null, ResourceRef.none()).allowed()).isFalse();
    }

    @Test
    void adminOnlyActionsFollowRole() {
        assertThat(service.authorize(actor(Role.ADMIN, AccountStatus.ACTIVE), Actions.SETTINGS_EDIT,
                ResourceRef.none()).allowed()).isTrue();
        assertThat(service.authorize(actor(Role.SUBJECT_MANAGER, AccountStatus.ACTIVE), Actions.ACCOUNT_LIST,
                ResourceRef.none()).allowed()).isFalse();
    }

    @Test
    void adminCannotUploadFiles() {
        assertThat(service.authorize(actor(Role.ADMIN, AccountStatus.ACTIVE), Actions.FILE_UPLOAD,
                ResourceRef.none()).allowed()).isFalse();
        assertThat(service.authorize(actor(Role.STUDENT, AccountStatus.ACTIVE), Actions.FILE_UPLOAD,
                ResourceRef.none()).allowed()).isTrue();
    }

    @Test
    void currentRoleFromDatabaseWinsOverActorRole() {
        Account demoted = IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(accounts.findById(demoted.id())).thenReturn(Optional.of(demoted));

        assertThat(service.authorize(new Actor(demoted.id(), Role.ADMIN), Actions.SETTINGS_VIEW,
                ResourceRef.none()).allowed()).isFalse();
    }

    @Test
    void inactiveOrMissingAccountIsDenied() {
        assertThat(service.authorize(actor(Role.ADMIN, AccountStatus.DISABLED), Actions.SETTINGS_VIEW,
                ResourceRef.none()).reason()).isEqualTo("ACCOUNT_NOT_ACTIVE");
        assertThat(service.authorize(new Actor(UUID.randomUUID(), Role.ADMIN), Actions.SETTINGS_VIEW,
                ResourceRef.none()).allowed()).isFalse();
        assertThat(service.authorize(null, Actions.SETTINGS_VIEW, ResourceRef.none()).allowed()).isFalse();
    }

    @Test
    void ownProfileOnly() {
        Actor student = actor(Role.STUDENT, AccountStatus.ACTIVE);
        assertThat(service.authorize(student, Actions.PROFILE_VIEW, ResourceRef.account(student.accountId()))
                .allowed()).isTrue();
        assertThat(service.authorize(student, Actions.PROFILE_VIEW, ResourceRef.account(UUID.randomUUID()))
                .allowed()).isFalse();
    }

    @Test
    void dependencyFailureDenies() {
        when(accounts.findById(any())).thenThrow(new IllegalStateException("db down"));

        assertThat(service.authorize(new Actor(UUID.randomUUID(), Role.ADMIN), Actions.SETTINGS_VIEW,
                ResourceRef.none()).reason()).isEqualTo("DEPENDENCY_UNAVAILABLE");
    }
}
