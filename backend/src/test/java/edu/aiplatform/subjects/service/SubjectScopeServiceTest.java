package edu.aiplatform.subjects.service;

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
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubjectScopeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");
    private final ISubjectRepository subjectRepository = mock(ISubjectRepository.class);
    private final IAudit audit = mock(IAudit.class);
    private final SubjectScopeService subjectScopeService =
            new SubjectScopeService(
                    subjectRepository, new SubjectAuditRecorder(
                            audit), Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);

    @Test
    void returnsManagedIdsForRoleChangeGuardWithoutCallingAuthorization() {
        UUID manager = UUID.randomUUID();
        Subject first = Subject.createSubject(UUID.randomUUID(), "SWR302", "A", null, manager, NOW);
        Subject second = Subject.createSubject(UUID.randomUUID(), "SWR303", "B", null, manager, NOW);
        when(subjectRepository.findSubjectsManagedBy(manager)).thenReturn(List.of(first, second));
        assertThat(
                subjectScopeService
                        .listManagedSubjects(manager)).containsExactly(first
                        .id(), second
                        .id());
    }

    @Test
    void releaseUnassignsEveryManagedSubjectBumpsVersionAndAuditsEach() {
        UUID manager = UUID.randomUUID();
        Subject first =
                Subject.createSubject(
                        UUID.randomUUID(), "SWR302", "A", null, manager, NOW.minusSeconds(60));
        Subject second =
                Subject.createSubject(
                        UUID.randomUUID(), "SWR303", "B", null, manager, NOW.minusSeconds(60));
        when(subjectRepository.findSubjectsManagedBy(manager)).thenReturn(List.of(first, second));
        when(subjectRepository.findByIdForUpdate(first.id())).thenReturn(Optional.of(first));
        when(subjectRepository.findByIdForUpdate(second.id())).thenReturn(Optional.of(second));
        when(subjectRepository.updateSubject(any(), anyLong())).thenReturn(true);

        assertThat(subjectScopeService.releaseManagerAssignments(manager, admin))
                .containsExactlyInAnyOrder(first.id(), second.id());

        verify(subjectRepository)
                .updateSubject(
                        argThat(
                                after ->
                                        after.id().equals(first.id())
                                                && after.managerId() == null
                                                && after.version() == 2
                                                && after.updatedAt().equals(NOW)),
                        org.mockito.ArgumentMatchers.eq(1L));
        verify(subjectRepository)
                .updateSubject(
                        argThat(
                                after ->
                                        after.id().equals(second.id())
                                                && after.managerId() == null),
                        org.mockito.ArgumentMatchers.eq(1L));
        verify(audit, times(2))
                .record(
                        argThat(
                                (AuditEvent event) ->
                                        event.action().equals("SUBJECT_MANAGER_CHANGED")
                                                && event.actorId().equals(admin.accountId())
                                                && manager.equals(
                                                        event.details().get("oldManagerId"))));
    }

    private List<Subject> managedPair(UUID manager) {
        Subject first = Subject.createSubject(new UUID(0, 1), "FIRST", "A", null, manager, NOW.minusSeconds(60));
        Subject second = Subject.createSubject(new UUID(0, 2), "SECOND", "B", null, manager, NOW.minusSeconds(60));
        when(subjectRepository.findSubjectsManagedBy(manager)).thenReturn(List.of(second, first));
        when(subjectRepository.findByIdForUpdate(first.id())).thenReturn(Optional.of(first));
        when(subjectRepository.findByIdForUpdate(second.id())).thenReturn(Optional.of(second));
        return List.of(first, second);
    }
}
