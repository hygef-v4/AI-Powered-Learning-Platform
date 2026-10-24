package edu.aiplatform.grading.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.aiplatform.grading.domain.Evaluation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Indexed identity/state columns plus the versioned grading document. */
@Entity
@Table(name = "evaluations")
class EvaluationJpaEntity {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    @Id
    UUID id;
    UUID assignmentId;
    UUID classId;
    UUID attemptId;
    UUID groupDocumentId;
    UUID accountId;
    String kind;
    String status;
    String submissionHash;
    BigDecimal score;
    BigDecimal maxScore;
    @Version
    Long version;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grading_document", columnDefinition = "jsonb")
    // Immutable JSON text avoids false dirty checks of mutable aggregate snapshots.
    String document;

    protected EvaluationJpaEntity() {
    }

    void apply(Evaluation e) {
        id = e.id;
        assignmentId = e.assignmentId;
        classId = e.classId;
        attemptId = e.attemptId;
        groupDocumentId = e.groupDocumentId;
        accountId = e.accountId;
        kind = e.kind.name();
        status = e.state.name();
        submissionHash = e.submissionHash;
        score = e.score;
        maxScore = e.maxScore;
        try {
            document = JSON.writeValueAsString(e);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize evaluation", ex);
        }
    }

    Evaluation view() {
        try {
            var result = JSON.readValue(document, Evaluation.class);
            result.version = version == null ? 0 : version;
            return result;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot deserialize evaluation", ex);
        }
    }
}
