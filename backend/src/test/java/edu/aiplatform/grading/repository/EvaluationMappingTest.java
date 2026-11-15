package edu.aiplatform.grading.repository;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.grading.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvaluationMappingTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(ContributionLevel.class)
    void contributionLevelsRoundTripAndOldDocumentsDefaultUnassessed(ContributionLevel level)
            throws Exception {
        var member = Evaluation.createPendingEvaluation(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "hash", BigDecimal.TEN, "[]", Instant.EPOCH);
        member.kind = EvaluationKind.MEMBER;
        member.contributionLevel = level;
        member.memberOverride = true;
        member.adjustmentReason = "Hoàn thành phần API";
        var row = new EvaluationRow();
        row.applyEvaluation(member);
        assertThat(row.mapEvaluation().contributionLevel).isEqualTo(level);
        assertThat(row.mapEvaluation().adjustmentReason).isEqualTo("Hoàn thành phần API");
        var legacy = (ObjectNode) new ObjectMapper().readTree(row.document);
        legacy.remove("contributionLevel");
        row.document = legacy.toString();
        assertThat(row.mapEvaluation().contributionLevel).isNull();
    }
    @Test
    void aggregateMutationsCannotAutoFlushStaleIndexColumns() {
        var aggregate = Evaluation.createPendingEvaluation(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "hash", BigDecimal.TEN, "[]", Instant.now());
        var row = new EvaluationRow();
        row.applyEvaluation(aggregate);
        row.version = 3L;
        aggregate.feedback = "mutated after save";
        assertThat(row.mapEvaluation().feedback).isNull();
        var loaded = row.mapEvaluation();
        loaded.score = BigDecimal.ONE;
        loaded.state = EvaluationStatus.DRAFT;
        assertThat(loaded.version).isEqualTo(3);
        assertThat(row.mapEvaluation().score).isNull();
        assertThat(row.status).isEqualTo("PENDING");
        row.applyEvaluation(loaded);
        assertThat(row.score).isEqualByComparingTo("1");
        assertThat(row.status).isEqualTo("DRAFT");
    }

    @Test
    void fractionalScoresAndStructuredFeedbackRoundTripInsideExistingJsonDocumentAndOldRowsDefaultEmpty()
            throws Exception {
        var aggregate = Evaluation.createPendingEvaluation(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "hash", BigDecimal.TEN, "[]", Instant.now());
        var rubric = UUID.randomUUID();
        var item = UUID.randomUUID();
        aggregate.rubricItemScores = Map.of(rubric, Map.of(item, new BigDecimal("0.50")));
        aggregate.structuredFeedback = new GradingFeedback(1,
                new GradingFeedback.Summary(List.of("Tốt"), List.of("Cần sửa"),
                        List.of("Bước tiếp")),
                List.of(new GradingFeedback.Item(rubric, item, "Minh chứng", "Lý do", "Gợi ý", "q1",
                        false, null)));
        aggregate.reviewedItemScores = Map.of(rubric, Map.of(item, new BigDecimal("0.50")));
        aggregate.acknowledgedReviewItems = Map.of(rubric, Set.of(item));
        var row = new EvaluationRow();
        row.applyEvaluation(aggregate);
        row.version = 1L;
        var viewed = row.mapEvaluation();
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
        var legacyView = row.mapEvaluation();
        assertThat(legacyView.rubricItemScores).isEmpty();
        assertThat(legacyView.structuredFeedback).isNull();
        assertThat(legacyView.reviewedItemScores).isEmpty();
        assertThat(legacyView.acknowledgedReviewItems).isEmpty();
    }

    @Test
    void proposalDocumentStoresImmutableJsonTextAndIgnoresUnknownPropertiesInBothEntities()
            throws Exception {
        var json = new ObjectMapper().findAndRegisterModules();
        var evaluation = Evaluation.createPendingEvaluation(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "hash", BigDecimal.TEN, "[]", Instant.now());
        var evalRow = new EvaluationRow();
        evalRow.applyEvaluation(evaluation);
        var evalJson = (ObjectNode) json.readTree(evalRow.document);
        evalJson.put("futureEvaluationField", "ignored-safely");
        evalRow.document = evalJson.toString();
        assertThat(evalRow.mapEvaluation().id).isEqualTo(evaluation.id);

        var proposal = new GradingProposal();
        proposal.id = UUID.randomUUID();
        proposal.evaluationId = evaluation.id;
        proposal.requesterId = UUID.randomUUID();
        proposal.versionNo = 1;
        proposal.requestRef = "req-1";
        proposal.status = "PENDING";
        proposal.taskState = "QUEUED";
        var proposalRow = new GradingProposalRow();
        proposalRow.applyProposal(proposal);
        proposal.status = "MUTATED_IN_MEMORY";
        assertThat(proposalRow.mapProposal().status).isEqualTo("PENDING");

        var proposalJson = (ObjectNode) json.readTree(proposalRow.document);
        proposalJson.put("futureProposalField", "ignored-safely");
        proposalRow.document = proposalJson.toString();
        assertThat(proposalRow.mapProposal().id).isEqualTo(proposal.id);
        assertThat(proposalRow.mapProposal().taskState).isEqualTo("QUEUED");
    }
}
