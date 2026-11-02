package edu.aiplatform.subjects.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;

class SubjectMutationGuardTest {
    private final ISubjectRepository repository = mock(ISubjectRepository.class);
    private final SubjectMutationGuard guard = new SubjectMutationGuard(repository);
    private final UUID id = UUID.randomUUID();

    @Test
    void activeReturnsContractRefUsingBoundedLock() {
        Subject subject = subject();
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenReturn(Optional.of(subject));
        var ref = guard.lockActive(id, Duration.ofSeconds(5));
        assertThat(ref.subjectId()).isEqualTo(id);
        verify(repository).findByIdForUpdate(eq(id), argThat(value -> !value.isZero() && !value.isNegative()
                && value.compareTo(Duration.ofSeconds(1)) <= 0));
    }

    @Test
    void archivedAndMissingCannotMutate() {
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenReturn(Optional.empty());
        code(() -> guard.lockActive(id, Duration.ofSeconds(1)), "SUBJECT_NOT_FOUND");
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenReturn(Optional.of(subject().archive(Instant.now())));
        code(() -> guard.lockActive(id, Duration.ofSeconds(1)), "SUBJECT_ARCHIVED");
    }

    @Test
    void nullIdOrInvalidBudgetDoesNotReadDatabase() {
        code(() -> guard.lockActive(null, Duration.ofSeconds(1)), "SUBJECT_ID_REQUIRED");
        code(() -> guard.lockActive(id, Duration.ZERO), "SUBJECT_TIMEOUT_INVALID");
        code(() -> guard.lockActive(id, null), "SUBJECT_TIMEOUT_INVALID");
        verifyNoInteractions(repository);
    }

    @Test
    void lockErrorIsFailClosed() {
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenThrow(new CannotAcquireLockException("SQL"));
        code(() -> guard.lockActive(id, Duration.ofSeconds(1)), "SUBJECT_LIFECYCLE_UNAVAILABLE");
    }

    private Subject subject() { return Subject.create(id, "ABC", "Name", null, null, Instant.now()); }
    private void code(Runnable call, String expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo(expected));
    }
}
