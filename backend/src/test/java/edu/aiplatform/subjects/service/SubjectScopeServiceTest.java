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
                    subjectRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);

    @Test
    void readsCurrentAssignmentEveryTimeAndDoesNotCacheAnAllowDecision() {
        UUID manager = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        when(subjectRepository.isManagedBy(manager, subject)).thenReturn(true, false);
        assertThat(subjectScopeService.isSubjectManager(manager, subject)).isTrue();
        assertThat(subjectScopeService.isSubjectManager(manager, subject)).isFalse();
        verify(subjectRepository, times(2)).isManagedBy(manager, subject);
    }

    @Test
    void unknownOrUnassignedSubjectDoesNotGrantScope() {
        UUID manager = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(subjectRepository.isManagedBy(manager, missing)).thenReturn(false);
        assertThat(subjectScopeService.isSubjectManager(manager, missing)).isFalse();
    }

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
        verifyNoInteractions(audit);
    }

    @Test
    void releaseSkipsSubjectReassignedBeforeLockAndFailsOnLostUpdate() {
        UUID manager = UUID.randomUUID();
        Subject subject =
                Subject.createSubject(
                        UUID.randomUUID(), "SWR302", "A", null, manager, NOW.minusSeconds(60));
        when(subjectRepository.findSubjectsManagedBy(manager)).thenReturn(List.of(subject));
        when(subjectRepository.findByIdForUpdate(subject.id()))
                .thenReturn(Optional.of(subject.changeManager(UUID.randomUUID(), NOW)));
        assertThat(subjectScopeService.releaseManagerAssignments(manager, admin)).isEmpty();
        verify(subjectRepository, never()).updateSubject(any(), anyLong());

        when(subjectRepository.findByIdForUpdate(subject.id())).thenReturn(Optional.of(subject));
        when(subjectRepository.updateSubject(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(() -> subjectScopeService.releaseManagerAssignments(manager, admin))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        error -> assertThat(error.readCode()).isEqualTo("SUBJECT_VERSION_CONFLICT"));
        verifyNoInteractions(audit);
    }

    @Test
    void releasesLockInIdOrderAndSecondRepositoryFailurePropagates() {
        UUID manager = UUID.randomUUID();
        List<Subject> subjects = managedPair(manager);
        var failure = new IllegalStateException("second update unavailable");
        when(subjectRepository.updateSubject(any(), anyLong())).thenAnswer(call -> {
            Subject value = call.getArgument(0);
            if (value.id().equals(subjects.get(1).id())) throw failure;
            return true;
        });

        assertThatThrownBy(
                () -> subjectScopeService.releaseManagerAssignments(
                        manager, admin)).isSameAs(failure);

        var order = org.mockito.Mockito.inOrder(subjectRepository);
        order.verify(subjectRepository).findSubjectsManagedBy(manager);
        order.verify(subjectRepository).findByIdForUpdate(subjects.get(0).id());
        order
                .verify(subjectRepository)
                .updateSubject(argThat(s -> s.id().equals(subjects.get(0).id())), anyLong());
        order.verify(subjectRepository).findByIdForUpdate(subjects.get(1).id());
        order
                .verify(subjectRepository)
                .updateSubject(argThat(s -> s.id().equals(subjects.get(1).id())), anyLong());
        verifyNoInteractions(audit);
    }

    private List<Subject> managedPair(UUID manager) {
        Subject first = Subject.createSubject(new UUID(0, 1), "FIRST", "A", null, manager, NOW.minusSeconds(60));
        Subject second = Subject.createSubject(new UUID(0, 2), "SECOND", "B", null, manager, NOW.minusSeconds(60));
        when(subjectRepository.findSubjectsManagedBy(manager)).thenReturn(List.of(second, first));
        when(subjectRepository.findByIdForUpdate(first.id())).thenReturn(Optional.of(first));
        when(subjectRepository.findByIdForUpdate(second.id())).thenReturn(Optional.of(second));
        return List.of(first, second);
    }

    @Test
    void nullIdsFailClosedWithoutQueryingTheDatabase() {
        assertThat(subjectScopeService.isSubjectManager(null, UUID.randomUUID())).isFalse();
        assertThat(subjectScopeService.isSubjectManager(UUID.randomUUID(), null)).isFalse();
        assertThat(subjectScopeService.listManagedSubjects(null)).isEmpty();
        assertThat(subjectScopeService.releaseManagerAssignments(null, admin)).isEmpty();
        verifyNoInteractions(subjectRepository);
    }

    @Test
    void repositoryFailurePropagatesSoU01CanDenyRatherThanAllowOnDependencyFailure() {
        UUID manager = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(subjectRepository.isManagedBy(manager, subject)).thenThrow(failure);
        assertThatThrownBy(
                () -> subjectScopeService.isSubjectManager(manager, subject)).isSameAs(failure);
    }
}
