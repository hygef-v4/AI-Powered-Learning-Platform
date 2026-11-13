package edu.aiplatform.grading.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.grading.model.Evaluation;
import java.math.BigDecimal;
import java.util.UUID;

/** Indexed identity/state columns plus the versioned grading document. */
class EvaluationRow {
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
    Long version;
    // JSON text keeps saved snapshots independent of mutable aggregates.
    String document;

    /** Copies aggregate fields and serializes the evaluation document. */
    void applyEvaluation(Evaluation e) {
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
            document = objectMapper.writeValueAsString(e);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize evaluation", ex);
        }
    }

    /** Deserializes the evaluation document and restores its database version. */
    Evaluation mapEvaluation() {
        try {
            var result = objectMapper.readValue(document, Evaluation.class);
            result.version = version == null ? 0 : version;
            return result;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot deserialize evaluation", ex);
        }
    }
}
