package edu.aiplatform.subjects.application;

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
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
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
    private final UUID subjectId = UUID.randomUUID(), oldManager = UUID.randomUUID(), newManager = UUID.randomUUID();
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    private final ISubjectRepository repository = mock(ISubjectRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAccountLookup accounts = mock(IAccountLookup.class);
    private final IAudit audit = mock(IAudit.class);
    private final SubjectCommandService service = new SubjectCommandService(repository, authorization, accounts,
            new SubjectAuditRecorder(audit), Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setup() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(repository.update(any(), anyLong())).thenReturn(true);
    }

    @Test
    void anonymousCannotExecuteAnyCommand() {
        code(() -> service.create(null, null), "AUTHENTICATION_REQUIRED");
        code(() -> service.updateInformation(null, subjectId, null), "AUTHENTICATION_REQUIRED");
        code(() -> service.changeManager(null, subjectId, null), "AUTHENTICATION_REQUIRED");
        verifyNoInteractions(repository, accounts);
        verify(audit, times(3)).record(argThat(event -> event.result() == AuditResult.DENIED));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void nonAdminCannotExecuteAnyCommand(Role role) {
        var actor = new Actor(UUID.randomUUID(), role);
        code(() -> service.create(actor, null), "SUBJECT_FORBIDDEN");
        code(() -> service.updateInformation(actor, subjectId, null), "SUBJECT_FORBIDDEN");
        code(() -> service.changeManager(actor, subjectId, null), "SUBJECT_FORBIDDEN");
        verifyNoInteractions(repository, accounts);
        verify(audit, times(3)).record(argThat(event -> event.result() == AuditResult.DENIED));
    }

    @Test
    void deniedAndUnavailableAuthorizationNeverWrite() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("DISABLED"));
        code(() -> service.create(admin, null), "SUBJECT_FORBIDDEN");
        doThrow(new IllegalStateException("down")).when(authorization).authorize(any(), anyString(), any());
        code(() -> service.changeManager(admin, subjectId, null), "SUBJECT_AUTHORIZATION_UNAVAILABLE");
        verifyNoInteractions(repository, accounts);
        verify(audit, times(2)).record(argThat(event -> event.result() == AuditResult.DENIED));
    }

    @Test
    void createWithoutManagerNormalizesAndAuditsAfterInsert() {
        var subject = service.create(admin, new SubjectCommandService.Create(" swr302 ", " Name ", "secret description", null));
        assertThat(subject.code().value()).isEqualTo("SWR302");
        assertThat(subject.name()).isEqualTo("Name");
        assertThat(subject.status()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(subject.version()).isEqualTo(1);
        assertThat(subject.createdAt()).isEqualTo(NOW);
        var order = inOrder(repository, audit);
        order.verify(repository).existsByCode(subject.code());
        order.verify(repository).insert(subject);
        order.verify(audit).record(any());
        AuditEvent event = event();
        assertThat(event.action()).isEqualTo("SUBJECT_CREATED");
        assertThat(event.actorId()).isEqualTo(admin.accountId());
        assertThat(event.result()).isEqualTo(AuditResult.SUCCESS);
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.details()).containsEntry("managerId", null).doesNotContainKey("description");
        verifyNoInteractions(accounts);
    }

    @Test
    void duplicatePrecheckRejectsWithoutInsertAuditOrGuard() {
        when(repository.existsByCode(any())).thenReturn(true);
        code(() -> service.create(admin, create(newManager)), "SUBJECT_CODE_DUPLICATE");
        verify(repository, never()).insert(any());
        verifyNoInteractions(accounts, audit);
    }

    @Test
    void onlyNamedUniqueConstraintIsTranslatedForConcurrentInsert() {
        var unique = new org.hibernate.exception.ConstraintViolationException("duplicate", new java.sql.SQLException(), "uk_subjects_code");
        doThrow(new DataIntegrityViolationException("duplicate", unique)).when(repository).insert(any());
        code(() -> service.create(admin, create(null)), "SUBJECT_CODE_DUPLICATE");
        var foreign = new DataIntegrityViolationException("FK", new org.hibernate.exception.ConstraintViolationException(
                "FK", new java.sql.SQLException(), "fk_subjects_manager"));
        doThrow(foreign).when(repository).insert(any());
        assertThatThrownBy(() -> service.create(admin, create(null))).isSameAs(foreign);
        verifyNoInteractions(audit);
    }

    @Test
    void createManagerIsValidatedBeforeInsert() {
        eligible(newManager);
        var after = service.create(admin, create(newManager));
        var order = inOrder(accounts, repository, audit);
        order.verify(repository).existsByCode(after.code());
        order.verify(accounts).getContact(newManager);
        order.verify(repository).insert(after);
        order.verify(audit).record(any());
        assertThat(after.managerId()).isEqualTo(newManager);
    }

    static Stream<Arguments> invalidManagers() {
        return Stream.of(Arguments.of(Role.ADMIN, AccountStatus.ACTIVE), Arguments.of(Role.STUDENT, AccountStatus.ACTIVE),
                Arguments.of(Role.TEACHER, AccountStatus.ACTIVE), Arguments.of(Role.SUBJECT_MANAGER, AccountStatus.PENDING),
                Arguments.of(Role.SUBJECT_MANAGER, AccountStatus.DISABLED));
    }

    @ParameterizedTest
    @MethodSource("invalidManagers")
    void invalidManagerIsRejectedAtSubmit(Role role, AccountStatus status) {
        when(accounts.getContact(newManager)).thenReturn(Optional.of(new AccountContact(newManager, "x@example.test", "X", role, status)));
        code(() -> service.create(admin, create(newManager)), "SUBJECT_MANAGER_INVALID");
        verify(repository, never()).insert(any());
        verifyNoInteractions(audit);
    }

    @Test
    void missingOrMismatchedContactIsInvalidButLookupErrorIsUnavailable() {
        when(accounts.getContact(newManager)).thenReturn(Optional.empty());
        code(() -> service.create(admin, create(newManager)), "SUBJECT_MANAGER_INVALID");
        when(accounts.getContact(newManager)).thenReturn(Optional.of(new AccountContact(oldManager, "x@example.test", "X", Role.SUBJECT_MANAGER, AccountStatus.ACTIVE)));
        code(() -> service.create(admin, create(newManager)), "SUBJECT_MANAGER_INVALID");
        when(accounts.getContact(newManager)).thenThrow(new IllegalStateException("down"));
        code(() -> service.create(admin, create(newManager)), "SUBJECT_ACCOUNT_LOOKUP_UNAVAILABLE");
        verify(repository, never()).insert(any());
        verifyNoInteractions(audit);
    }

    @Test
    void invalidInputAndMissingVersionNeverWrite() {
        code(() -> service.create(admin, new SubjectCommandService.Create("a_", "Name", null, null)), "SUBJECT_CODE_INVALID");
        code(() -> service.create(admin, new SubjectCommandService.Create("AAA", " ", null, null)), "SUBJECT_NAME_INVALID");
        code(() -> service.updateInformation(admin, subjectId, new SubjectCommandService.UpdateInformation("Name", null, null)), "SUBJECT_VERSION_REQUIRED");
        code(() -> service.changeManager(admin, subjectId, new SubjectCommandService.ChangeManager(null, 0L)), "SUBJECT_VERSION_REQUIRED");
        verify(repository, never()).insert(any());
        verify(repository, never()).update(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void metadataOnArchivedSubjectChangesOnlyEditableFieldsAndAuditsFieldNames() {
        Subject before = subject(oldManager).archive(NOW.minusSeconds(30));
        when(repository.findById(subjectId)).thenReturn(Optional.of(before));
        Subject after = service.updateInformation(admin, subjectId, new SubjectCommandService.UpdateInformation("New", null, 2L));
        assertThat(after.status()).isEqualTo(SubjectStatus.ARCHIVED);
        assertThat(after.managerId()).isNull();
        assertThat(after.code()).isEqualTo(before.code());
        assertThat(after.createdAt()).isEqualTo(before.createdAt());
        assertThat(after.version()).isEqualTo(3);
        assertThat(event().details()).containsEntry("changedFields", List.of("name", "description"));
        verifyNoInteractions(accounts);
    }

    @Test
    void staleVersionRejectedEvenForSameValues() {
        when(repository.findById(subjectId)).thenReturn(Optional.of(subject(oldManager)));
        code(() -> service.updateInformation(admin, subjectId, new SubjectCommandService.UpdateInformation("Name", "Description", 2L)), "SUBJECT_VERSION_CONFLICT");
        code(() -> service.changeManager(admin, subjectId, new SubjectCommandService.ChangeManager(oldManager, 2L)), "SUBJECT_VERSION_CONFLICT");
        verify(repository, never()).update(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void currentNoOpDoesNotWriteLockOrAudit() {
        Subject before = subject(oldManager);
        when(repository.findById(subjectId)).thenReturn(Optional.of(before));
        assertThat(service.updateInformation(admin, subjectId, new SubjectCommandService.UpdateInformation("Name", "Description", 1L))).isSameAs(before);
        assertThat(service.changeManager(admin, subjectId, new SubjectCommandService.ChangeManager(oldManager, 1L))).isSameAs(before);
        verify(repository, never()).update(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void managerChangeReloadsLockedSubjectBeforeValidationAndWrite() {
        Subject before = subject(oldManager);
        when(repository.findById(subjectId)).thenReturn(Optional.of(before));
        when(repository.findByIdForUpdate(subjectId)).thenReturn(Optional.of(before));
        eligible(newManager);
        Subject after = service.changeManager(admin, subjectId, new SubjectCommandService.ChangeManager(newManager, 1L));
        var order = inOrder(repository, accounts, audit);
        order.verify(repository).findById(subjectId);
        order.verify(repository).findByIdForUpdate(subjectId);
        order.verify(accounts).getContact(newManager);
        order.verify(repository).update(after, 1L);
        order.verify(audit).record(any());
        assertThat(after.managerId()).isEqualTo(newManager);
        assertThat(event().details()).containsEntry("oldManagerId", oldManager).containsEntry("newManagerId", newManager);
    }

    @Test
    void unassignmentAuditContainsExplicitNull() {
        Subject before = subject(oldManager);
        when(repository.findById(subjectId)).thenReturn(Optional.of(before));
        when(repository.findByIdForUpdate(subjectId)).thenReturn(Optional.of(before));
        Subject after = service.changeManager(admin, subjectId, new SubjectCommandService.ChangeManager(null, 1L));
        assertThat(after.managerId()).isNull();
        assertThat(after.version()).isEqualTo(2);
        assertThat(event().details()).containsEntry("newManagerId", null);
        verifyNoInteractions(accounts);
    }

    @Test
    void assigningManagerToArchivedSubjectIsRejected() {
        Subject archived = subject(null).archive(NOW.minusSeconds(30));
        when(repository.findById(subjectId)).thenReturn(Optional.of(archived));
        when(repository.findByIdForUpdate(subjectId)).thenReturn(Optional.of(archived));
        code(() -> service.changeManager(admin, subjectId, new SubjectCommandService.ChangeManager(newManager, 2L)), "SUBJECT_ARCHIVED");
        verify(repository, never()).update(any(), anyLong());
        verifyNoInteractions(accounts, audit);
    }

    @Test
    void concurrentChangeIsRejectedAfterLockedReload() {
        Subject before = subject(oldManager);
        when(repository.findById(subjectId)).thenReturn(Optional.of(before));
        when(repository.findByIdForUpdate(subjectId)).thenReturn(Optional.of(before.changeManager(null, NOW)));
        code(() -> service.changeManager(admin, subjectId, new SubjectCommandService.ChangeManager(newManager, 1L)), "SUBJECT_VERSION_CONFLICT");
        verifyNoInteractions(accounts, audit);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    void compareAndSetFailureDoesNotEmitSuccessAudit() {
        when(repository.findById(subjectId)).thenReturn(Optional.of(subject(null)));
        when(repository.update(any(), anyLong())).thenReturn(false);
        code(() -> service.updateInformation(admin, subjectId, new SubjectCommandService.UpdateInformation("New", null, 1L)), "SUBJECT_VERSION_CONFLICT");
        verifyNoInteractions(audit);
    }

    @Test
    void auditFailureIsPropagatedForTransactionRollback() {
        var failure = new IllegalStateException("audit down");
        doThrow(failure).when(audit).record(any());
        assertThatThrownBy(() -> service.create(admin, create(null))).isSameAs(failure);
    }

    private SubjectCommandService.Create create(UUID manager) {
        return new SubjectCommandService.Create("SWR302", "Name", "Description", manager);
    }

    private Subject subject(UUID manager) {
        return Subject.create(subjectId, "SWR302", "Name", "Description", manager, NOW.minusSeconds(60));
    }

    private void eligible(UUID id) {
        when(accounts.getContact(id)).thenReturn(Optional.of(new AccountContact(id, "manager@example.test", "Manager", Role.SUBJECT_MANAGER, AccountStatus.ACTIVE)));
    }

    private AuditEvent event() {
        var captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(captor.capture());
        return captor.getValue();
    }

    private void code(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo(code));
    }
}
