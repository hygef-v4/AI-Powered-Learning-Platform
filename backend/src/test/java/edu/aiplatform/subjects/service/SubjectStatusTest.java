package edu.aiplatform.subjects.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.CannotAcquireLockException;

class SubjectStatusTest {
    private static final Instant NOW = Instant.parse("2026-10-18T08:00:00Z");
    private final UUID id = UUID.randomUUID();
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    private final ISubjectRepository subjectRepository = mock(ISubjectRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final ISubjectClasses checker = mock(ISubjectClasses.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<ISubjectClasses> checks = mock(ObjectProvider.class);

    private final IAudit audit = mock(IAudit.class);
    private final SubjectStatusService subjectStatusService =
            new SubjectStatusService(
                    subjectRepository,
                    authorization,
                    checks,
                    new SubjectAuditRecorder(audit),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setup() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(AuthorizationDecision.allow());
        when(checks.getIfAvailable()).thenReturn(checker);
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenReturn(Optional.of(active()));
        when(subjectRepository.updateSubject(any(), anyLong())).thenReturn(true);
    }

    @Test
    void anonymousRejectedBeforeRepository() {
        code(
                () -> subjectStatusService.changeStatus(null, id, archive()),
                "AUTHENTICATION_REQUIRED");
        verifyNoInteractions(subjectRepository, checks, checker);
        verify(audit)
                .record(
                        argThat(
                                event ->
                                        event.result()
                                                == edu.aiplatform.audit.contract.AuditResult
                                                        .DENIED));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void nonAdminRejectedBeforeRepository(Role role) {
        code(
                () -> subjectStatusService.changeStatus(
                        new Actor(UUID.randomUUID(), role), id, archive()),
                "SUBJECT_FORBIDDEN");
        verifyNoInteractions(subjectRepository, checks, checker);
        verify(audit)
                .record(
                        argThat(
                                event ->
                                        event.result()
                                                == edu.aiplatform.audit.contract.AuditResult
                                                        .DENIED));
    }

    @Test
    void authorizationErrorFailsClosed() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenThrow(new IllegalStateException("down"));
        code(
                () -> subjectStatusService.changeStatus(admin, id, archive()),
                "SUBJECT_AUTHORIZATION_UNAVAILABLE");
        verifyNoInteractions(subjectRepository, checks, checker);
        verify(audit)
                .record(
                        argThat(
                                event ->
                                        event.result()
                                                == edu.aiplatform.audit.contract.AuditResult
                                                        .DENIED));
    }

    @Test
    void invalidStatusAndVersionNeverLockOrWrite() {
        code(() -> subjectStatusService.changeStatus(admin, id, null), "SUBJECT_STATUS_INVALID");
        code(
                () ->
                        subjectStatusService.changeStatus(
                                admin, id, new SubjectStatusService.ChangeStatus(null, 1L)),
                "SUBJECT_STATUS_INVALID");
        code(
                () ->
                        subjectStatusService.changeStatus(
                                admin,
                                id,
                                new SubjectStatusService.ChangeStatus(
                                        SubjectStatus.ARCHIVED, null)),
                "SUBJECT_VERSION_REQUIRED");
        verifyNoInteractions(subjectRepository, checks, checker, audit);
    }

    @Test
    void missingSubjectIs404BeforeProvider() {
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenReturn(Optional.empty());
        code(() -> subjectStatusService.changeStatus(admin, id, archive()), "SUBJECT_NOT_FOUND");
        verifyNoInteractions(checks, checker, audit);
    }

    @Test
    void lockFailureIsUnavailableAndDoesNotAskClasses() {
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenThrow(new CannotAcquireLockException("busy"));
        code(
                () -> subjectStatusService.changeStatus(admin, id, archive()),
                "SUBJECT_LIFECYCLE_UNAVAILABLE");
        verifyNoInteractions(checks, checker, audit);
    }

    @Test
    void staleVersionRejectedBeforeNoOpOrClasses() {
        code(
                () ->
                        subjectStatusService.changeStatus(
                                admin,
                                id,
                                new SubjectStatusService.ChangeStatus(SubjectStatus.ACTIVE, 2L)),
                "SUBJECT_VERSION_CONFLICT");
        verifyNoInteractions(checks, checker, audit);
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
    }

    @Test
    void validNoOpDoesNotRequireProviderOrAudit() {
        Subject before = active();
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenReturn(Optional.of(before));
        assertThat(
                        subjectStatusService.changeStatus(
                                admin,
                                id,
                                new SubjectStatusService.ChangeStatus(SubjectStatus.ACTIVE, 1L)))
                .isSameAs(before);
        verifyNoInteractions(checks, checker, audit);
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
    }

    @Test
    void nullArchiveResultFailsClosedBeforeSubjectUpdateOrSuccessAudit() {
        when(checker.archiveClassesOfSubject(id, admin)).thenReturn(null);
        code(
                () -> subjectStatusService.changeStatus(admin, id, archive()),
                "SUBJECT_CLASSES_UNAVAILABLE");
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void missingNullAndFailedProviderCannotBecomeZeroClasses() {
        when(checks.getIfAvailable()).thenReturn(null);
        code(
                () -> subjectStatusService.changeStatus(admin, id, archive()),
                "SUBJECT_CLASSES_UNAVAILABLE");
        when(checks.getIfAvailable()).thenReturn(checker);
        when(checker.archiveClassesOfSubject(id, admin))
                .thenThrow(new IllegalStateException("SQL internal"));
        code(
                () -> subjectStatusService.changeStatus(admin, id, archive()),
                "SUBJECT_CLASSES_UNAVAILABLE");
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void archiveAsksU05ToArchiveEveryClassOnCallerThreadBeforeSavingSubject() {
        Thread caller = Thread.currentThread();
        when(checker.archiveClassesOfSubject(id, admin))
                .thenAnswer(
                        call -> {
                            assertThat(Thread.currentThread()).isSameAs(caller);
                            return List.of(UUID.randomUUID(), UUID.randomUUID());
                        });
        Subject after = subjectStatusService.changeStatus(admin, id, archive());
        assertThat(after.status()).isEqualTo(SubjectStatus.ARCHIVED);
        assertThat(after.version()).isEqualTo(2);
        var order = inOrder(subjectRepository, checker, audit);
        order.verify(subjectRepository).findByIdForUpdate(eq(id), any(Duration.class));
        order.verify(checker).archiveClassesOfSubject(id, admin);
        order.verify(subjectRepository).updateSubject(after, 1L);
        order.verify(audit).record(any());
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("SUBJECT_ABC_ARCHIVED");
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void reopenDoesNotReadOrReopenClasses() {
        Subject archived = active().archive(NOW.minusSeconds(1));
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenReturn(Optional.of(archived));
        Subject reopened =
                subjectStatusService.changeStatus(
                        admin, id, new SubjectStatusService.ChangeStatus(SubjectStatus.ACTIVE, 2L));
        assertThat(reopened.status()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(reopened.version()).isEqualTo(3);
        verifyNoInteractions(checks, checker);
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("SUBJECT_ABC_REOPENED");
    }

    @Test
    void lateZeroFromProviderCannotAuthorizeArchive() throws InterruptedException {
        when(checker.archiveClassesOfSubject(id, admin))
                .thenAnswer(
                        call -> {
                            Thread.sleep(1100);
                            return List.of();
                        });
        code(
                () -> subjectStatusService.changeStatus(admin, id, archive()),
                "SUBJECT_LIFECYCLE_TIMEOUT");
        verify(subjectRepository, never()).updateSubject(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void staleCasDoesNotAuditSuccessAndAuditErrorIsNotSwallowed() {
        when(checker.archiveClassesOfSubject(id, admin)).thenReturn(List.of());
        when(subjectRepository.updateSubject(any(), anyLong())).thenReturn(false);
        code(
                () -> subjectStatusService.changeStatus(admin, id, archive()),
                "SUBJECT_VERSION_CONFLICT");
        verifyNoInteractions(audit);
        when(subjectRepository.updateSubject(any(), anyLong())).thenReturn(true);
        var failure = new IllegalStateException("audit down");
        doThrow(failure).when(audit).record(any());
        assertThatThrownBy(
                () -> subjectStatusService.changeStatus(admin, id, archive())).isSameAs(failure);
    }

    @Test
    void archiveWithManagerAuditsOnlyTheArchiveForOneVersionChange() {
        UUID managerId = UUID.randomUUID();
        Subject before = Subject.createSubject(id, "ABC", "Name", null, managerId, NOW.minusSeconds(60));
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenReturn(Optional.of(before));
        when(checker.archiveClassesOfSubject(id, admin)).thenReturn(List.of());

        Subject after = subjectStatusService.changeStatus(admin, id, archive());

        assertThat(after.managerId()).isNull();
        assertThat(after.version()).isEqualTo(2);
        verify(subjectRepository).updateSubject(after, 1L);
        var events = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit, times(1)).record(events.capture());
        assertThat(events.getAllValues()).extracting(AuditEvent::action)
                .containsExactly("SUBJECT_ABC_ARCHIVED");
        assertThat(events.getAllValues()).allSatisfy(event -> {
            assertThat(event.actorId()).isEqualTo(admin.accountId());
            assertThat(event.objectId()).isEqualTo(id.toString());
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
        assertThat(events.getAllValues().get(0).details())
                .containsEntry("before", "ACTIVE").containsEntry("after", "ARCHIVED");
    }

    private Subject active() {
        return Subject.createSubject(id, "ABC", "Name", null, null, NOW.minusSeconds(60));
    }

    private SubjectStatusService.ChangeStatus archive() {
        return new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, 1L);
    }

    private void code(Runnable call, String code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        ApiException.class, error -> assertThat(error.readCode()).isEqualTo(code));
    }
}
