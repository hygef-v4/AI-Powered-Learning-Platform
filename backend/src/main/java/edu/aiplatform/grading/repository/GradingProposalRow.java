package edu.aiplatform.grading.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.grading.model.GradingProposal;
import java.util.UUID;

class GradingProposalRow {
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    UUID id;
    UUID evaluationId;
    long versionNo;
    String requestRef;
    String status;
    // JSON text keeps saved snapshots independent of mutable proposals.
    String document;

    void applyProposal(GradingProposal p) {
        id = p.id;
        evaluationId = p.evaluationId;
        versionNo = p.versionNo;
        requestRef = p.requestRef;
        status = p.status;
        try {
            document = objectMapper.writeValueAsString(p);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize grading proposal", ex);
        }
    }

    GradingProposal mapProposal() {
        try {
            return objectMapper.readValue(document, GradingProposal.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot deserialize grading proposal", ex);
        }
    }
}
