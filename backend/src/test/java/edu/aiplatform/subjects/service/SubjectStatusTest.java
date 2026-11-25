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
        assertThat(event.getValue().action()).isEqualTo("SUBJECT_ARCHIVED");
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
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
