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

    private final ISubjectRepository subjectRepository = mock(ISubjectRepository.class);
    private final SubjectQueryService subjectQueryService =
            new SubjectQueryService(subjectRepository);
    private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");

    @Test
    void managedQueryMapsSubjectsAndNullableManagerIsRepresented() {
        UUID manager = UUID.randomUUID();
        Subject managed =
                Subject.createSubject(UUID.randomUUID(), "SWR302", "Requirements", null, manager, NOW);
        when(subjectRepository.findSubjectsManagedBy(manager)).thenReturn(List.of(managed));
        assertThat(subjectQueryService.listManagedSubjects(manager))
                .containsExactly(
                        new SubjectRef(
                                managed.id(),
                                "SWR302",
                                "Requirements",
                                SubjectStatus.ACTIVE,
                                manager));
        Subject unassigned = Subject.createSubject(UUID.randomUUID(), "SWR303", "Other", null, null, NOW);
        when(subjectRepository.findById(unassigned.id())).thenReturn(Optional.of(unassigned));
        assertThat(
                subjectQueryService.getSubject(unassigned.id()).orElseThrow().managerId()).isNull();
    }
}
