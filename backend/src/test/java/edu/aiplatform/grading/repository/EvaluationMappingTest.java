package edu.aiplatform.grading.repository;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.grading.model.*;
import edu.aiplatform.grading.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvaluationMappingTest {
    @Test
    void aggregateMutationsCannotAutoFlushStaleIndexColumns() {
        var aggregate = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "hash", BigDecimal.TEN, "[]", Instant.now());
        var row = new EvaluationJpaEntity();
        row.apply(aggregate);
        row.version = 3L;
        aggregate.feedback = "mutated after save";
        assertThat(row.view().feedback).isNull();
        var loaded = row.view();
        loaded.score = BigDecimal.ONE;
        loaded.state = EvaluationStatus.DRAFT;
        assertThat(loaded.version).isEqualTo(3);
        assertThat(row.view().score).isNull();
        assertThat(row.status).isEqualTo("PENDING");
        row.apply(loaded);
        assertThat(row.score).isEqualByComparingTo("1");
        assertThat(row.status).isEqualTo("DRAFT");
    }

    @Test
    void fractionalScoresAndStructuredFeedbackRoundTripInsideExistingJsonDocumentAndOldRowsDefaultEmpty() throws Exception {
        var aggregate = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "hash", BigDecimal.TEN, "[]", Instant.now());
        var rubric = UUID.randomUUID();
        var item = UUID.randomUUID();
        aggregate.rubricItemScores = Map.of(rubric, Map.of(item, new BigDecimal("0.50")));
        aggregate.structuredFeedback = new GradingFeedback(
                1,
                new GradingFeedback.Summary(List.of("Tốt"), List.of("Cần sửa"), List.of("Bước tiếp")),
                List.of(new GradingFeedback.Item(rubric, item, "Minh chứng", "Lý do", "Gợi ý", "q1", false, null)));
        aggregate.reviewedItemScores = Map.of(rubric, Map.of(item, new BigDecimal("0.50")));
        aggregate.acknowledgedReviewItems = Map.of(rubric, Set.of(item));
        var row = new EvaluationJpaEntity();
        row.apply(aggregate);
        row.version = 1L;
        var viewed = row.view();
        assertThat(viewed.rubricItemScores.get(rubric).get(item)).isEqualByComparingTo("0.50");
        assertThat(viewed.structuredFeedback).isEqualTo(aggregate.structuredFeedback);
        assertThat(viewed.reviewedItemScores.get(rubric).get(item)).isEqualByComparingTo("0.50");
        assertThat(viewed.acknowledgedReviewItems.get(rubric)).containsExactly(item);

        var json = new ObjectMapper();
        var old = (ObjectNode) json.readTree(row.document);
        old.remove("rubricItemScores");
        old.remove("structuredFeedback");
        old.remove("reviewedItemScores");
        old.remove("acknowledgedReviewItems");
        row.document = old.toString();
        var legacyView = row.view();
        assertThat(legacyView.rubricItemScores).isEmpty();
        assertThat(legacyView.structuredFeedback).isNull();
        assertThat(legacyView.reviewedItemScores).isEmpty();
        assertThat(legacyView.acknowledgedReviewItems).isEmpty();
    }

    @Test
    void proposalDocumentStoresImmutableJsonTextAndIgnoresUnknownPropertiesInBothEntities() throws Exception {
        var json = new ObjectMapper().findAndRegisterModules();
        var evaluation = Evaluation.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "hash", BigDecimal.TEN, "[]", Instant.now());
        var evalRow = new EvaluationJpaEntity();
        evalRow.apply(evaluation);
        var evalJson = (ObjectNode) json.readTree(evalRow.document);
        evalJson.put("futureEvaluationField", "ignored-safely");
        evalRow.document = evalJson.toString();
        assertThat(evalRow.view().id).isEqualTo(evaluation.id);

        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.evaluationId = evaluation.id;
        proposal.requesterId = UUID.randomUUID();
        proposal.versionNo = 1;
        proposal.requestRef = "req-1";
        proposal.status = "PENDING";
        proposal.taskState = "QUEUED";
        var proposalRow = new GradingProposalJpaEntity();
        proposalRow.apply(proposal);
        proposal.status = "MUTATED_IN_MEMORY";
        assertThat(proposalRow.view().status).isEqualTo("PENDING");

        var proposalJson = (ObjectNode) json.readTree(proposalRow.document);
        proposalJson.put("futureProposalField", "ignored-safely");
        proposalRow.document = proposalJson.toString();
        assertThat(proposalRow.view().id).isEqualTo(proposal.id);
        assertThat(proposalRow.view().taskState).isEqualTo("QUEUED");
    }
}
