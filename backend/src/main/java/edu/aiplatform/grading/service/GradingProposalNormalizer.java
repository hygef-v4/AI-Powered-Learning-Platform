package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.RubricView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Normalizes and merges AI grading proposal JSON payloads without fabricating missing evidence or scores. */
final class GradingProposalNormalizer {
    private static final BigDecimal ZERO_SCALED = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final List<String> SUMMARY_ARRAY_FIELDS = List.of(
            "strengths", "improvements", "nextSteps");
    private static final List<String> ITEM_STRING_FIELDS = List.of(
            "rubricId", "itemId", "evidence", "reason",
            "suggestion", "evidenceBlockId", "reviewReason");
    static final String MISSING_EVIDENCE_REVIEW_REASON =
            "AI không nêu bằng chứng cho mục này; giảng viên cần kiểm tra lại.";
    private static final String MISSING_REVIEW_REASON =
            "AI đánh dấu mục này cần kiểm tra nhưng chưa nêu lý do.";

    // Ngăn tạo đối tượng cho lớp chỉ cung cấp các hàm tĩnh.
    private GradingProposalNormalizer() {
    }

    // Dựng đề xuất điểm 0 đầy đủ khi tài liệu không có phần nào cần gửi AI.
    static ObjectNode createZeroProposal(List<RubricView> rubrics, ObjectMapper objectMapper) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode checks = root.putObject("checks");
        ObjectNode scores = root.putObject("itemScoresByRubric");
        ArrayNode sections = root.putArray("sections");
        Set<String> seenPartIds = new HashSet<>();
        ObjectNode structured = initEmptyStructuredFeedback(root);
        ArrayNode items = structured.putArray("items");
        for (RubricView rubric : rubrics) {
            appendZeroRubricEntries(rubric, checks, scores, sections, seenPartIds, items);
        }
        root.put("score", "0.00");
        root.put("feedback", "Bài làm chưa có nội dung (các phần đều để trống).");
        return root;
    }

    // Tạo phiên bản schema và các mảng rỗng cần thiết cho phản hồi cấu trúc.
    private static ObjectNode initEmptyStructuredFeedback(ObjectNode root) {
        ObjectNode structured = root.putObject("structuredFeedback");
        structured.put("schemaVersion", 1);
        ObjectNode summary = structured.putObject("summary");
        summary.putArray("strengths");
        summary.putArray("improvements").add("Bài làm chưa có nội dung (các phần đều để trống).");
        summary.putArray("nextSteps");
        return structured;
    }

    // Điền điểm 0 cho rubric của mục trống và tránh lặp mục trong phản hồi.
    private static void appendZeroRubricEntries(RubricView rubric, ObjectNode checks,
            ObjectNode scores, ArrayNode sections, Set<String> seenPartIds, ArrayNode items) {
        String rId = rubric.rubricId().toString();
        if (!checks.has(rId)) {
            checks.putArray(rId);
        }
        ObjectNode rubricScores = scores.has(rId) && scores.get(rId).isObject()
                ? (ObjectNode) scores.get(rId)
                : scores.putObject(rId);
        if (seenPartIds.add(rubric.partId())) {
            ObjectNode section = sections.addObject();
            section.put("partId", rubric.partId());
            section.put("feedback", "Phần này sinh viên chưa làm bài (để trống).");
        }
        for (var criterion : rubric.criteria()) {
            for (var item : criterion.items()) {
                appendZeroItemEntry(rId, item, rubricScores, items);
            }
        }
    }

    // Thêm điểm 0 và phản hồi tương ứng cho một tiêu chí thuộc mục trống.
    private static void appendZeroItemEntry(String rId, RubricItem item,
            ObjectNode rubricScores, ArrayNode items) {
        String iId = item.itemId().toString();
        rubricScores.put(iId, ZERO_SCALED);
        ObjectNode itemObj = items.addObject();
        itemObj.put("rubricId", rId);
        itemObj.put("itemId", iId);
        itemObj.put("evidence", "Không có nội dung bài làm (để trống).");
        itemObj.put("reason", "Phần này sinh viên chưa làm bài (để trống).");
        itemObj.put("suggestion", "Cần hoàn thành nội dung cho phần này để đạt điểm.");
        itemObj.putNull("evidenceBlockId");
        itemObj.put("requiresReview", false);
        itemObj.putNull("reviewReason");
    }

    // Sao chép kết quả AI trước khi ghép thêm điểm và phản hồi cho các mục trống.
    static JsonNode mergeProposalWithEmptyRubrics(JsonNode aiPayload,
            List<RubricView> allRubrics, List<RubricView> emptyRubrics, ObjectMapper objectMapper) {
        if (emptyRubrics == null || emptyRubrics.isEmpty()) {
            return aiPayload;
        }
        ObjectNode merged = (ObjectNode) aiPayload.deepCopy();
        ObjectNode checks = ensureObject(merged, "checks");
        ObjectNode scores = ensureObject(merged, "itemScoresByRubric");
        ArrayNode sections = ensureArray(merged, "sections");
        Set<String> existingPartIds = collectExistingPartIds(sections);
        ObjectNode structured = ensureStructuredForMerge(merged);
        ArrayNode items = ensureArray(structured, "items");
        for (RubricView empty : emptyRubrics) {
            appendZeroRubricEntries(empty, checks, scores, sections, existingPartIds, items);
        }
        if (!merged.hasNonNull("score")) {
            merged.put("score", sumRubricScores(allRubrics, scores).toPlainString());
        }
        JsonNode improvementsNode = structured.path("summary").path("improvements");
        if (improvementsNode instanceof ArrayNode impArray && impArray.size() < 5) {
            impArray.add("Các phần để trống đã được tính 0 điểm.");
        }
        return merged;
    }

    // Lập tập ID mục đã có trong đề xuất để bước ghép không thêm trùng.
    private static Set<String> collectExistingPartIds(ArrayNode sections) {
        Set<String> existingPartIds = new HashSet<>();
        for (JsonNode section : sections) {
            existingPartIds.add(section.path("partId").asText(""));
        }
        return existingPartIds;
    }

    // Bổ sung các trường cấu trúc còn thiếu để có thể ghép phản hồi mục trống.
    private static ObjectNode ensureStructuredForMerge(ObjectNode merged) {
        ObjectNode structured = ensureObject(merged, "structuredFeedback");
        if (!structured.has("schemaVersion")) {
            structured.put("schemaVersion", 1);
        }
        if (!structured.has("summary") || !structured.get("summary").isObject()) {
            ObjectNode sum = structured.putObject("summary");
            sum.putArray("strengths");
            sum.putArray("improvements");
            sum.putArray("nextSteps");
        }
        return structured;
    }

    // Tính tổng từ điểm từng mục của rubric thay vì dùng tổng do AI tự cộng.
    private static BigDecimal sumRubricScores(List<RubricView> rubrics, ObjectNode scores) {
        BigDecimal totalScore = ZERO_SCALED;
        if (rubrics == null || scores == null) {
            return totalScore;
        }
        for (RubricView rubric : rubrics) {
            JsonNode rubricScores = scores.path(rubric.rubricId().toString());
            for (var criterion : rubric.criteria()) {
                for (var item : criterion.items()) {
                    JsonNode val = rubricScores.path(item.itemId().toString());
                    if (val.isNumber()) {
                        totalScore = totalScore.add(
                                val.decimalValue().setScale(2, RoundingMode.HALF_UP));
                    }
                }
            }
        }
        return totalScore.setScale(2, RoundingMode.HALF_UP);
    }

    // Chuẩn hóa dữ liệu JSON của đề xuất trước khi kiểm quy tắc theo rubric và bằng chứng.
    static JsonNode sanitizeAndNormalizeProposal(JsonNode payload,
            List<RubricView> rubrics, Set<String> validBlockIds, ObjectMapper objectMapper) {
        if (payload == null || !payload.isObject()) {
            return payload;
        }
        ObjectNode merged = (ObjectNode) payload;
        normalizeScoresAndSyncOmittedChecks(merged, rubrics);
        normalizeExistingChecks(merged);
        normalizeTopLevelScore(merged, rubrics);
        trimTextField(merged, "feedback");
        trimSections(merged);
        trimStructuredFeedback(merged, validBlockIds);
        return merged;
    }

    // Chuẩn hóa bảng điểm và bổ sung danh sách mục được đánh dấu nếu AI bỏ trường đó.
    private static void normalizeScoresAndSyncOmittedChecks(ObjectNode merged,
            List<RubricView> rubrics) {
        if (!(merged.get("itemScoresByRubric") instanceof ObjectNode scores)) {
            return;
        }
        if (rubrics == null || rubrics.isEmpty()) {
            return;
        }
        ObjectNode checks = merged.get("checks") instanceof ObjectNode c ? c : null;
        for (RubricView rubric : rubrics) {
            String rId = rubric.rubricId().toString();
            if (!(scores.get(rId) instanceof ObjectNode rubricScores)) {
                continue;
            }
            roundItemScores(rubric, rubricScores);
            boolean checksOmitted = checks == null || !checks.has(rId) || checks.get(rId).isNull();
            if (checksOmitted) {
                if (checks == null) {
                    checks = merged.putObject("checks");
                }
                syncOmittedRubricChecks(rubric, rubricScores, checks.putArray(rId));
            }
        }
    }

    /**
     * Điểm mục có hơn 2 chữ số thập phân (ví dụ 0.125 là nửa của 0.25, hay 0.05000000000000001 do số thực) được làm
     * tròn HALF_UP khi kết quả vẫn trong [0, tối đa]; ngoài khoảng thì giữ nguyên để validator từ chối (G4, 2026-11-20).
     */
    private static void roundItemScores(RubricView rubric, ObjectNode rubricScores) {
        for (var criterion : rubric.criteria()) {
            for (var item : criterion.items()) {
                String iId = item.itemId().toString();
                JsonNode scoreNode = rubricScores.get(iId);
                if (scoreNode == null || !scoreNode.isNumber() || item.points() == null) {
                    continue;
                }
                BigDecimal rounded = scoreNode.decimalValue().setScale(2, RoundingMode.HALF_UP);
                if (rounded.signum() >= 0 && rounded.compareTo(item.points()) <= 0) {
                    rubricScores.put(iId, rounded);
                }
            }
        }
    }

    // Suy ra mục checklist từ điểm đã có trong rubric khi phải tạo lại checklist.
    private static void syncOmittedRubricChecks(RubricView rubric, ObjectNode rubricScores,
            ArrayNode syncedChecks) {
        for (var criterion : rubric.criteria()) {
            for (var item : criterion.items()) {
                String iId = item.itemId().toString();
                JsonNode scoreNode = rubricScores.get(iId);
                if (scoreNode != null && scoreNode.isNumber()) {
                    BigDecimal scaled = scoreNode.decimalValue();
                    BigDecimal maxPoints = item.points() != null
                            ? item.points()
                            : ZERO_SCALED;
                    if (scaled.compareTo(BigDecimal.ZERO) >= 0 && scaled.compareTo(maxPoints) == 0) {
                        syncedChecks.add(iId);
                    }
                }
            }
        }
    }

    // Chuẩn hóa danh sách mục được đánh dấu ở từng rubric đã có trong payload.
    private static void normalizeExistingChecks(ObjectNode merged) {
        if (!(merged.get("checks") instanceof ObjectNode checks)) {
            return;
        }
        checks.fields().forEachRemaining(entry -> {
            if (entry.getValue() instanceof ArrayNode arr) {
                trimArrayStrings(arr);
            }
        });
    }

    // Chuẩn hóa biểu diễn tổng điểm và suy ra từ điểm từng mục khi thiếu.
    private static void normalizeTopLevelScore(ObjectNode merged, List<RubricView> rubrics) {
        JsonNode scoreNode = merged.get("score");
        if (scoreNode != null && !scoreNode.isNull()) {
            if (scoreNode.isTextual()) {
                merged.put("score", scoreNode.asText().trim());
            } else if (scoreNode.isNumber()) {
                merged.put("score",
                        scoreNode.decimalValue().toPlainString());
            }
            return;
        }
        if (merged.get("itemScoresByRubric") instanceof ObjectNode scores
                && rubrics != null && !rubrics.isEmpty()) {
            merged.put("score", sumRubricScores(rubrics, scores).toPlainString());
        }
    }

    // Bỏ khoảng trắng thừa trong các trường chữ của phản hồi theo mục.
    private static void trimSections(ObjectNode merged) {
        if (!(merged.get("sections") instanceof ArrayNode sections)) {
            return;
        }
        for (JsonNode section : sections) {
            if (section instanceof ObjectNode sectionObj) {
                trimTextField(sectionObj, "partId");
                trimTextField(sectionObj, "feedback");
            }
        }
    }

    // Chuẩn hóa phản hồi cấu trúc và đối chiếu tham chiếu bằng chứng với tập block hợp lệ.
    private static void trimStructuredFeedback(ObjectNode merged, Set<String> validBlockIds) {
        if (!(merged.get("structuredFeedback") instanceof ObjectNode structured)) {
            return;
        }
        if (structured.get("summary") instanceof ObjectNode summary) {
            for (String field : SUMMARY_ARRAY_FIELDS) {
                if (summary.get(field) instanceof ArrayNode arr) {
                    trimArrayStrings(arr);
                }
            }
        }
        if (structured.get("items") instanceof ArrayNode items) {
            for (JsonNode itemNode : items) {
                if (itemNode instanceof ObjectNode itemObj) {
                    for (String field : ITEM_STRING_FIELDS) {
                        trimTextField(itemObj, field);
                    }
                    dropUnknownEvidenceBlock(itemObj, validBlockIds);
                    completeReviewFeedback(itemObj);
                }
            }
        }
    }

    /** Mã block AI chỉ sai hoặc rỗng thì bỏ vị trí đó, giữ lời giải thích, thay vì để cả đề xuất bị từ chối. */
    private static void dropUnknownEvidenceBlock(ObjectNode itemObj, Set<String> validBlockIds) {
        JsonNode blockId = itemObj.get("evidenceBlockId");
        if (blockId != null && blockId.isTextual() && validBlockIds != null
                && (blockId.asText().isBlank() || !validBlockIds.contains(blockId.asText()))) {
            itemObj.putNull("evidenceBlockId");
        }
    }

    /** Adds a review flag or reason when AI leaves review information incomplete. */
    private static void completeReviewFeedback(ObjectNode itemObj) {
        JsonNode review = itemObj.path("requiresReview");
        if (!review.isBoolean()) {
            return;
        }
        boolean missingEvidence = isMissingText(itemObj.path("evidence"));
        if (missingEvidence) {
            itemObj.putNull("evidence");
            itemObj.put("requiresReview", true);
        }
        if ((missingEvidence || review.booleanValue()) && isMissingText(itemObj.path("reviewReason"))) {
            itemObj.put("reviewReason", missingEvidence ? MISSING_EVIDENCE_REVIEW_REASON : MISSING_REVIEW_REASON);
        }
    }

    // Xem trường thiếu, null hoặc chuỗi trắng là chưa có nội dung.
    private static boolean isMissingText(JsonNode value) {
        return value.isMissingNode() || value.isNull() || (value.isTextual() && value.asText().isBlank());
    }

    // Sửa khoảng trắng tại chỗ cho trường JSON kiểu chuỗi.
    private static void trimTextField(ObjectNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value != null && value.isTextual()) {
            node.put(fieldName, value.asText().trim());
        }
    }

    // Chuẩn hóa các phần tử chuỗi trong mảng tại chỗ, giữ các kiểu khác.
    private static void trimArrayStrings(ArrayNode arrayNode) {
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode element = arrayNode.get(i);
            if (element != null && element.isTextual()) {
                arrayNode.set(i, arrayNode.textNode(element.asText().trim()));
            }
        }
    }

    // Lấy object con hiện có hoặc tạo object để ghi trường tiếp theo.
    private static ObjectNode ensureObject(ObjectNode parent, String fieldName) {
        return parent.path(fieldName) instanceof ObjectNode obj ? obj : parent.putObject(fieldName);
    }

    // Lấy mảng con hiện có hoặc tạo mảng để nối phần tử.
    private static ArrayNode ensureArray(ObjectNode parent, String fieldName) {
        return parent.path(fieldName) instanceof ArrayNode arr ? arr : parent.putArray(fieldName);
    }
}
