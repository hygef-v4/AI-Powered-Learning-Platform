package edu.aiplatform.subjects.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubjectScopeServiceTest {

    private final ISubjectRepository repository = mock(ISubjectRepository.class);
    private final SubjectScopeService service = new SubjectScopeService(repository);

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
    void returnsIdsIncludingArchivedForRoleChangeGuardWithoutCallingAuthorization() {
        UUID manager = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-18T00:00:00Z");
        Subject active = Subject.create(UUID.randomUUID(), "SWR302", "A", null, manager, now);
        Subject archived = Subject.create(UUID.randomUUID(), "SWR303", "B", null, manager, now).archive(now);
        when(repository.findManagedBy(manager)).thenReturn(List.of(active, archived));
        assertThat(service.listManagedSubjects(manager)).containsExactly(active.id(), archived.id());
    }

    @Test
    void nullIdsFailClosedWithoutQueryingTheDatabase() {
        assertThat(service.isSubjectManager(null, UUID.randomUUID())).isFalse();
        assertThat(service.isSubjectManager(UUID.randomUUID(), null)).isFalse();
        assertThat(service.listManagedSubjects(null)).isEmpty();
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
