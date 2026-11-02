package edu.aiplatform.grading.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface IGradingProposalJpaRepository extends JpaRepository<GradingProposalJpaEntity, UUID> {
    @Query("select p.evaluationId from GradingProposalJpaEntity p where p.id=:id")
    Optional<UUID> findEvaluationIdById(UUID id);

    Optional<GradingProposalJpaEntity> findByRequestRef(String ref);

    List<GradingProposalJpaEntity> findByEvaluationIdOrderByVersionNoDesc(UUID evaluationId);
}
