package edu.aiplatform.grading.repository;

import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.shared.error.ApiException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EvaluationRepositoryAdapter implements IEvaluationRepository {
    private final IEvaluationJpaRepository evaluationJpaRepository;
    private final EntityManager entityManager;

    public EvaluationRepositoryAdapter(IEvaluationJpaRepository evaluationJpaRepository, EntityManager entityManager) {
        this.evaluationJpaRepository = evaluationJpaRepository;
        this.entityManager = entityManager;
    }

    public Optional<Evaluation> findById(UUID id) {
        return evaluationJpaRepository.findById(id).map(EvaluationJpaEntity::mapEvaluation);
    }

    public Optional<Evaluation> lockById(UUID id) {
        return evaluationJpaRepository.lockById(id).map(EvaluationJpaEntity::mapEvaluation);
    }

    public Optional<Evaluation> findByAttemptId(UUID id) {
        return evaluationJpaRepository.findByAttemptId(id).map(EvaluationJpaEntity::mapEvaluation);
    }

    public List<Evaluation> findForAssignmentAndClass(UUID assignmentId, UUID classId) {
        return evaluationJpaRepository.findByAssignmentIdAndClassId(assignmentId, classId).stream().map(EvaluationJpaEntity::mapEvaluation)
                .toList();
    }

    public List<Evaluation> findForClass(UUID classId) {
        return evaluationJpaRepository.findByClassId(classId).stream().map(EvaluationJpaEntity::mapEvaluation).toList();
    }

    public Evaluation save(Evaluation e) {
        var row = evaluationJpaRepository.findById(e.id).orElseGet(EvaluationJpaEntity::new);
        if (row.version != null && row.version != e.version)
            throw ApiException.conflict("GRADE_VERSION_CONFLICT", "Reload evaluation");
        row.applyEvaluation(e);
        var saved = evaluationJpaRepository.saveAndFlush(row);
        e.version = saved.version;
        return saved.mapEvaluation();
    }

    public void lockIdentity(UUID classId, UUID assignmentId, UUID subjectId) {
        // Advisory transaction lock also serializes first submissions before a row
        // exists.
        String key = classId + ":" + assignmentId + ":" + subjectId;
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(cast(:key as text), 0))")
                .setParameter("key", key).getSingleResult();
    }
}
