package edu.aiplatform.grading.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface IEvaluationJpaRepository extends JpaRepository<EvaluationJpaEntity, UUID> {
    Optional<EvaluationJpaEntity> findByAttemptId(UUID attemptId);

    List<EvaluationJpaEntity> findByAssignmentIdAndClassId(UUID assignmentId, UUID classId);

    List<EvaluationJpaEntity> findByClassId(UUID classId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EvaluationJpaEntity e where e.id=:id")
    Optional<EvaluationJpaEntity> lockById(@Param("id") UUID id);
}
