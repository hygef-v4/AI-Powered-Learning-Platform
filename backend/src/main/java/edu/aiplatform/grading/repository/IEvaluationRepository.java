package edu.aiplatform.grading.repository;

import edu.aiplatform.grading.model.Evaluation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEvaluationRepository {
    Optional<Evaluation> findById(UUID id);

    Optional<Evaluation> lockById(UUID id);

    Optional<Evaluation> findByAttemptId(UUID attemptId);

    List<Evaluation> findForAssignmentAndClass(UUID assignmentId, UUID classId);

    List<Evaluation> findForClass(UUID classId);

    Evaluation save(Evaluation evaluation);

    void lockIdentity(UUID classId, UUID assignmentId, UUID subjectId);
}
