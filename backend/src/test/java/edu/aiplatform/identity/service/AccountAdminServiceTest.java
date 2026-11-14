package edu.aiplatform.identity.service;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
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
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.integration.OtpChallengeStore;
import edu.aiplatform.identity.integration.SessionStore;
import edu.aiplatform.identity.integration.VerificationTicketStore;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionOperations;

class AccountAdminServiceTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final ISubjectScope subjectScope = mock(ISubjectScope.class);
    private final IClassScope classScope = mock(IClassScope.class);
    private final SessionStore sessions = mock(SessionStore.class);
    private final OtpChallengeStore otp = mock(OtpChallengeStore.class);
    private final VerificationTicketStore tickets = mock(VerificationTicketStore.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private final IAudit audit = audits::add;
    private final ISubjectQuery subjectQuery = mock(ISubjectQuery.class);
    private final IClassAccess classAccess = mock(IClassAccess.class);
    private final AccountAdminService service = new AccountAdminService(accounts, authorization, subjectScope,
            classScope, provider(subjectQuery), provider(classAccess), sessions, otp, tickets, audit,
            TransactionOperations.withoutTransaction(), Clock.fixed(NOW, ZoneOffset.UTC));
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
    void dependencyFailureIsSystemBusyNotForbiddenAndNotAudited() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(AuthorizationDecision.deny(AuthorizationService.DEPENDENCY_UNAVAILABLE));

        assertThatThrownBy(() -> service.detail(admin, UUID.randomUUID()))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status()).isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.code()).isEqualTo("SYSTEM_BUSY");
                });
        assertThat(audits).isEmpty();
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
    void createRejectsDuplicateAndMalformedEmail() {
        when(accounts.findByEmail("taivm@fpt.edu.vn"))
                .thenReturn(Optional.of(IdentityTestData.account(Role.STUDENT, AccountStatus.ACTIVE, "h")));

        assertThatThrownBy(() -> service.create(admin, "taivm@fpt.edu.vn", "Tài", Role.TEACHER))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("EMAIL_EXISTS"));
        assertThatThrownBy(() -> service.create(admin, "a@gmail", "A", Role.TEACHER))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_EMAIL"));
        verify(accounts, never()).insert(any());
    }

    /** BR-U01-03: email thuộc tên miền nào cũng tạo được. */
    @Test
    void createAcceptsEmailFromAnyDomain() {
        when(accounts.findByEmail("a@gmail.com")).thenReturn(Optional.empty());

        AccountViews.Detail created = service.create(admin, "A@Gmail.com", "A", Role.STUDENT, "se000001");

        assertThat(created.email()).isEqualTo("a@gmail.com");
        verify(accounts).insert(any());
    }

    @Test
    void staleVersionIsConflict() {
        Account target = stored(Role.STUDENT, AccountStatus.ACTIVE, "h");
        when(accounts.updateAccountDetails(eq(target.id()), anyString(), any(), any(), eq(1L), any())).thenReturn(0);

        assertThatThrownBy(() -> service.update(admin, target.id(), "Tên mới", null, 1L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(AccountAdminService.VERSION_CONFLICT));
    }

    @Test
    void roleChangeRevokesSessionsAndAuditsBeforeAfter() {
        Account target = stored(Role.TEACHER, AccountStatus.ACTIVE, "h");
        when(accounts.updateRole(target.id(), Role.SUBJECT_MANAGER, 3L, NOW)).thenReturn(1);

        service.changeRole(admin, target.id(), Role.SUBJECT_MANAGER, 3L);

        verify(sessions, times(2)).deleteAll(target.id());
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("ROLE_CHANGED");
            assertThat(e.details()).containsEntry("from", "TEACHER").containsEntry("to", "SUBJECT_MANAGER");
        });
    }

    @Test
    void roleChangeLocksTheAccountThenChecksAssignmentsInsideTheTransaction() {
        Account target = stored(Role.SUBJECT_MANAGER, AccountStatus.ACTIVE, "h");
        boolean[] inTransaction = {false};
        TransactionOperations tracking = new TransactionOperations() {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                inTransaction[0] = true;
                try {
                    return action.doInTransaction(null);
                } finally {
                    inTransaction[0] = false;
                }
            }
        };
        AccountAdminService transactional = new AccountAdminService(accounts, authorization, subjectScope,
                classScope, provider(subjectQuery), provider(classAccess), sessions, otp, tickets, audit, tracking,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(subjectScope.listManagedSubjects(target.id())).thenAnswer(call -> {
            assertThat(inTransaction[0]).as("kiểm phân công trong transaction đổi role").isTrue();
            return List.of();
        });
        when(accounts.updateRole(target.id(), Role.TEACHER, 3L, NOW)).thenReturn(1);

        transactional.changeRole(admin, target.id(), Role.TEACHER, 3L);

        InOrder order = inOrder(accounts, subjectScope);
        order.verify(accounts).lockById(target.id());
        order.verify(subjectScope).listManagedSubjects(target.id());
        order.verify(accounts).updateRole(target.id(), Role.TEACHER, 3L, NOW);
    }

    @Test
    void roleChangeBlockedWhileStillAssigned() {
        Account teacher = stored(Role.SUBJECT_MANAGER, AccountStatus.ACTIVE, "h");
        UUID subject = UUID.randomUUID();
        UUID clazz = UUID.randomUUID();
        when(subjectScope.listManagedSubjects(teacher.id())).thenReturn(List.of(subject));
        when(classScope.listTaughtClasses(teacher.id())).thenReturn(List.of(clazz));
        when(subjectQuery.getSubject(subject)).thenReturn(Optional.of(
                new SubjectRef(subject, "SWR302", "Software Requirements", SubjectStatus.ACTIVE, teacher.id())));
        when(classAccess.getClassRef(clazz)).thenReturn(Optional.of(
                new ClassRef(clazz, subject, "SE1801", "Lớp SE1801", ClassStatus.OPEN, teacher.id(), false)));

        assertThatThrownBy(() -> service.changeRole(admin, teacher.id(), Role.TEACHER, 3L))
                .isInstanceOfSatisfying(RoleChangeBlockedException.class, e -> {
                    assertThat(e.managedSubjects()).containsExactly(subject);
                    assertThat(e.subjects()).containsExactly(
                            new RoleChangeBlockedException.Held(subject, "SWR302", "Software Requirements"));
                    assertThat(e.taughtClasses()).isEmpty();
                });
        assertThatThrownBy(() -> service.changeRole(admin, teacher.id(), Role.ADMIN, 3L))
                .isInstanceOfSatisfying(RoleChangeBlockedException.class, e -> {
                    assertThat(e.taughtClasses()).containsExactly(clazz);
                    assertThat(e.classes()).containsExactly(new RoleChangeBlockedException.Held(clazz, "SE1801", null));
                });
        verify(accounts, never()).updateRole(any(), any(), anyLong(), any());
    }

    @Test
    void roleChangeStaysBlockedWithIdsWhenSubjectOrClassLookupFails() {
        Account teacher = stored(Role.SUBJECT_MANAGER, AccountStatus.ACTIVE, "h");
        UUID subject = UUID.randomUUID();
        UUID clazz = UUID.randomUUID();
        when(subjectScope.listManagedSubjects(teacher.id())).thenReturn(List.of(subject));
        when(classScope.listTaughtClasses(teacher.id())).thenReturn(List.of(clazz));
        when(subjectQuery.getSubject(subject)).thenThrow(new IllegalStateException("U04 lỗi"));
        when(classAccess.getClassRef(clazz)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(admin, teacher.id(), Role.ADMIN, 3L))
                .isInstanceOfSatisfying(RoleChangeBlockedException.class, e -> {
                    assertThat(e.subjects()).containsExactly(new RoleChangeBlockedException.Held(subject, null, null));
                    assertThat(e.classes()).containsExactly(new RoleChangeBlockedException.Held(clazz, null, null));
                });
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
        verify(tickets).revokeAll(target.id());
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

    /** BR-U01-102, 103: MSSV bắt buộc và đúng định dạng với STUDENT, cấm với role khác. */
    @Test
    void createRequiresValidStudentCodeOnlyForStudents() {
        assertThatThrownBy(() -> service.create(admin, "sv@fpt.edu.vn", "SV", Role.STUDENT))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MISSING_STUDENT_CODE"));
        assertThatThrownBy(() -> service.create(admin, "sv@fpt.edu.vn", "SV", Role.STUDENT, "S1234567"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STUDENT_CODE"));
        assertThatThrownBy(() -> service.create(admin, "gv@fpt.edu.vn", "GV", Role.TEACHER, "SE000001"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("STUDENT_CODE_NOT_ALLOWED"));
        verify(accounts, never()).insert(any());
    }

    @Test
    void createStoresNormalizedCodeAndAuditsWithoutValue() {
        when(accounts.findByEmail("sv@fpt.edu.vn")).thenReturn(Optional.empty());

        AccountViews.Detail created = service.create(admin, "sv@fpt.edu.vn", "SV", Role.STUDENT, " se000001 ");

        assertThat(created.studentCode()).isEqualTo("SE000001");
        assertThat(created.studentCodeEditable()).isTrue();
        verify(accounts).insert(org.mockito.ArgumentMatchers.argThat(a -> "SE000001".equals(a.studentCode())));
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.details()).containsEntry("studentCodeSet", true);
            assertThat(e.details().toString()).doesNotContain("SE000001");
        });
    }

    /** BR-U01-101: mã đã có người dùng, kể cả khi hai request ghi cùng lúc. */
    @Test
    void createRejectsTakenStudentCode() {
        when(accounts.findByEmail(anyString())).thenReturn(Optional.empty());
        when(accounts.existingStudentCodes(List.of("SE000001"))).thenReturn(java.util.Set.of("SE000001"));
        assertThatThrownBy(() -> service.create(admin, "a@fpt.edu.vn", "A", Role.STUDENT, "SE000001"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STUDENT_CODE_EXISTS"));

        org.mockito.Mockito.doThrow(new org.springframework.dao.DuplicateKeyException(
                        "duplicate key value violates unique constraint \"accounts_student_code_key\""))
                .when(accounts).insert(any());
        assertThatThrownBy(() -> service.create(admin, "b@fpt.edu.vn", "B", Role.STUDENT, "SE000002"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STUDENT_CODE_EXISTS"));
    }

    private Account storedStudent(AccountStatus status, String studentCode) {
        Account base = IdentityTestData.account(Role.STUDENT, status, status == AccountStatus.PENDING ? null : "h");
        Account account = new Account(base.id(), base.schoolEmail(), base.role(), base.status(), base.passwordHash(),
                base.passwordChangedAt(), base.displayName(), base.phoneNumber(), 0, null, base.createdAt(),
                base.updatedAt(), base.version(), studentCode);
        when(accounts.findById(account.id())).thenReturn(Optional.of(account));
        return account;
    }

    /**
     * BR-U01-105: không gửi giữ nguyên; chỉ sửa khi PENDING; khóa khi đã kích hoạt, kể cả tài khoản cũ chưa có mã
     * (không có chức năng gán mã cho tài khoản cũ, người dùng chốt 2026-11-14).
     */
    @Test
    void updateAppliesStudentCodeRules() {
        when(accounts.updateAccountDetails(any(), anyString(), any(), any(), anyLong(), any())).thenReturn(1);
        Account legacy = storedStudent(AccountStatus.ACTIVE, null);
        Account pending = storedStudent(AccountStatus.PENDING, "SE000001");
        Account active = storedStudent(AccountStatus.ACTIVE, "SE000002");

        service.update(admin, active.id(), "Vũ Minh Tài", null, null, 3L);
        service.update(admin, active.id(), "Vũ Minh Tài", null, "se000002", 3L);
        verify(accounts, times(2)).updateAccountDetails(active.id(), "Vũ Minh Tài", null, "SE000002", 3L, NOW);
        service.update(admin, legacy.id(), "Vũ Minh Tài", null, "", 3L);
        verify(accounts).updateAccountDetails(legacy.id(), "Vũ Minh Tài", null, null, 3L, NOW);
        assertThatThrownBy(() -> service.update(admin, legacy.id(), "Vũ Minh Tài", null, "se000003", 3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STUDENT_CODE_LOCKED"));
        service.update(admin, pending.id(), "Vũ Minh Tài", null, "SE000004", 3L);
        verify(accounts).updateAccountDetails(pending.id(), "Vũ Minh Tài", null, "SE000004", 3L, NOW);
        assertThat(audits).last().satisfies(e -> assertThat(e.details()).containsEntry("fields", List.of("studentCode")));

        assertThatThrownBy(() -> service.update(admin, active.id(), "Vũ Minh Tài", null, "SE000009", 3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STUDENT_CODE_LOCKED"));
        assertThatThrownBy(() -> service.update(admin, active.id(), "Vũ Minh Tài", null, " ", 3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STUDENT_CODE"));
        assertThatThrownBy(() -> service.update(admin, pending.id(), "Vũ Minh Tài", null, "SE12", 3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STUDENT_CODE"));
        Account teacher = stored(Role.TEACHER, AccountStatus.ACTIVE, "h");
        assertThatThrownBy(() -> service.update(admin, teacher.id(), "GV", null, "SE000005", 3L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("STUDENT_CODE_NOT_ALLOWED"));
        when(accounts.existingStudentCodes(List.of("SE000006"))).thenReturn(java.util.Set.of("SE000006"));
        assertThatThrownBy(() -> service.update(admin, pending.id(), "Vũ Minh Tài", null, "SE000006", 3L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STUDENT_CODE_EXISTS"));
    }

    @Test
    void detailMarksStudentCodeEditableOnlyForPendingStudents() {
        assertThat(service.detail(admin, storedStudent(AccountStatus.ACTIVE, null).id()).studentCodeEditable()).isFalse();
        assertThat(service.detail(admin, storedStudent(AccountStatus.PENDING, "SE000001").id()).studentCodeEditable())
                .isTrue();
        assertThat(service.detail(admin, storedStudent(AccountStatus.ACTIVE, "SE000002").id()).studentCodeEditable())
                .isFalse();
        assertThat(service.detail(admin, stored(Role.TEACHER, AccountStatus.PENDING, null).id()).studentCodeEditable())
                .isFalse();
    }

    /** BR-U01-107: không đổi role vào hoặc ra khỏi STUDENT; xét trước các quy tắc khác và ghi audit từ chối. */
    @Test
    void roleChangeIntoOrOutOfStudentIsRejected() {
        Account student = storedStudent(AccountStatus.ACTIVE, "SE000001");
        Account teacher = stored(Role.TEACHER, AccountStatus.ACTIVE, "h");

        assertThatThrownBy(() -> service.changeRole(admin, student.id(), Role.TEACHER, 3L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("STUDENT_ROLE_CHANGE_NOT_ALLOWED"));
        assertThatThrownBy(() -> service.changeRole(admin, teacher.id(), Role.STUDENT, 3L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("STUDENT_ROLE_CHANGE_NOT_ALLOWED"));
        verify(accounts, never()).updateRole(any(), any(), anyLong(), any());
        verify(sessions, never()).deleteAll(any());
        assertThat(audits).extracting(AuditEvent::action).containsExactly("ROLE_CHANGE_REJECTED", "ROLE_CHANGE_REJECTED");
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(bean);
        return provider;
    }
}
