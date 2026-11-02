package edu.aiplatform.subjects.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubjectQueryServiceTest {

    private final ISubjectRepository repository = mock(ISubjectRepository.class);
    private final SubjectQueryService service = new SubjectQueryService(repository);
    private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");

    @Test
    void returnsOnlyContractFieldsAndMissingSubjectIsEmpty() {
        UUID id = UUID.randomUUID();
        UUID manager = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(Subject.create(id, "swr302", "Requirements", "Private note", manager, NOW)));
        assertThat(service.getSubject(id)).contains(new SubjectRef(id, "SWR302", "Requirements", SubjectStatus.ACTIVE, manager));
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());
        assertThat(service.getSubject(missing)).isEmpty();
        assertThat(service.isActive(missing)).isFalse();
    }

    @Test
    void activeCheckReadsAgainAndDoesNotTreatArchivedAsActive() {
        UUID id = UUID.randomUUID();
        Subject active = Subject.create(id, "SWR302", "Requirements", null, null, NOW);
        when(repository.findById(id)).thenReturn(Optional.of(active), Optional.of(active.archive(NOW)));
        assertThat(service.isActive(id)).isTrue();
        assertThat(service.isActive(id)).isFalse();
    }

    @Test
    void managedQueryMapsSubjectsAndNullableManagerIsRepresented() {
        UUID manager = UUID.randomUUID();
        Subject managed = Subject.create(UUID.randomUUID(), "SWR302", "Requirements", null, manager, NOW);
        when(repository.findManagedBy(manager)).thenReturn(List.of(managed));
        assertThat(service.listManagedSubjects(manager)).containsExactly(new SubjectRef(managed.id(), "SWR302",
                "Requirements", SubjectStatus.ACTIVE, manager));
        Subject unassigned = Subject.create(UUID.randomUUID(), "SWR303", "Other", null, null, NOW);
        when(repository.findById(unassigned.id())).thenReturn(Optional.of(unassigned));
        assertThat(service.getSubject(unassigned.id()).orElseThrow().managerId()).isNull();
    }

    @Test
    void countsUseTheWholeRepositoryWithoutStatusOrSearchFilter() {
        Map<SubjectStatus, Long> counts = Map.of(SubjectStatus.ACTIVE, 0L, SubjectStatus.ARCHIVED, 2L);
        when(repository.countByStatus(null)).thenReturn(counts);
        assertThat(service.countSubjectsByStatus()).containsExactlyInAnyOrderEntriesOf(counts);
        verify(repository).countByStatus(null);
    }

    @Test
    void nullIdsDoNotBecomeQueriesForUnassignedSubjects() {
        assertThat(service.getSubject(null)).isEmpty();
        assertThat(service.isActive(null)).isFalse();
        assertThat(service.listManagedSubjects(null)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void databaseFailureIsNotReturnedAsFabricatedMissingOrActiveData() {
        UUID id = UUID.randomUUID();
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(repository.findById(id)).thenThrow(failure);
        assertThatThrownBy(() -> service.getSubject(id)).isSameAs(failure);
        assertThatThrownBy(() -> service.isActive(id)).isSameAs(failure);
    }
}
