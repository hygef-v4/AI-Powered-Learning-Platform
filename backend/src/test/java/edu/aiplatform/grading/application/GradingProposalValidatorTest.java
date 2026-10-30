package edu.aiplatform.grading.application;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class GradingProposalValidatorTest {
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    final UUID rid = UUID.randomUUID(), iid = UUID.randomUUID();

    @Test
    void rejectionDiagnosticsIdentifyValidationStageWithoutEchoingProviderContent() {
        assertThat(GradingProposalValidator.rejectionDiagnostic(
                ApiException.badRequest("INVALID_AI_PROPOSAL", "Unknown evidenceBlockId: private-provider-content")))
                .isEqualTo("FEEDBACK_UNKNOWN_EVIDENCE_BLOCK");
        assertThat(GradingProposalValidator.rejectionDiagnostic(
                ApiException.badRequest("INVALID_AI_PROPOSAL", "AI structuredFeedback must provide feedback for every rubric item")))
                .isEqualTo("FEEDBACK_ITEM_COVERAGE");
        assertThat(GradingProposalValidator.rejectionDiagnostic(
                ApiException.badRequest("INVALID_AI_PROPOSAL", "private-provider-content")))
                .isEqualTo("PROPOSAL_SCHEMA_MISMATCH");
    }

    List<RubricView> rubric() {
        return List.of(new RubricView(
                rid, UUID.randomUUID(), "p", List.of(new RubricCriterion(UUID.randomUUID(), "c", "",
                        CriterionKind.values()[0], List.of(new RubricItem(iid, "i", BigDecimal.ONE)))),
                BigDecimal.ONE));
    }

    @Test
    void acceptsKnownChecklistAndRejectsUnknownIdsAndWrongTotals() {
        var validator = new GradingProposalValidator(json);
        var checks = Map.of(rid.toString(), List.of(iid.toString()));
        assertThat(validator.validate(
                json.valueToTree(Map.of("checks", checks, "score", "1", "feedback", "ok", "sections", List.of())),
                rubric()).score()).isEqualByComparingTo("1");
        assertThatThrownBy(() -> validator.validate(json.valueToTree(Map.of("checks", checks, "score", "2")), rubric()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> validator.validate(
                json.valueToTree(
                        Map.of("checks", Map.of(rid.toString(), List.of(UUID.randomUUID().toString())), "score", "1")),
                rubric())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsDuplicateChecksAndNonObjectPayload() {
        var validator = new GradingProposalValidator(json);
        assertThatThrownBy(
                () -> validator.validate(
                        json.valueToTree(Map.of("checks",
                                Map.of(rid.toString(), List.of(iid.toString(), iid.toString())), "score", "1")),
                        rubric()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> validator.validate(json.valueToTree("not a proposal"), rubric()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validatesFractionalProposalsUsingItemScoresAndRejectsForgedTotals() {
        var validator = new GradingProposalValidator(json);
        var payload = json.valueToTree(Map.of("checks", Map.of(), "score", "0.50", "itemScoresByRubric",
                Map.of(rid.toString(), Map.of(iid.toString(), new BigDecimal("0.50")))));
        assertThat(validator.validate(payload, rubric()).score()).isEqualByComparingTo("0.50");
        ((ObjectNode) payload).put("score", "1.00");
        assertThatThrownBy(() -> validator.validate(payload, rubric())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void validatesStructuredFeedbackForPartialFullAndReviewFlaggedItems() throws Exception {
        var validator = new GradingProposalValidator(json);
        // 1. Valid 0.50/1.00 partial score with evidence and known block anchor
        var partialPayload = json.readTree("""
            {
              "checks": {"%s": []},
              "itemScoresByRubric": {"%s": {"%s": 0.50}},
              "score": "0.50",
              "feedback": "Đạt một nửa yêu cầu",
              "sections": [{"partId": "p", "feedback": "Cần bổ sung khóa ngoại"}],
              "structuredFeedback": {
                "schemaVersion": 1,
                "summary": {
                  "strengths": ["Đã nêu đúng thực thể chính"],
                  "improvements": ["Thiếu ràng buộc khóa ngoại"],
                  "nextSteps": ["Bổ sung khóa ngoại giữa hai bảng"]
                },
                "items": [{
                  "rubricId": "%s",
                  "itemId": "%s",
                  "evidence": "Bảng DonHang chưa có cột MaKH tham chiếu KhachHang",
                  "reason": "Đạt 0.50/1.00 do đã có bảng nhưng thiếu khóa ngoại",
                  "suggestion": "Thêm cột MaKH làm khóa ngoại",
                  "evidenceBlockId": "block-1",
                  "requiresReview": false,
                  "reviewReason": null
                }]
              }
            }
            """.formatted(rid, rid, iid, rid, iid));
        var validatedPartial = validator.validate(partialPayload, rubric(), Set.of("p", "block-1"));
        assertThat(validatedPartial.score()).isEqualByComparingTo("0.50");
        assertThat(validatedPartial.structuredFeedback()).isNotNull();
        assertThat(validatedPartial.structuredFeedback().items()).singleElement().satisfies(item -> {
            assertThat(item.evidenceBlockId()).isEqualTo("block-1");
            assertThat(item.requiresReview()).isFalse();
        });

        // 2. Full marks with optional advice
        var fullMarksPayload = json.readTree("""
            {
              "checks": {"%s": ["%s"]},
              "itemScoresByRubric": {"%s": {"%s": 1.00}},
              "score": "1.00",
              "feedback": "Làm tốt",
              "sections": [],
              "structuredFeedback": {
                "schemaVersion": 1,
                "summary": {
                  "strengths": ["Hoàn thành đầy đủ yêu cầu"],
                  "improvements": [],
                  "nextSteps": ["Gợi ý thêm (không trừ điểm): thử chuẩn hóa tên thuộc tính"]
                },
                "items": [{
                  "rubricId": "%s",
                  "itemId": "%s",
                  "evidence": "Đã trình bày đủ thực thể và khóa chính",
                  "reason": "Đạt tối đa 1.00/1.00 điểm theo tiêu chí",
                  "suggestion": "Gợi ý thêm (không trừ điểm): đặt tên thuộc tính nhất quán theo camelCase",
                  "evidenceBlockId": "p",
                  "requiresReview": false,
                  "reviewReason": null
                }]
              }
            }
            """.formatted(rid, iid, rid, iid, rid, iid));
        var validatedFull = validator.validate(fullMarksPayload, rubric(), Set.of("p", "block-1"));
        assertThat(validatedFull.score()).isEqualByComparingTo("1.00");
        assertThat(validatedFull.structuredFeedback().items().get(0).suggestion())
                .startsWith("Gợi ý thêm (không trừ điểm):");

        // 3. Unreadable evidence / alternative valid design with requiresReview=true and null evidence
        var reviewPayload = json.readTree("""
            {
              "checks": {"%s": []},
              "itemScoresByRubric": {"%s": {"%s": 0.50}},
              "score": "0.50",
              "feedback": "Cần giảng viên xem sơ đồ",
              "sections": [],
              "structuredFeedback": {
                "schemaVersion": 1,
                "summary": {
                  "strengths": [],
                  "improvements": [],
                  "nextSteps": []
                },
                "items": [{
                  "rubricId": "%s",
                  "itemId": "%s",
                  "evidence": null,
                  "reason": "Sơ đồ nhúng dạng ảnh chưa trích xuất được chữ, tạm giữ mức điểm trung gian để giảng viên duyệt",
                  "suggestion": null,
                  "evidenceBlockId": null,
                  "requiresReview": true,
                  "reviewReason": "Minh chứng nằm trong ảnh sơ đồ chưa đọc được hoặc có hướng thiết kế thay thế hợp lệ"
                }]
              }
            }
            """.formatted(rid, rid, iid, rid, iid));
        var validatedReview = validator.validate(reviewPayload, rubric(), Set.of("p"));
        assertThat(validatedReview.structuredFeedback().items().get(0).requiresReview()).isTrue();
    }

    @Test
    void rejectsInvalidBlockIdsDuplicateItemsMissingEvidenceAndUnknownFields() throws Exception {
        var validator = new GradingProposalValidator(json);
        // Unknown block anchor
        var badBlock = json.readTree("""
            {
              "checks": {"%s": []},
              "itemScoresByRubric": {"%s": {"%s": 0.50}},
              "score": "0.50",
              "feedback": "ok",
              "sections": [],
              "structuredFeedback": {
                "schemaVersion": 1,
                "summary": {"strengths": [], "improvements": [], "nextSteps": []},
                "items": [{
                  "rubricId": "%s",
                  "itemId": "%s",
                  "evidence": "Trích dẫn",
                  "reason": "Giải thích",
                  "suggestion": null,
                  "evidenceBlockId": "non-existent-block",
                  "requiresReview": false,
                  "reviewReason": null
                }]
              }
            }
            """.formatted(rid, rid, iid, rid, iid));
        assertThatThrownBy(() -> validator.validate(badBlock, rubric(), Set.of("p", "block-1")))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("INVALID_AI_PROPOSAL");
                    assertThat(GradingProposalValidator.rejectionDiagnostic(ex)).isEqualTo("FEEDBACK_UNKNOWN_EVIDENCE_BLOCK");
                });

        // Missing evidence when requiresReview is false
        var missingEvidence = json.readTree("""
            {
              "checks": {"%s": []},
              "itemScoresByRubric": {"%s": {"%s": 0.50}},
              "score": "0.50",
              "feedback": "ok",
              "sections": [],
              "structuredFeedback": {
                "schemaVersion": 1,
                "summary": {"strengths": [], "improvements": [], "nextSteps": []},
                "items": [{
                  "rubricId": "%s",
                  "itemId": "%s",
                  "evidence": "   ",
                  "reason": "Giải thích",
                  "suggestion": null,
                  "evidenceBlockId": null,
                  "requiresReview": false,
                  "reviewReason": null
                }]
              }
            }
            """.formatted(rid, rid, iid, rid, iid));
        assertThatThrownBy(() -> validator.validate(missingEvidence, rubric(), Set.of("p")))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("INVALID_AI_PROPOSAL");
                    assertThat(GradingProposalValidator.rejectionDiagnostic(ex)).isEqualTo("FEEDBACK_MISSING_EVIDENCE");
                });

        // Duplicate item entry
        var duplicateItems = json.readTree("""
            {
              "checks": {"%s": []},
              "itemScoresByRubric": {"%s": {"%s": 0.50}},
              "score": "0.50",
              "feedback": "ok",
              "sections": [],
              "structuredFeedback": {
                "schemaVersion": 1,
                "summary": {"strengths": [], "improvements": [], "nextSteps": []},
                "items": [
                  {"rubricId": "%s", "itemId": "%s", "evidence": "A", "reason": "B", "suggestion": null, "evidenceBlockId": null, "requiresReview": false, "reviewReason": null},
                  {"rubricId": "%s", "itemId": "%s", "evidence": "A", "reason": "B", "suggestion": null, "evidenceBlockId": null, "requiresReview": false, "reviewReason": null}
                ]
              }
            }
            """.formatted(rid, rid, iid, rid, iid, rid, iid));
        UUID secondItem = UUID.randomUUID();
        var twoItemRubric = List.of(new RubricView(rid, UUID.randomUUID(), "p",
                List.of(new RubricCriterion(UUID.randomUUID(), "c", "", CriterionKind.values()[0],
                        List.of(new RubricItem(iid, "i", BigDecimal.ONE),
                                new RubricItem(secondItem, "second", BigDecimal.ONE)))), new BigDecimal("2")));
        assertThatThrownBy(() -> validator.validate(duplicateItems, twoItemRubric, Set.of("p")))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("INVALID_AI_PROPOSAL");
                    assertThat(GradingProposalValidator.rejectionDiagnostic(ex)).isEqualTo("FEEDBACK_DUPLICATE_ITEM");
                });

        var missingItem = duplicateItems.deepCopy();
        ((com.fasterxml.jackson.databind.node.ArrayNode) missingItem.path("structuredFeedback").path("items")).remove(1);
        assertThatThrownBy(() -> validator.validate(missingItem, twoItemRubric, Set.of("p")))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(GradingProposalValidator.rejectionDiagnostic(ex)).isEqualTo("FEEDBACK_ITEM_COVERAGE"));

        var missingReviewReason = missingEvidence.deepCopy();
        ((ObjectNode) missingReviewReason.path("structuredFeedback").path("items").get(0)).put("requiresReview", true);
        assertThatThrownBy(() -> validator.validate(missingReviewReason, rubric(), Set.of("p")))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(GradingProposalValidator.rejectionDiagnostic(ex)).isEqualTo("FEEDBACK_MISSING_REVIEW_REASON"));

        // Unknown field inside structuredFeedback
        var unknownField = json.readTree("""
            {
              "checks": {"%s": []},
              "itemScoresByRubric": {"%s": {"%s": 0.50}},
              "score": "0.50",
              "feedback": "ok",
              "sections": [],
              "structuredFeedback": {
                "schemaVersion": 1,
                "confidence": 0.95,
                "summary": {"strengths": [], "improvements": [], "nextSteps": []},
                "items": [{"rubricId": "%s", "itemId": "%s", "evidence": "A", "reason": "B", "suggestion": null, "evidenceBlockId": null, "requiresReview": false, "reviewReason": null}]
              }
            }
            """.formatted(rid, rid, iid, rid, iid));
        assertThatThrownBy(() -> validator.validate(unknownField, rubric(), Set.of("p")))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("INVALID_AI_PROPOSAL");
                    assertThat(GradingProposalValidator.rejectionDiagnostic(ex)).isEqualTo("FEEDBACK_UNKNOWN_FIELD");
                });
    }

    @Test
    void feedbackMayBeOmittedButOversizedFeedbackAndSectionsAreRejected() {
        var validator = new GradingProposalValidator(json);
        ObjectNode payload = json.valueToTree(Map.of("checks", Map.of(), "score", "0.00"));
        assertThat(validator.validate(payload, rubric()).feedback()).isNull();
        payload.put("feedback", "x".repeat(10_000));
        assertThat(validator.validate(payload, rubric()).feedback()).hasSize(10_000);
        payload.put("feedback", "x".repeat(10_001));
        assertThatThrownBy(() -> validator.validate(payload, rubric()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("INVALID_AI_PROPOSAL"));
        payload.remove("feedback");
        payload.set("sections", json.valueToTree(List.of(Map.of("partId", "p", "feedback", "x".repeat(10_001)))));
        assertThatThrownBy(() -> validator.validate(payload, rubric()))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(GradingProposalValidator.rejectionDiagnostic(ex)).isEqualTo("PROPOSAL_SECTION_MISMATCH"));
    }
}
