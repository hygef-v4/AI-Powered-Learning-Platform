package edu.aiplatform.assignmentwork.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Định dạng phản hồi Practice (BR-U08-42): nhận xét chung, điểm mạnh, nhận xét từng phần, gợi ý; luôn kèm nhãn "AI ước
 * tính, không phải điểm chính thức". Từ 2026-11-27 AI ước tính điểm từng phần theo rubric (chỉ trả điểm phần và nhận xét,
 * không liệt kê từng tiêu chí); server giới hạn điểm trong rubric và tính tổng ({@link #applyRubricScores}). Sai định
 * dạng thì không lưu (BR-U08-43).
 */
final class FeedbackFormat {

    static final String LABEL = "AI ước tính, không phải điểm chính thức";
    /** Không có phần ước tính theo rubric: người học không được xem rubric (yêu cầu 2026-10-21 thay BR-U08-42). */
    static final String OUTPUT_SCHEMA = """
            {"type":"object","required":["summary","parts","suggestions"],"additionalProperties":false,
             "properties":{
              "summary":{"type":"string"},
              "strengths":{"type":"array","items":{"type":"string"}},
              "parts":{"type":"array","items":{"type":"object","required":["partId","comment"],
               "additionalProperties":false,"properties":{"partId":{"type":"string"},"comment":{"type":"string"},
                "score":{"type":"number"}}}},
              "suggestions":{"type":"array","items":{"type":"string"}}}}
            """;
    private static final int MAX_TEXT = 4_000;
    private static final int MAX_ITEMS = 200;

    private FeedbackFormat() {
    }

    /** Chuẩn hóa; {@code null} nếu thiếu phần bắt buộc. Phần rubric, điểm ước tính (nếu có) bị bỏ. */
    static ObjectNode normalize(JsonNode raw) {
        if (raw == null || !raw.isObject() || !raw.path("summary").isTextual() || !raw.path("parts").isArray()
                || !raw.path("suggestions").isArray()) {
            return null;
        }
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.put("label", LABEL);
        out.put("summary", cut(raw.path("summary").asText()));
        copyStrings(raw.path("strengths"), out.putArray("strengths"));
        copyStrings(raw.path("suggestions"), out.putArray("suggestions"));
        ArrayNode parts = out.putArray("parts");
        for (JsonNode part : raw.path("parts")) {
            if (parts.size() >= MAX_ITEMS || !part.path("partId").isTextual()) {
                continue;
            }
            ObjectNode entry = parts.addObject().put("partId", cut(part.path("partId").asText()))
                    .put("comment", cut(part.path("comment").asText("")));
            if (part.path("score").isNumber()) {
                entry.put("score", part.path("score").decimalValue());
            }
        }
        return out;
    }

    /**
     * Điểm theo rubric (yêu cầu 2026-11-27): phần có rubric tính điểm thì điểm AI được giới hạn trong [0, điểm tối đa]
     * và ghi kèm {@code maxScore}; phần thiếu điểm được tính 0. Phần không có rubric tính điểm thì bỏ điểm. Tổng
     * {@code score}/{@code maxScore} chỉ có khi bài có rubric tính điểm.
     */
    static ObjectNode applyRubricScores(ObjectNode feedback, Map<String, BigDecimal> maxByPart) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        Map<String, ObjectNode> byPart = new LinkedHashMap<>();
        for (JsonNode part : feedback.path("parts")) {
            if (part instanceof ObjectNode node) {
                byPart.putIfAbsent(node.path("partId").asText(), node);
            }
        }
        for (JsonNode part : feedback.path("parts")) {
            if (part instanceof ObjectNode node && byPart.get(node.path("partId").asText()) != node) {
                node.remove(List.of("score", "maxScore"));
            }
        }
        for (Map.Entry<String, BigDecimal> rubric : maxByPart.entrySet()) {
            BigDecimal partMax = rubric.getValue();
            if (partMax == null || partMax.signum() <= 0) {
                continue;
            }
            ObjectNode part = byPart.get(rubric.getKey());
            if (part == null) {
                part = ((ArrayNode) feedback.withArray("parts")).addObject().put("partId", rubric.getKey()).put("comment", "");
                byPart.put(rubric.getKey(), part);
            }
            BigDecimal score = part.path("score").isNumber() ? part.path("score").decimalValue() : BigDecimal.ZERO;
            score = score.max(BigDecimal.ZERO).min(partMax).setScale(2, RoundingMode.HALF_UP);
            part.put("score", score);
            part.put("maxScore", partMax);
            total = total.add(score);
            max = max.add(partMax);
        }
        for (Map.Entry<String, ObjectNode> part : byPart.entrySet()) {
            BigDecimal partMax = maxByPart.get(part.getKey());
            if (partMax == null || partMax.signum() <= 0) {
                part.getValue().remove(List.of("score", "maxScore"));
            }
        }
        if (max.signum() > 0) {
            feedback.put("score", total);
            feedback.put("maxScore", max);
        } else {
            feedback.remove(List.of("score", "maxScore"));
        }
        return feedback;
    }

    private static void copyStrings(JsonNode source, ArrayNode target) {
        for (JsonNode value : source) {
            if (value.isTextual() && target.size() < MAX_ITEMS) {
                target.add(cut(value.asText()));
            }
        }
    }

    private static String cut(String value) {
        return value.length() > MAX_TEXT ? value.substring(0, MAX_TEXT) : value;
    }
}
