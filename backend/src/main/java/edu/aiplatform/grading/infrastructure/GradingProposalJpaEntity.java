package edu.aiplatform.grading.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
    // Immutable JSON text avoids false dirty checks of mutable proposal snapshots during polling.
    String document;

    protected GradingProposalJpaEntity() {
    }

    void apply(GradingProposal p) {
        id = p.id;
        evaluationId = p.evaluationId;
        versionNo = p.versionNo;
        requestRef = p.requestRef;
        status = p.status;
        try {
            document = JSON.writeValueAsString(p);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize grading proposal", ex);
        }
    }

    GradingProposal view() {
        try {
            return JSON.readValue(document, GradingProposal.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot deserialize grading proposal", ex);
        }
    }
}
