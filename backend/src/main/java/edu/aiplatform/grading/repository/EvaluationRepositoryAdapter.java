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
    private final IEvaluationJpaRepository repo;
    private final EntityManager em;

    public EvaluationRepositoryAdapter(IEvaluationJpaRepository repo, EntityManager em) {
        this.repo = repo;
        this.em = em;
    }

    public Optional<Evaluation> findById(UUID id) {
        return repo.findById(id).map(EvaluationJpaEntity::view);
    }

    public Optional<Evaluation> lockById(UUID id) {
        return repo.lockById(id).map(EvaluationJpaEntity::view);
    }

    public Optional<Evaluation> findByAttemptId(UUID id) {
        return repo.findByAttemptId(id).map(EvaluationJpaEntity::view);
    }

    public List<Evaluation> findForAssignmentAndClass(UUID assignmentId, UUID classId) {
        return repo.findByAssignmentIdAndClassId(assignmentId, classId).stream().map(EvaluationJpaEntity::view)
                .toList();
    }

    public List<Evaluation> findForClass(UUID classId) {
        return repo.findByClassId(classId).stream().map(EvaluationJpaEntity::view).toList();
    }

    public Evaluation save(Evaluation e) {
        var row = repo.findById(e.id).orElseGet(EvaluationJpaEntity::new);
        if (row.version != null && row.version != e.version)
            throw ApiException.conflict("GRADE_VERSION_CONFLICT", "Reload evaluation");
        row.apply(e);
        var saved = repo.saveAndFlush(row);
        e.version = saved.version;
        return saved.view();
    }

    public void lockIdentity(UUID classId, UUID assignmentId, UUID subjectId) {
        // Advisory transaction lock also serializes first submissions before a row
        // exists.
        String key = classId + ":" + assignmentId + ":" + subjectId;
        em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(cast(:key as text), 0))")
                .setParameter("key", key).getSingleResult();
    }
}
