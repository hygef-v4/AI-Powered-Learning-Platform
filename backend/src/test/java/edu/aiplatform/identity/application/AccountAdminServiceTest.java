package edu.aiplatform.identity.application;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.infrastructure.OtpChallengeStore;
import edu.aiplatform.identity.infrastructure.SessionStore;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

class AccountAdminServiceTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final ISubjectScope subjectScope = mock(ISubjectScope.class);
    private final IClassScope classScope = mock(IClassScope.class);
    private final SessionStore sessions = mock(SessionStore.class);
    private final OtpChallengeStore otp = mock(OtpChallengeStore.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private final IAudit audit = audits::add;
    private final AccountAdminService service = new AccountAdminService(accounts, authorization, subjectScope,
            classScope, sessions, otp, audit, TransactionOperations.withoutTransaction(),
            IdentityTestData.properties(), Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
    }

    private Account stored(Role role, AccountStatus status, String hash) {
        Account account = IdentityTestData.account(role, status, hash);
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));
        return account;
    }

    @Test
    void nonAdminIsDeniedAndAudited() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("ROLE"));

        assertThatThrownBy(() -> service.detail(admin, UUID.randomUUID())).isInstanceOf(ApiException.class);
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("ACCESS_DENIED");
            assertThat(e.result()).isEqualTo(AuditResult.DENIED);
        });
    }

    @Test
    void createMakesPendingAccountWithoutPasswordOrMail() {
        when(accounts.findByEmail("gv01@fpt.edu.vn")).thenReturn(Optional.empty());

        AccountViews.Detail created = service.create(admin, " GV01@fpt.edu.vn ", " Giảng viên 01 ", Role.TEACHER);

        assertThat(created.status()).isEqualTo(AccountStatus.PENDING);
        assertThat(created.email()).isEqualTo("gv01@fpt.edu.vn");
        verify(accounts).insert(org.mockito.ArgumentMatchers.argThat(a -> a.passwordHash() == null));
        assertThat(audits).extracting(AuditEvent::action).containsExactly("ACCOUNT_CREATED");
    }

    @Test
    void createRejectsDuplicateAndForeignDomain() {
        when(accounts.findByEmail("taivm@fpt.edu.vn"))
                .thenReturn(Optional.of(IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h")));

        assertThatThrownBy(() -> service.create(admin, "taivm@fpt.edu.vn", "Tài", Role.STUDENT))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("EMAIL_EXISTS"));
        assertThatThrownBy(() -> service.create(admin, "a@gmail.com", "A", Role.STUDENT))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("EMAIL_DOMAIN_NOT_ALLOWED"));
        verify(accounts, never()).insert(any());
    }

    @Test
    void staleVersionIsConflict() {
        Account target = stored(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(accounts.updateProfile(eq(target.id()), anyString(), any(), eq(1L), any())).thenReturn(0);

        assertThatThrownBy(() -> service.update(admin, target.id(), "Tên mới", null, 1L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(AccountAdminService.VERSION_CONFLICT));
    }

    @Test
    void roleChangeRevokesSessionsAndAuditsBeforeAfter() {
        Account target = stored(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(accounts.updateRole(target.id(), Role.TEACHER, 3L, NOW)).thenReturn(1);

        service.changeRole(admin, target.id(), Role.TEACHER, 3L);

        verify(sessions, times(2)).deleteAll(target.id());
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("ROLE_CHANGED");
            assertThat(e.details()).containsEntry("from", "STUDENT").containsEntry("to", "TEACHER");
        });
    }

    @Test
    void roleChangeBlockedWhileStillAssigned() {
        Account teacher = stored(Role.SUBJECT_MANAGER, AccountStatus.ACTIVE, "h");
        UUID subject = UUID.randomUUID();
        UUID clazz = UUID.randomUUID();
        when(subjectScope.listManagedSubjects(teacher.id())).thenReturn(List.of(subject));
        when(classScope.listTaughtClasses(teacher.id())).thenReturn(List.of(clazz));

        assertThatThrownBy(() -> service.changeRole(admin, teacher.id(), Role.TEACHER, 3L))
                .isInstanceOfSatisfying(RoleChangeBlockedException.class, e -> {
                    assertThat(e.managedSubjects()).containsExactly(subject);
                    assertThat(e.taughtClasses()).isEmpty();
                });
        assertThatThrownBy(() -> service.changeRole(admin, teacher.id(), Role.ADMIN, 3L))
                .isInstanceOfSatisfying(RoleChangeBlockedException.class,
                        e -> assertThat(e.taughtClasses()).containsExactly(clazz));
        verify(accounts, never()).updateRole(any(), any(), anyLong(), any());
    }

    @Test
    void adminCannotChangeOwnRoleOrDisableSelf() {
        Account self = stored(Role.ADMIN, AccountStatus.ACTIVE, "h");
        Actor me = new Actor(self.id(), Role.ADMIN);

        assertThatThrownBy(() -> service.changeRole(me, self.id(), Role.TEACHER, 3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SELF_CHANGE_BLOCKED"));
        assertThatThrownBy(() -> service.changeStatus(me, self.id(), AccountStatus.DISABLED, 3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SELF_CHANGE_BLOCKED"));
    }

    @Test
    void lastActiveAdminIsProtected() {
        Account lastAdmin = stored(Role.ADMIN, AccountStatus.ACTIVE, "h");
        when(accounts.lockActiveAdmins()).thenReturn(List.of(lastAdmin.id()));

        assertThatThrownBy(() -> service.changeRole(admin, lastAdmin.id(), Role.TEACHER, 3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LAST_ADMIN"));
        assertThatThrownBy(() -> service.changeStatus(admin, lastAdmin.id(), AccountStatus.DISABLED, 3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LAST_ADMIN"));
    }

    @Test
    void disableRevokesSessionsAndOtp() {
        Account target = stored(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(accounts.updateStatus(target.id(), AccountStatus.DISABLED, 3L, NOW)).thenReturn(1);

        service.changeStatus(admin, target.id(), AccountStatus.DISABLED, 3L);

        verify(sessions, times(2)).deleteAll(target.id());
        verify(otp).deleteAll(target.id());
        assertThat(audits).extracting(AuditEvent::action).containsExactly("ACCOUNT_DISABLED");
    }

    @Test
    void disableReleasesSubjectManagerAssignmentsAndConflictSkipsIt() {
        Account manager = stored(Role.SUBJECT_MANAGER, AccountStatus.ACTIVE, "h");
        when(accounts.updateStatus(manager.id(), AccountStatus.DISABLED, 3L, NOW)).thenReturn(1);

        service.changeStatus(admin, manager.id(), AccountStatus.DISABLED, 3L);

        verify(subjectScope).releaseManagerAssignments(manager.id(), admin);

        Account stale = stored(Role.SUBJECT_MANAGER, AccountStatus.ACTIVE, "h");
        when(accounts.updateStatus(stale.id(), AccountStatus.DISABLED, 2L, NOW)).thenReturn(0);
        assertThatThrownBy(() -> service.changeStatus(admin, stale.id(), AccountStatus.DISABLED, 2L));
        verify(subjectScope, never()).releaseManagerAssignments(stale.id(), admin);
    }

    @Test
    void reopenGoesBackToPendingWhenNeverActivated() {
        Account neverActivated = stored(Role.STUDENT, AccountStatus.DISABLED, null);
        Account wasActive = stored(Role.STUDENT, AccountStatus.DISABLED, "h");
        when(accounts.updateStatus(any(), any(), anyLong(), any())).thenReturn(1);

        service.changeStatus(admin, neverActivated.id(), AccountStatus.ACTIVE, 3L);
        service.changeStatus(admin, wasActive.id(), AccountStatus.ACTIVE, 3L);

        verify(accounts).updateStatus(neverActivated.id(), AccountStatus.PENDING, 3L, NOW);
        verify(accounts).updateStatus(wasActive.id(), AccountStatus.ACTIVE, 3L, NOW);
        verify(sessions, never()).deleteAll(any());
        verify(subjectScope, never()).releaseManagerAssignments(any(), any());
    }
}
