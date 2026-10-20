package edu.aiplatform.grading.infrastructure;

import edu.aiplatform.grading.domain.GradingProposal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "grading_proposals")
class GradingProposalJpaEntity {
    @Id
    UUID id;
    UUID evaluationId;
    long versionNo;
    String requestRef;
    String status;
    @Version
    Long version;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposal_document", columnDefinition = "jsonb")
    GradingProposal document;

    protected GradingProposalJpaEntity() {
    }
}
