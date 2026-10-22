package edu.aiplatform.assignmentwork.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Định dạng phản hồi Practice (BR-U08-42): nhận xét chung, điểm mạnh, nhận xét từng phần, gợi ý; luôn kèm nhãn "AI ước
 * tính, không phải điểm chính thức". Không có ước tính theo rubric (người học không xem rubric). Sai định dạng thì
 * không lưu (BR-U08-43).
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
               "additionalProperties":false,"properties":{"partId":{"type":"string"},"comment":{"type":"string"}}}},
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
        strings(raw.path("strengths"), out.putArray("strengths"));
        strings(raw.path("suggestions"), out.putArray("suggestions"));
        ArrayNode parts = out.putArray("parts");
        for (JsonNode part : raw.path("parts")) {
            if (parts.size() >= MAX_ITEMS || !part.path("partId").isTextual()) {
                continue;
            }
            parts.addObject().put("partId", cut(part.path("partId").asText()))
                    .put("comment", cut(part.path("comment").asText("")));
        }
        return out;
    }

    private static void strings(JsonNode source, ArrayNode target) {
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
