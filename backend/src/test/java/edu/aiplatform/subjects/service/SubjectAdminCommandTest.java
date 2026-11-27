package edu.aiplatform.subjects.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class SubjectAdminCommandTest {
    private static final Instant NOW = Instant.parse("2026-10-18T08:00:00Z");
    private final UUID subjectId = UUID.randomUUID(),
            oldManager = UUID.randomUUID(),
            newManager = UUID.randomUUID();
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    private final ISubjectRepository subjectRepository = mock(ISubjectRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAccountLookup accountLookup = mock(IAccountLookup.class);
    private final IAudit audit = mock(IAudit.class);
    private final SubjectCommandService subjectCommandService =
            new SubjectCommandService(
                    subjectRepository,
                    authorization,
                    accountLookup,
                    new SubjectAuditRecorder(audit),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setup() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(AuthorizationDecision.allow());
        when(subjectRepository.updateSubject(any(), anyLong())).thenReturn(true);
    }

    @Test
    void anonymousCannotExecuteAnyCommand() {
        code(() -> subjectCommandService.createSubject(null, null), "AUTHENTICATION_REQUIRED");
        code(
                () -> subjectCommandService.updateInformation(null, subjectId, null),
                "AUTHENTICATION_REQUIRED");
        code(
                () -> subjectCommandService.changeManager(null, subjectId, null),
                "AUTHENTICATION_REQUIRED");
        verifyNoInteractions(subjectRepository, accountLookup);
        verify(audit, times(3)).record(argThat(event -> event.result() == AuditResult.DENIED));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void nonAdminCannotExecuteAnyCommand(Role role) {
        var actor = new Actor(UUID.randomUUID(), role);
        code(() -> subjectCommandService.createSubject(actor, null), "SUBJECT_FORBIDDEN");
        code(
                () -> subjectCommandService.updateInformation(actor, subjectId, null),
                "SUBJECT_FORBIDDEN");
        code(
                () -> subjectCommandService.changeManager(actor, subjectId, null),
                "SUBJECT_FORBIDDEN");
        verifyNoInteractions(subjectRepository, accountLookup);
        verify(audit, times(3)).record(argThat(event -> event.result() == AuditResult.DENIED));
    }

    @Test
    void deniedAndUnavailableAuthorizationNeverWrite() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(AuthorizationDecision.deny("DISABLED"));
        code(() -> subjectCommandService.createSubject(admin, null), "SUBJECT_FORBIDDEN");
        doThrow(new IllegalStateException("down"))
                .when(authorization)
                .authorize(any(), anyString(), any());
        code(
                () -> subjectCommandService.changeManager(admin, subjectId, null),
                "SUBJECT_AUTHORIZATION_UNAVAILABLE");
        verifyNoInteractions(subjectRepository, accountLookup);
        verify(audit, times(2)).record(argThat(event -> event.result() == AuditResult.DENIED));
    }

    @Test
    void createWithoutManagerNormalizesAndAuditsAfterInsert() {
        var subject =
                subjectCommandService.createSubject(
                        admin,
                        new SubjectCommandService.Create(
                                " swr302 ", " Name ", "secret description", null));
        assertThat(subject.code().value()).isEqualTo("SWR302");
        assertThat(subject.name()).isEqualTo("Name");
        assertThat(subject.status()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(subject.version()).isEqualTo(1);
        assertThat(subject.createdAt()).isEqualTo(NOW);
        var order = inOrder(subjectRepository, audit);
        order.verify(subjectRepository).hasSubjectWithCode(subject.code());
        order.verify(subjectRepository).insertSubject(subject);
        order.verify(audit).record(any());
        AuditEvent event = event();
        assertThat(event.action()).isEqualTo("SUBJECT_SWR302_CREATED");
        assertThat(event.actorId()).isEqualTo(admin.accountId());
        assertThat(event.result()).isEqualTo(AuditResult.SUCCESS);
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.details())
                .containsEntry("managerId", null)
                .doesNotContainKey("description");
        verifyNoInteractions(accountLookup);
    }

    @Test
    void duplicatePrecheckRejectsWithoutInsertAuditOrGuard() {
        when(subjectRepository.hasSubjectWithCode(any())).thenReturn(true);
        code(
                () -> subjectCommandService.createSubject(admin, create(newManager)),
                "SUBJECT_CODE_DUPLICATE");
        verify(subjectRepository, never()).insertSubject(any());
        verifyNoInteractions(accountLookup, audit);
    }

    @Test
    void onlyNamedUniqueConstraintIsTranslatedForConcurrentInsert() {
        var unique =
                new java.sql.SQLException(
                        "ERROR: duplicate key value violates unique constraint \"uk_subjects_code\"",
                        "23505");
        doThrow(new DataIntegrityViolationException("duplicate", unique))
                .when(subjectRepository)
                .insertSubject(any());
        code(
                () -> subjectCommandService.createSubject(admin, create(null)),
                "SUBJECT_CODE_DUPLICATE");
        var foreign =
                new DataIntegrityViolationException(
                        "FK",
                        new java.sql.SQLException(
                                "ERROR: insert or update on table \"subjects\" violates foreign key "
                                        + "constraint \"fk_subjects_manager\"",
                                "23503"));
        doThrow(foreign).when(subjectRepository).insertSubject(any());
        assertThatThrownBy(
                () -> subjectCommandService.createSubject(admin, create(null))).isSameAs(foreign);
        verifyNoInteractions(audit);
    }

    @Test
    void createManagerIsValidatedBeforeInsert() {
        eligible(newManager);
        var after = subjectCommandService.createSubject(admin, create(newManager));
        var order = inOrder(accountLookup, subjectRepository, audit);
        order.verify(subjectRepository).hasSubjectWithCode(after.code());
        order.verify(accountLookup).lockContact(newManager);
        order.verify(subjectRepository).insertSubject(after);
        order.verify(audit).record(any());
        assertThat(after.managerId()).isEqualTo(newManager);
    }

    static Stream<Arguments> invalidManagers() {
        return Stream.of(
                Arguments.of(Role.ADMIN, AccountStatus.ACTIVE),
                Arguments.of(Role.STUDENT, AccountStatus.ACTIVE),
                Arguments.of(Role.TEACHER, AccountStatus.ACTIVE),
                Arguments.of(Role.SUBJECT_MANAGER, AccountStatus.PENDING),
                Arguments.of(Role.SUBJECT_MANAGER, AccountStatus.DISABLED));
    }

    @ParameterizedTest
    @MethodSource("invalidManagers")
    void invalidManagerIsRejectedAtSubmit(Role role, AccountStatus status) {
        when(accountLookup.lockContact(newManager))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        newManager, "x@example.test", "X", role, status)));
        code(
                () -> subjectCommandService.createSubject(admin, create(newManager)),
                "SUBJECT_MANAGER_INVALID");
        verify(subjectRepository, never()).insertSubject(any());
        verifyNoInteractions(audit);
    }

    @Test
    void missingOrMismatchedContactIsInvalidButLookupErrorIsUnavailable() {
        when(accountLookup.lockContact(newManager)).thenReturn(Optional.empty());
        code(
                () -> subjectCommandService.createSubject(admin, create(newManager)),
                "SUBJECT_MANAGER_INVALID");
        when(accountLookup.lockContact(newManager))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        oldManager,
                                        "x@example.test",
                                        "X",
                                        Role.SUBJECT_MANAGER,
                                        AccountStatus.ACTIVE)));
        code(
                () -> subjectCommandService.createSubject(admin, create(newManager)),
                "SUBJECT_MANAGER_INVALID");
        when(accountLookup.lockContact(newManager)).thenThrow(new IllegalStateException("down"));
        code(
                () -> subjectCommandService.createSubject(admin, create(newManager)),
                "SUBJECT_ACCOUNT_LOOKUP_UNAVAILABLE");
        verify(subjectRepository, never()).insertSubject(any());
        verifyNoInteractions(audit);
    }

    @Test
    void invalidInputAndMissingVersionNeverWrite() {
        code(
                () ->
                        subjectCommandService.createSubject(
                                admin, new SubjectCommandService.Create("a_", "Name", null, null)),
                "SUBJECT_CODE_INVALID");
        code(
                () ->
                        subjectCommandService.createSubject(
                                admin, new SubjectCommandService.Create("AAA", " ", null, null)),
                "SUBJECT_NAME_INVALID");
        code(
                () ->
                        subjectCommandService.updateInformation(
                                admin,
                                subjectId,
                                new SubjectCommandService.UpdateInformation("Name", null, null)),
                "SUBJECT_VERSION_REQUIRED");
        code(
                () ->
                        subjectCommandService.changeManager(
                                admin,
                                subjectId,
                                new SubjectCommandService.ChangeManager(null, 0L)),
                "SUBJECT_VERSION_REQUIRED");
        verify(subjectRepository, never()).insertSubject(any());
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void metadataOnArchivedSubjectChangesOnlyEditableFieldsWithoutAudit() {
        Subject before = subject(oldManager).archive(NOW.minusSeconds(30));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(before));
        Subject after =
                subjectCommandService.updateInformation(
                        admin,
                        subjectId,
                        new SubjectCommandService.UpdateInformation("New", null, 2L));
        assertThat(after.status()).isEqualTo(SubjectStatus.ARCHIVED);
        assertThat(after.managerId()).isNull();
        assertThat(after.code()).isEqualTo(before.code());
        assertThat(after.createdAt()).isEqualTo(before.createdAt());
        assertThat(after.version()).isEqualTo(3);
        verifyNoInteractions(accountLookup, audit);
    }

    @Test
    void staleVersionRejectedEvenForSameValues() {
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(oldManager)));
        code(
                () ->
                        subjectCommandService.updateInformation(
                                admin,
                                subjectId,
                                new SubjectCommandService.UpdateInformation(
                                        "Name", "Description", 2L)),
                "SUBJECT_VERSION_CONFLICT");
        code(
                () ->
                        subjectCommandService.changeManager(
                                admin,
                                subjectId,
                                new SubjectCommandService.ChangeManager(oldManager, 2L)),
                "SUBJECT_VERSION_CONFLICT");
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void currentNoOpDoesNotWriteLockOrAudit() {
        Subject before = subject(oldManager);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(before));
        assertThat(
                        subjectCommandService.updateInformation(
                                admin,
                                subjectId,
                                new SubjectCommandService.UpdateInformation(
                                        "Name", "Description", 1L)))
                .isSameAs(before);
        assertThat(
                        subjectCommandService.changeManager(
                                admin,
                                subjectId,
                                new SubjectCommandService.ChangeManager(oldManager, 1L)))
                .isSameAs(before);
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void managerChangeReloadsLockedSubjectBeforeValidationAndWrite() {
        Subject before = subject(oldManager);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(before));
        when(subjectRepository.findByIdForUpdate(subjectId)).thenReturn(Optional.of(before));
        eligible(newManager);
        Subject after =
                subjectCommandService.changeManager(
                        admin, subjectId, new SubjectCommandService.ChangeManager(newManager, 1L));
        var order = inOrder(subjectRepository, accountLookup);
        order.verify(subjectRepository).findById(subjectId);
        order.verify(accountLookup).lockContact(newManager);
        order.verify(subjectRepository).findByIdForUpdate(subjectId);
        order.verify(subjectRepository).updateSubject(after, 1L);
        assertThat(after.managerId()).isEqualTo(newManager);
        verifyNoInteractions(audit);
    }

    @Test
    void unassignmentWritesNoAudit() {
        Subject before = subject(oldManager);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(before));
        when(subjectRepository.findByIdForUpdate(subjectId)).thenReturn(Optional.of(before));
        Subject after =
                subjectCommandService.changeManager(
                        admin, subjectId, new SubjectCommandService.ChangeManager(null, 1L));
        assertThat(after.managerId()).isNull();
        assertThat(after.version()).isEqualTo(2);
        verifyNoInteractions(accountLookup, audit);
    }

    @Test
    void assigningManagerToArchivedSubjectIsRejected() {
        Subject archived = subject(null).archive(NOW.minusSeconds(30));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(archived));
        when(subjectRepository.findByIdForUpdate(subjectId)).thenReturn(Optional.of(archived));
        code(
                () ->
                        subjectCommandService.changeManager(
                                admin,
                                subjectId,
                                new SubjectCommandService.ChangeManager(newManager, 2L)),
                "SUBJECT_ARCHIVED");
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
        verifyNoInteractions(accountLookup, audit);
    }

    @Test
    void concurrentChangeIsRejectedAfterLockedReload() {
        Subject before = subject(oldManager);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(before));
        when(subjectRepository.findByIdForUpdate(subjectId))
                .thenReturn(Optional.of(before.changeManager(null, NOW)));
        code(
                () ->
                        subjectCommandService.changeManager(
                                admin,
                                subjectId,
                                new SubjectCommandService.ChangeManager(newManager, 1L)),
                "SUBJECT_VERSION_CONFLICT");
        // Tài khoản được khóa trước dòng môn (cùng thứ tự với U01); version lệch vẫn báo trước Chủ
        // nhiệm môn.
        verify(accountLookup).lockContact(newManager);
        verifyNoInteractions(audit);
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
    }

    @Test
    void compareAndSetFailureDoesNotEmitSuccessAudit() {
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject(null)));
        when(subjectRepository.updateSubject(any(), anyLong())).thenReturn(false);
        code(
                () ->
                        subjectCommandService.updateInformation(
                                admin,
                                subjectId,
                                new SubjectCommandService.UpdateInformation("New", null, 1L)),
                "SUBJECT_VERSION_CONFLICT");
        verifyNoInteractions(audit);
    }

    @Test
    void auditFailureIsPropagatedForTransactionRollback() {
        var failure = new IllegalStateException("audit down");
        doThrow(failure).when(audit).record(any());
        assertThatThrownBy(
                () -> subjectCommandService.createSubject(admin, create(null))).isSameAs(failure);
    }

    private SubjectCommandService.Create create(UUID manager) {
        return new SubjectCommandService.Create("SWR302", "Name", "Description", manager);
    }

    private Subject subject(UUID manager) {
        return Subject.createSubject(
                subjectId, "SWR302", "Name", "Description", manager, NOW.minusSeconds(60));
    }

    private void eligible(UUID id) {
        when(accountLookup.lockContact(id))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        id,
                                        "manager@example.test",
                                        "Manager",
                                        Role.SUBJECT_MANAGER,
                                        AccountStatus.ACTIVE)));
    }

    private AuditEvent event() {
        var captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(captor.capture());
        return captor.getValue();
    }

    private void code(Runnable call, String code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        ApiException.class, error -> assertThat(error.readCode()).isEqualTo(code));
    }
}
