package edu.aiplatform.subjects.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubjectScopeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");
    private final ISubjectRepository repository = mock(ISubjectRepository.class);
    private final IAudit audit = mock(IAudit.class);
    private final SubjectScopeService service = new SubjectScopeService(repository, new SubjectAuditRecorder(audit),
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);

    @Test
    void readsCurrentAssignmentEveryTimeAndDoesNotCacheAnAllowDecision() {
        UUID manager = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        when(repository.isManagedBy(manager, subject)).thenReturn(true, false);
        assertThat(service.isSubjectManager(manager, subject)).isTrue();
        assertThat(service.isSubjectManager(manager, subject)).isFalse();
        verify(repository, times(2)).isManagedBy(manager, subject);
    }

    @Test
    void unknownOrUnassignedSubjectDoesNotGrantScope() {
        UUID manager = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(repository.isManagedBy(manager, missing)).thenReturn(false);
        assertThat(service.isSubjectManager(manager, missing)).isFalse();
    }

    @Test
    void returnsManagedIdsForRoleChangeGuardWithoutCallingAuthorization() {
        UUID manager = UUID.randomUUID();
        Subject first = Subject.create(UUID.randomUUID(), "SWR302", "A", null, manager, NOW);
        Subject second = Subject.create(UUID.randomUUID(), "SWR303", "B", null, manager, NOW);
        when(repository.findManagedBy(manager)).thenReturn(List.of(first, second));
        assertThat(service.listManagedSubjects(manager)).containsExactly(first.id(), second.id());
    }

    @Test
    void releaseUnassignsEveryManagedSubjectBumpsVersionAndAuditsEach() {
        UUID manager = UUID.randomUUID();
        Subject first = Subject.create(UUID.randomUUID(), "SWR302", "A", null, manager, NOW.minusSeconds(60));
        Subject second = Subject.create(UUID.randomUUID(), "SWR303", "B", null, manager, NOW.minusSeconds(60));
        when(repository.findManagedBy(manager)).thenReturn(List.of(first, second));
        when(repository.findByIdForUpdate(first.id())).thenReturn(Optional.of(first));
        when(repository.findByIdForUpdate(second.id())).thenReturn(Optional.of(second));
        when(repository.update(any(), anyLong())).thenReturn(true);

        assertThat(service.releaseManagerAssignments(manager, admin)).containsExactlyInAnyOrder(first.id(), second.id());

        verify(repository).update(argThat(after -> after.id().equals(first.id()) && after.managerId() == null
                && after.version() == 2 && after.updatedAt().equals(NOW)), org.mockito.ArgumentMatchers.eq(1L));
        verify(repository).update(argThat(after -> after.id().equals(second.id()) && after.managerId() == null),
                org.mockito.ArgumentMatchers.eq(1L));
        verify(audit, times(2)).record(argThat((AuditEvent event) -> event.action().equals("SUBJECT_MANAGER_CHANGED")
                && event.actorId().equals(admin.accountId()) && manager.equals(event.details().get("oldManagerId"))));
    }

    @Test
    void releaseSkipsSubjectReassignedBeforeLockAndFailsOnLostUpdate() {
        UUID manager = UUID.randomUUID();
        Subject subject = Subject.create(UUID.randomUUID(), "SWR302", "A", null, manager, NOW.minusSeconds(60));
        when(repository.findManagedBy(manager)).thenReturn(List.of(subject));
        when(repository.findByIdForUpdate(subject.id())).thenReturn(Optional.of(subject.changeManager(UUID.randomUUID(), NOW)));
        assertThat(service.releaseManagerAssignments(manager, admin)).isEmpty();
        verify(repository, never()).update(any(), anyLong());

        when(repository.findByIdForUpdate(subject.id())).thenReturn(Optional.of(subject));
        when(repository.update(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(() -> service.releaseManagerAssignments(manager, admin))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("SUBJECT_VERSION_CONFLICT"));
        verifyNoInteractions(audit);
    }

    @Test
    void nullIdsFailClosedWithoutQueryingTheDatabase() {
        assertThat(service.isSubjectManager(null, UUID.randomUUID())).isFalse();
        assertThat(service.isSubjectManager(UUID.randomUUID(), null)).isFalse();
        assertThat(service.listManagedSubjects(null)).isEmpty();
        assertThat(service.releaseManagerAssignments(null, admin)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void repositoryFailurePropagatesSoU01CanDenyRatherThanAllowOnDependencyFailure() {
        UUID manager = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(repository.isManagedBy(manager, subject)).thenThrow(failure);
        assertThatThrownBy(() -> service.isSubjectManager(manager, subject)).isSameAs(failure);
    }
}
