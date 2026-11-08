package edu.aiplatform.grading.repository;

import edu.aiplatform.grading.model.Evaluation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEvaluationRepository {
    /** Finds an evaluation by its identifier. */
    Optional<Evaluation> findById(UUID id);

    /** Locks and loads an evaluation by its identifier. */
    Optional<Evaluation> lockById(UUID id);

    /** Finds the evaluation for a submission attempt. */
    Optional<Evaluation> findByAttemptId(UUID attemptId);

    /** Lists evaluations for an assignment in a class. */
    List<Evaluation> findForAssignmentAndClass(UUID assignmentId, UUID classId);

    /** Lists evaluations in a class. */
    List<Evaluation> findForClass(UUID classId);

    /** Persists the evaluation and returns its stored state. */
    Evaluation save(Evaluation evaluation);

    /** Locks the identity used to coordinate evaluations for a submission subject. */
    void lockIdentity(UUID classId, UUID assignmentId, UUID subjectId);
}
