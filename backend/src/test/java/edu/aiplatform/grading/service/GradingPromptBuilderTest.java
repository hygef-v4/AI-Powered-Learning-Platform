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
        assertThat(checker.validateValue(schema, payload)).isEmpty();
        var wrongTotal = payload.deepCopy().put("score", "9.50");
        assertThat(checker.validateValue(schema, wrongTotal)).isPresent();
        var contradictory = payload.deepCopy();
        contradictory.putObject("checks").putArray(rubricId.toString()).add(zeroItem.toString());
        assertThat(checker.validateValue(schema, contradictory)).isPresent();
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
    // "0.001" không còn ở đây: từ G4 (2026-11-20) điểm dư chữ số thập phân trong khoảng hợp lệ được làm tròn.
    @org.junit.jupiter.params.provider.ValueSource(strings = {"-0.01", "1.01", "1.006", "missing", "unknown"})
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
    @Test
    void blankEvidenceIsFlaggedForTeacherReviewInsteadOfRejectingTheProposal() throws Exception {
        UUID rubricId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        var rubrics = List.of(new RubricView(rubricId, UUID.randomUUID(), "part-1",
                List.of(new RubricCriterion(UUID.randomUUID(), "Design", "", CriterionKind.TEXT,
                        List.of(new RubricItem(itemId, "Item", new BigDecimal("2.00"))))),
                new BigDecimal("2.00")));
        JsonNode normalized = GradingPromptBuilder.sanitizeAndNormalizeProposal(json.readTree("""
                {"checks":{"%s":[]},"itemScoresByRubric":{"%s":{"%s":0}},"score":"0.00","feedback":"ok","sections":[],"structuredFeedback":{"schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},"items":[{"rubricId":"%s","itemId":"%s","evidence":"  ","reason":"Why","suggestion":"Fix","evidenceBlockId":null,"requiresReview":false,"reviewReason":null}]}}
                """.formatted(rubricId, rubricId, itemId, rubricId, itemId)), rubrics, Set.of("part-1"), json);

        JsonNode item = normalized.at("/structuredFeedback/items/0");
        assertThat(item.path("evidence").isNull()).isTrue();
        assertThat(item.path("requiresReview").asBoolean()).isTrue();
        assertThat(item.path("reviewReason").asText()).isNotBlank();
        new GradingProposalValidator(json).validateProposal(normalized, rubrics, Set.of("part-1"));
    }

    @Test
    void gradingInstructionsUseAssignmentContextWithinTheLockedRubric() {
        assertThat(new GradingPromptBuilder(json).buildGradingInstructions())
                .contains("Use the assignment to interpret the rubric",
                        "Mention unmet assignment-only requirements in the overall feedback",
                        "without deducting points or setting requiresReview for them",
                        "A different system or topic earns zero for topic-dependent items",
                        "A clearly observed mismatch is sufficient evidence for the score",
                        "When a rubric item assesses a trigger",
                        "A populated Trigger field for a different use case earns zero",
                        "Apply topic-independent rubric items as written",
                        "Absent notation earns no credit unless the rubric says it is optional")
                .doesNotContain("even if the rubric item only asks whether that element exists");
    }

    @Test
    void gradingInstructionsCoverDiagramTypeNotationAmbiguityAndBlockEvidence() {
        // Kế hoạch 2026-11-20 giai đoạn 1–2.
        assertThat(new GradingPromptBuilder(json).buildGradingInstructions())
                .contains("TYPE is guessed by the system from shapes and may be OTHER or wrong",
                        "COMET/UML or Wiegers", "never add notation criteria the rubric does not state",
                        "'hình X trong Y' means shape X lies inside shape Y", "'…(đã cắt)' it is incomplete",
                        "'[block <id>]' line directly above the cited text",
                        "Never paste '[block <id>]' lines or raw diagram-summary lines as evidence",
                        "names, actors and elements match what the rubric requires",
                        "a solid <<include>> or <<extend>> edge does not satisfy a dashed-line requirement",
                        "Full credit requires evidence for every mandatory condition",
                        "If the rubric makes an element optional or conditional",
                        "Set requiresReview=true only when the available evidence is insufficient",
                        "A missing point split alone does not require review",
                        "another part of the submission")
                .doesNotContain("If absence or partial credit is ambiguous, set requiresReview=true");
    }

    @Test
    void gradingInstructionsTieEachDeductionToItsOwnRubricItemAndEvidence() {
        assertThat(new GradingPromptBuilder(json).buildGradingInstructions())
                .contains("Read evidence from that item's rubric part",
                        "Do not carry a defect into another item unless it violates that item's own requirement",
                        "A reason that denies all credit must have numeric score 0",
                        "Award full points when the cited evidence satisfies all stated requirements",
                        "Award each explicitly allocated partial score independently",
                        "award justified partial credit based on the extent of proven fulfillment",
                        "Recognize equivalent behavior even without the exact label",
                        "Add the earned points for the separately scored parts",
                        "Write that sum as the item score in itemScoresByRubric",
                        "Use zero for a split item only when none of its parts earn points",
                        "a required count of flows defines full credit",
                        "selecting one date and time slot and completing that booking demonstrates a one-time flow",
                        "A rule mentioning recurrence is not a recurring flow",
                        "A guarded break stops only its own branch");
    }

    @Test
    void itemScoresWithExtraDecimalsAreRoundedOnlyInsideTheValidRange() throws Exception {
        // G4 (2026-11-20): Gemini trả 0.125 (nửa của 0.25) và 0.05000000000000001; trước đây cả đề xuất bị loại.
        UUID rubricId = UUID.randomUUID();
        UUID half = UUID.randomUUID();
        UUID noisy = UUID.randomUUID();
        UUID tooHigh = UUID.randomUUID();
        var rubrics = List.of(new RubricView(rubricId, UUID.randomUUID(), "part-1",
                List.of(new RubricCriterion(UUID.randomUUID(), "Design", "", CriterionKind.TEXT,
                        List.of(new RubricItem(half, "Half", new BigDecimal("0.25")),
                                new RubricItem(noisy, "Noisy", new BigDecimal("0.10")),
                                new RubricItem(tooHigh, "Too high", new BigDecimal("0.25"))))),
                new BigDecimal("0.60")));
        String items = """
                {"rubricId":"%1$s","itemId":"%2$s","evidence":"Obs","reason":"Why","suggestion":"Fix","evidenceBlockId":null,"requiresReview":false,"reviewReason":null}""";
        JsonNode normalized = GradingPromptBuilder.sanitizeAndNormalizeProposal(json.readTree("""
                {"itemScoresByRubric":{"%s":{"%s":0.125,"%s":0.05000000000000001,"%s":0.2549}},"feedback":"ok","sections":[],"structuredFeedback":{"schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},"items":[%s,%s,%s]}}
                """.formatted(rubricId, half, noisy, tooHigh, items.formatted(rubricId, half),
                        items.formatted(rubricId, noisy), items.formatted(rubricId, tooHigh))), rubrics, Set.of("part-1"),
                json);

        JsonNode scores = normalized.path("itemScoresByRubric").path(rubricId.toString());
        assertThat(scores.path(half.toString()).decimalValue()).isEqualByComparingTo("0.13");
        assertThat(scores.path(noisy.toString()).decimalValue()).isEqualByComparingTo("0.05");
        assertThat(scores.path(tooHigh.toString()).decimalValue()).isEqualByComparingTo("0.25");
        assertThat(normalized.path("score").asText()).isEqualTo("0.43");
        new GradingProposalValidator(json).validateProposal(normalized, rubrics, Set.of("part-1"));

        JsonNode outOfRange = GradingPromptBuilder.sanitizeAndNormalizeProposal(json.readTree("""
                {"itemScoresByRubric":{"%s":{"%s":0.256,"%s":0,"%s":0}},"feedback":"ok","sections":[],"structuredFeedback":{"schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},"items":[%s,%s,%s]}}
                """.formatted(rubricId, half, noisy, tooHigh, items.formatted(rubricId, half),
                        items.formatted(rubricId, noisy), items.formatted(rubricId, tooHigh))), rubrics, Set.of("part-1"),
                json);
        assertThat(outOfRange.at("/itemScoresByRubric/" + rubricId + "/" + half).decimalValue())
                .as("làm tròn ra 0.26 > 0.25 thì giữ nguyên để bị từ chối").isEqualByComparingTo("0.256");
        assertThatThrownBy(() -> new GradingProposalValidator(json).validateProposal(outOfRange, rubrics,
                Set.of("part-1"))).isInstanceOf(ApiException.class);
    }

    @Test
    void unknownOrBlankEvidenceBlockIsDroppedSoTheProposalStillValidates() throws Exception {
        UUID rubricId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        var rubrics = List.of(new RubricView(rubricId, UUID.randomUUID(), "part-1",
                List.of(new RubricCriterion(UUID.randomUUID(), "Design", "", CriterionKind.TEXT,
                        List.of(new RubricItem(itemId, "Item", new BigDecimal("2.00"))))),
                new BigDecimal("2.00")));
        for (String blockId : List.of("\"made-up-block\"", "\"  \"")) {
            JsonNode normalized = GradingPromptBuilder.sanitizeAndNormalizeProposal(json.readTree("""
                    {"checks":{"%s":[]},"itemScoresByRubric":{"%s":{"%s":1.00}},"score":"1.00","feedback":"ok","sections":[],"structuredFeedback":{"schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},"items":[{"rubricId":"%s","itemId":"%s","evidence":"Obs","reason":"Why","suggestion":"Fix","evidenceBlockId":%s,"requiresReview":false,"reviewReason":null}]}}
                    """.formatted(rubricId, rubricId, itemId, rubricId, itemId, blockId)), rubrics, Set.of("part-1"), json);

            JsonNode item = normalized.at("/structuredFeedback/items/0");
            assertThat(item.path("evidenceBlockId").isNull()).isTrue();
            assertThat(item.path("evidence").asText()).isEqualTo("Obs");
            new GradingProposalValidator(json).validateProposal(normalized, rubrics, Set.of("part-1"));
        }
        JsonNode valid = GradingPromptBuilder.sanitizeAndNormalizeProposal(json.readTree("""
                {"checks":{"%s":[]},"itemScoresByRubric":{"%s":{"%s":1.00}},"score":"1.00","feedback":"ok","sections":[],"structuredFeedback":{"schemaVersion":1,"summary":{"strengths":[],"improvements":[],"nextSteps":[]},"items":[{"rubricId":"%s","itemId":"%s","evidence":"Obs","reason":"Why","suggestion":"Fix","evidenceBlockId":"part-1","requiresReview":false,"reviewReason":null}]}}
                """.formatted(rubricId, rubricId, itemId, rubricId, itemId)), rubrics, Set.of("part-1"), json);
        assertThat(valid.at("/structuredFeedback/items/0/evidenceBlockId").asText()).isEqualTo("part-1");
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

    @Test
    void proposalEncodedAsTextAtTheRootIsParsedAsAnObject() {
        var parsed = new GradingProposalValidator(json).parseProposal("\"{\\\"score\\\":\\\"2.00\\\"}\"");
        assertThat(parsed.isObject()).isTrue();
        assertThat(parsed.path("score").asText()).isEqualTo("2.00");
    }
}
