package edu.aiplatform.subjects.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
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
    private final ISubjectRepository repository = mock(ISubjectRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final ISubjectClasses checker = mock(ISubjectClasses.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ISubjectClasses> checks = mock(ObjectProvider.class);
    private final IAudit audit = mock(IAudit.class);
    private final SubjectStatusService service = new SubjectStatusService(repository, authorization, checks,
            new SubjectAuditRecorder(audit), Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setup() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(checks.getIfAvailable()).thenReturn(checker);
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenReturn(Optional.of(active()));
        when(repository.update(any(), anyLong())).thenReturn(true);
    }

    @Test
    void anonymousRejectedBeforeRepository() {
        code(() -> service.changeStatus(null, id, archive()), "AUTHENTICATION_REQUIRED");
        verifyNoInteractions(repository, checks, checker);
        verify(audit).record(argThat(event -> event.result() == edu.aiplatform.audit.contract.AuditResult.DENIED));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void nonAdminRejectedBeforeRepository(Role role) {
        code(() -> service.changeStatus(new Actor(UUID.randomUUID(), role), id, archive()), "SUBJECT_FORBIDDEN");
        verifyNoInteractions(repository, checks, checker);
        verify(audit).record(argThat(event -> event.result() == edu.aiplatform.audit.contract.AuditResult.DENIED));
    }

    @Test
    void authorizationErrorFailsClosed() {
        when(authorization.authorize(any(), anyString(), any())).thenThrow(new IllegalStateException("down"));
        code(() -> service.changeStatus(admin, id, archive()), "SUBJECT_AUTHORIZATION_UNAVAILABLE");
        verifyNoInteractions(repository, checks, checker);
        verify(audit).record(argThat(event -> event.result() == edu.aiplatform.audit.contract.AuditResult.DENIED));
    }

    @Test
    void invalidStatusAndVersionNeverLockOrWrite() {
        code(() -> service.changeStatus(admin, id, null), "SUBJECT_STATUS_INVALID");
        code(() -> service.changeStatus(admin, id, new SubjectStatusService.ChangeStatus(null, 1L)), "SUBJECT_STATUS_INVALID");
        code(() -> service.changeStatus(admin, id, new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, null)), "SUBJECT_VERSION_REQUIRED");
        verifyNoInteractions(repository, checks, checker, audit);
    }

    @Test
    void missingSubjectIs404BeforeProvider() {
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenReturn(Optional.empty());
        code(() -> service.changeStatus(admin, id, archive()), "SUBJECT_NOT_FOUND");
        verifyNoInteractions(checks, checker, audit);
    }

    @Test
    void lockFailureIsUnavailableAndDoesNotAskClasses() {
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenThrow(new CannotAcquireLockException("busy"));
        code(() -> service.changeStatus(admin, id, archive()), "SUBJECT_LIFECYCLE_UNAVAILABLE");
        verifyNoInteractions(checks, checker, audit);
    }

    @Test
    void staleVersionRejectedBeforeNoOpOrClasses() {
        code(() -> service.changeStatus(admin, id, new SubjectStatusService.ChangeStatus(SubjectStatus.ACTIVE, 2L)), "SUBJECT_VERSION_CONFLICT");
        verifyNoInteractions(checks, checker, audit);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    void validNoOpDoesNotRequireProviderOrAudit() {
        Subject before = active();
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenReturn(Optional.of(before));
        assertThat(service.changeStatus(admin, id, new SubjectStatusService.ChangeStatus(SubjectStatus.ACTIVE, 1L))).isSameAs(before);
        verifyNoInteractions(checks, checker, audit);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    void missingNullAndFailedProviderCannotBecomeZeroClasses() {
        when(checks.getIfAvailable()).thenReturn(null);
        code(() -> service.changeStatus(admin, id, archive()), "SUBJECT_CLASSES_UNAVAILABLE");
        when(checks.getIfAvailable()).thenReturn(checker);
        when(checker.archiveClassesOfSubject(id, admin)).thenThrow(new IllegalStateException("SQL internal"));
        code(() -> service.changeStatus(admin, id, archive()), "SUBJECT_CLASSES_UNAVAILABLE");
        verify(repository, never()).update(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void archiveAsksU05ToArchiveEveryClassOnCallerThreadBeforeSavingSubject() {
        Thread caller = Thread.currentThread();
        when(checker.archiveClassesOfSubject(id, admin)).thenAnswer(call -> {
            assertThat(Thread.currentThread()).isSameAs(caller);
            return List.of(UUID.randomUUID(), UUID.randomUUID());
        });
        Subject after = service.changeStatus(admin, id, archive());
        assertThat(after.status()).isEqualTo(SubjectStatus.ARCHIVED);
        assertThat(after.version()).isEqualTo(2);
        var order = inOrder(repository, checker, audit);
        order.verify(repository).findByIdForUpdate(eq(id), any(Duration.class));
        order.verify(checker).archiveClassesOfSubject(id, admin);
        order.verify(repository).update(after, 1L);
        order.verify(audit).record(any());
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("SUBJECT_ARCHIVED");
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void reopenDoesNotReadOrReopenClasses() {
        Subject archived = active().archive(NOW.minusSeconds(1));
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenReturn(Optional.of(archived));
        Subject reopened = service.changeStatus(admin, id, new SubjectStatusService.ChangeStatus(SubjectStatus.ACTIVE, 2L));
        assertThat(reopened.status()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(reopened.version()).isEqualTo(3);
        verifyNoInteractions(checks, checker);
        var event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("SUBJECT_REOPENED");
    }

    @Test
    void lateZeroFromProviderCannotAuthorizeArchive() throws InterruptedException {
        when(checker.archiveClassesOfSubject(id, admin)).thenAnswer(call -> {
            Thread.sleep(1100);
            return List.of();
        });
        code(() -> service.changeStatus(admin, id, archive()), "SUBJECT_LIFECYCLE_TIMEOUT");
        verify(repository, never()).update(any(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void staleCasDoesNotAuditSuccessAndAuditErrorIsNotSwallowed() {
        when(checker.archiveClassesOfSubject(id, admin)).thenReturn(List.of());
        when(repository.update(any(), anyLong())).thenReturn(false);
        code(() -> service.changeStatus(admin, id, archive()), "SUBJECT_VERSION_CONFLICT");
        verifyNoInteractions(audit);
        when(repository.update(any(), anyLong())).thenReturn(true);
        var failure = new IllegalStateException("audit down");
        doThrow(failure).when(audit).record(any());
        assertThatThrownBy(() -> service.changeStatus(admin, id, archive())).isSameAs(failure);
    }

    private Subject active() { return Subject.create(id, "ABC", "Name", null, null, NOW.minusSeconds(60)); }
    private SubjectStatusService.ChangeStatus archive() { return new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, 1L); }
    private void code(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo(code));
    }
}
