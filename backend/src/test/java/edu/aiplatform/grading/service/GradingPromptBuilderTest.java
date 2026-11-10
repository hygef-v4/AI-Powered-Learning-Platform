package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GradingPromptBuilderTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void providerScoresDeriveTotalAndChecksWithOrWithoutEmptyRubrics(boolean includeEmpty) throws Exception {
        UUID rubricId = UUID.randomUUID();
        UUID zeroItem = UUID.randomUUID();
        UUID partialItem = UUID.randomUUID();
        UUID fullItem = UUID.randomUUID();
        var active = new RubricView(rubricId, UUID.randomUUID(), "active",
                List.of(new RubricCriterion(UUID.randomUUID(), "Design", "", CriterionKind.TEXT,
                        List.of(new RubricItem(zeroItem, "Missing work", new BigDecimal("0.50")),
                                new RubricItem(partialItem, "Partial work", new BigDecimal("0.75")),
                                new RubricItem(fullItem, "Complete work", BigDecimal.ONE)))), new BigDecimal("2.25"));
        var empty = new RubricView(UUID.randomUUID(), active.assignmentId(), "empty",
                List.of(new RubricCriterion(UUID.randomUUID(), "Empty", "", CriterionKind.TEXT,
                        List.of(new RubricItem(UUID.randomUUID(), "Unanswered", BigDecimal.ONE)))), BigDecimal.ONE);
        var builder = new GradingPromptBuilder(json);
        String schema = builder.buildProviderSchema(List.of(active));
        assertThat(json.readTree(schema).path("properties").has("checks")).isFalse();
        assertThat(json.readTree(schema).path("properties").has("score")).isFalse();
        assertThat(json.readTree(builder.buildOutputSchema(List.of(active))).path("properties").has("checks")).isTrue();
        assertThat(json.readTree(builder.buildOutputSchema(List.of(active))).path("properties").has("score")).isTrue();
        var payload = GradingPromptBuilder.createZeroProposal(List.of(active), json);
        payload.remove("checks");
        payload.remove("score");
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload.path("itemScoresByRubric").path(rubricId.toString()))
                .put(partialItem.toString(), new BigDecimal("0.25")).put(fullItem.toString(), BigDecimal.ONE);
        var checker = new edu.aiplatform.ai.service.JsonSchemaChecker(json);
        assertThat(checker.check(schema, payload)).isEmpty();
        var wrongTotal = payload.deepCopy().put("score", "9.50");
        assertThat(checker.check(schema, wrongTotal)).isPresent();
        var contradictory = payload.deepCopy();
        contradictory.putObject("checks").putArray(rubricId.toString()).add(zeroItem.toString());
        assertThat(checker.check(schema, contradictory)).isPresent();
        contradictory.put("score", "1.25");
        assertThatThrownBy(() -> new GradingProposalValidator(json).validateProposal(
                contradictory, List.of(active), Set.of("active")))
                .isInstanceOf(ApiException.class).hasMessage("Checks must match full-credit item scores");

        var rubrics = includeEmpty ? List.of(active, empty) : List.of(active);
        var merged = GradingPromptBuilder.mergeProposalWithEmptyRubrics(
                payload, rubrics, includeEmpty ? List.of(empty) : List.of(), json);
        var normalized = GradingPromptBuilder.sanitizeAndNormalizeProposal(merged, rubrics, Set.of("active", "empty"), json);
        var validated = new GradingProposalValidator(json).validateProposal(normalized, rubrics, Set.of("active", "empty"));
        assertThat(validated.score()).isEqualByComparingTo("1.25");
        assertThat(normalized.path("score").asText()).isEqualTo("1.25");
        assertThat(validated.checks().get(rubricId)).containsExactly(fullItem);
        if (includeEmpty) {
            assertThat(validated.checks().get(empty.rubricId())).isEmpty();
        }
        assertThat(validated.itemScoresByRubric().get(rubricId)).containsEntry(zeroItem, BigDecimal.ZERO.setScale(2))
                .containsEntry(partialItem, new BigDecimal("0.25"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"-0.01", "1.01", "0.001", "missing", "unknown"})
    void derivedTotalsNeverMakeInvalidItemScoresAcceptable(String invalidScore) {
        UUID rubricId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        var rubrics = List.of(new RubricView(rubricId, UUID.randomUUID(), "part-1",
                List.of(new RubricCriterion(UUID.randomUUID(), "Design", "", CriterionKind.TEXT,
                        List.of(new RubricItem(itemId, "Item", BigDecimal.ONE)))), BigDecimal.ONE));
        var payload = GradingPromptBuilder.createZeroProposal(rubrics, json);
        payload.remove("checks");
        payload.remove("score");
        var scores = (com.fasterxml.jackson.databind.node.ObjectNode)
                payload.path("itemScoresByRubric").path(rubricId.toString());
        if ("missing".equals(invalidScore)) {
            scores.remove(itemId.toString());
        } else if ("unknown".equals(invalidScore)) {
            scores.put(UUID.randomUUID().toString(), BigDecimal.ONE);
        } else {
            scores.put(itemId.toString(), new BigDecimal(invalidScore));
        }
        var normalized = GradingPromptBuilder.sanitizeAndNormalizeProposal(
                payload, rubrics, Set.of("part-1"), json);
        assertThatThrownBy(() -> new GradingProposalValidator(json)
                .validateProposal(normalized, rubrics, Set.of("part-1")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void sanitizeAndNormalizeProposalScalesNumbersSyncsOmittedChecksAndTrimsStrings() throws Exception {
        UUID rubricId = UUID.randomUUID();
        UUID itemFull = UUID.randomUUID();
        UUID itemPartial = UUID.randomUUID();
        var rubrics = List.of(new RubricView(rubricId, UUID.randomUUID(), "part-1",
                List.of(new RubricCriterion(UUID.randomUUID(), "Design", "", CriterionKind.TEXT,
                        List.of(
                                new RubricItem(itemFull, "Full item", new BigDecimal("2.00")),
                                new RubricItem(itemPartial, "Partial item", new BigDecimal("3.00"))))),
                new BigDecimal("5.00")));
        JsonNode raw = json.readTree("""
                {
                  "itemScoresByRubric": {"%s": {"%s": 2, "%s": 1.5}},
                  "score": " 3.50 ",
                  "feedback": "  Nhận xét chung  ",
                  "sections": [{"partId": " part-1 ", "feedback": " Tốt "}],
                  "structuredFeedback": {
                    "schemaVersion": 1,
                    "summary": {"strengths": [" Điểm mạnh "], "improvements": [" Cần sửa "], "nextSteps": []},
                    "items": [
                      {"rubricId": " %s ", "itemId": " %s ", "evidence": " Bằng chứng 1 ", "reason": " Lý do 1 ", "suggestion": null, "evidenceBlockId": " part-1 ", "requiresReview": false, "reviewReason": null},
                      {"rubricId": "%s", "itemId": "%s", "evidence": " Bằng chứng 2 ", "reason": " Lý do 2 ", "suggestion": " Gợi ý 2 ", "evidenceBlockId": "part-1", "requiresReview": false, "reviewReason": null}
                    ]
                  }
                }
                """.formatted(rubricId, itemFull, itemPartial, rubricId, itemFull, rubricId, itemPartial));

        JsonNode normalized = GradingPromptBuilder.sanitizeAndNormalizeProposal(
                raw, rubrics, Set.of("part-1"), json);

        assertThat(normalized.path("checks").path(rubricId.toString()))
                .extracting(JsonNode::asText)
                .containsExactly(itemFull.toString());
        assertThat(normalized.path("score").asText()).isEqualTo("3.50");
        assertThat(normalized.path("feedback").asText()).isEqualTo("Nhận xét chung");
        var validated = new GradingProposalValidator(json)
                .validateProposal(normalized, rubrics, Set.of("part-1"));
        assertThat(validated.score()).isEqualByComparingTo("3.50");
    }

    @Test
    void sanitizeAndNormalizeProposalPreservesInvalidFieldsForValidatorRejection() throws Exception {
        UUID rubricId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        var rubrics = List.of(new RubricView(rubricId, UUID.randomUUID(), "part-1",
                List.of(new RubricCriterion(UUID.randomUUID(), "Design", "", CriterionKind.TEXT,
                        List.of(new RubricItem(itemId, "Item", new BigDecimal("2.00"))))),
                new BigDecimal("2.00")));
        var validator = new GradingProposalValidator(json);

        JsonNode outOfRange = GradingPromptBuilder.sanitizeAndNormalizeProposal(json.readTree("""
                {"checks":{"%s":[]},"itemScoresByRubric":{"%s":{"%s":-1.0}},"score":"-1.00","feedback":"ok","sections":[],"structuredFeedback":{"schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},"items":[{"rubricId":"%s","itemId":"%s","evidence":"Obs","reason":"Why","suggestion":"Fix","evidenceBlockId":"part-1","requiresReview":false,"reviewReason":null}]}}
                """.formatted(rubricId, rubricId, itemId, rubricId, itemId)), rubrics, Set.of("part-1"), json);
        assertThatThrownBy(() -> validator.validateProposal(outOfRange, rubrics, Set.of("part-1")))
                .isInstanceOf(ApiException.class);

        JsonNode mismatchedTotal = GradingPromptBuilder.sanitizeAndNormalizeProposal(json.readTree("""
                {"checks":{"%s":[]},"itemScoresByRubric":{"%s":{"%s":1.00}},"score":"2.00","feedback":"ok","sections":[],"structuredFeedback":{"schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},"items":[{"rubricId":"%s","itemId":"%s","evidence":"Obs","reason":"Why","suggestion":"Fix","evidenceBlockId":"part-1","requiresReview":false,"reviewReason":null}]}}
                """.formatted(rubricId, rubricId, itemId, rubricId, itemId)), rubrics, Set.of("part-1"), json);
        assertThat(mismatchedTotal.path("score").asText()).isEqualTo("2.00");
        assertThatThrownBy(() -> validator.validateProposal(mismatchedTotal, rubrics, Set.of("part-1")))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.getMessage()).isNotBlank());
    }
    @org.junit.jupiter.api.Test
    void mergingBlankRubricsPreservesAiTotalForValidation() throws Exception {
        var rubric = new edu.aiplatform.assignments.contract.RubricView(java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(), "empty", java.util.List.of(), java.math.BigDecimal.ZERO);
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var payload = mapper.readTree("{\"score\":\"99.00\"}");
        var merged = GradingPromptBuilder.mergeProposalWithEmptyRubrics(payload,
                java.util.List.of(rubric), java.util.List.of(rubric), mapper);
        org.assertj.core.api.Assertions.assertThat(merged.path("score").asText()).isEqualTo("99.00");
    }
    @Test
    void normalizationDoesNotRoundAwayAnInvalidFractionalScore() throws Exception {
        var payload = json.readTree("{\"itemScoresByRubric\":{\"r\":{\"i\":2.001}},\"score\":2.001}");
        var normalized = GradingPromptBuilder.sanitizeAndNormalizeProposal(payload, List.of(), Set.of(), json);
        assertThat(normalized.path("itemScoresByRubric").path("r").path("i").decimalValue())
                .isEqualByComparingTo("2.001");
        assertThat(new BigDecimal(normalized.path("score").asText())).isEqualByComparingTo("2.001");
    }
    @Test
    void parsingKeepsDecimalPrecisionBeforeValidation() {
        var parsed = new GradingProposalValidator(json).parseProposal("{\"score\":2.0000000000000001}");
        assertThat(parsed.path("score").decimalValue()).isEqualByComparingTo("2.0000000000000001");
    }
}
