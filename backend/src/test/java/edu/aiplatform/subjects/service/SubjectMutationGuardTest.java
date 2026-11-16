package edu.aiplatform.subjects.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;

class SubjectMutationGuardTest {
    private final ISubjectRepository subjectRepository = mock(ISubjectRepository.class);
    private final SubjectMutationGuard subjectMutationGuard =
            new SubjectMutationGuard(subjectRepository);
    private final UUID id = UUID.randomUUID();

    @Test
    void activeReturnsContractRefUsingBoundedLock() {
        Subject subject = subject();
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenReturn(Optional.of(subject));
        var ref = subjectMutationGuard.lockActive(id, Duration.ofSeconds(5));
        assertThat(ref.subjectId()).isEqualTo(id);
        verify(subjectRepository)
                .findByIdForUpdate(
                        eq(id),
                        argThat(
                                value ->
                                        !value.isZero()
                                                && !value.isNegative()
                                                && value.compareTo(Duration.ofSeconds(1)) <= 0));
    }

    @Test
    void archivedAndMissingCannotMutate() {
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenReturn(Optional.empty());
        code(() -> subjectMutationGuard.lockActive(id, Duration.ofSeconds(1)), "SUBJECT_NOT_FOUND");
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenReturn(Optional.of(subject().archive(Instant.now())));
        code(() -> subjectMutationGuard.lockActive(id, Duration.ofSeconds(1)), "SUBJECT_ARCHIVED");
    }

    @Test
    void nullIdOrInvalidBudgetDoesNotReadDatabase() {
        code(
                () -> subjectMutationGuard.lockActive(null, Duration.ofSeconds(1)),
                "SUBJECT_ID_REQUIRED");
        code(() -> subjectMutationGuard.lockActive(id, Duration.ZERO), "SUBJECT_TIMEOUT_INVALID");
        code(() -> subjectMutationGuard.lockActive(id, null), "SUBJECT_TIMEOUT_INVALID");
        verifyNoInteractions(subjectRepository);
    }

    @Test
    void lockErrorIsFailClosed() {
        when(subjectRepository.findByIdForUpdate(eq(id), any(Duration.class)))
                .thenThrow(new CannotAcquireLockException("SQL"));
        code(
                () -> subjectMutationGuard.lockActive(id, Duration.ofSeconds(1)),
                "SUBJECT_LIFECYCLE_UNAVAILABLE");
    }

    private Subject subject() {
        return Subject.createSubject(id, "ABC", "Name", null, null, Instant.now());
    }

    private void code(Runnable call, String expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        ApiException.class, error -> assertThat(error.readCode()).isEqualTo(expected));
    }
}
