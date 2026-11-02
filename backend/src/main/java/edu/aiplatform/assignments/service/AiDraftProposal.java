package edu.aiplatform.assignments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentRubric;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Đề xuất của AI hoặc agent (BR-U07-50, 51): hướng dẫn, các mục heading cấp 1–6 kèm đoạn hướng dẫn, gợi ý rubric
 * theo mục. Kết quả lạ bị cắt về giới hạn của khung và rubric; không có sơ đồ.
 */
final class AiDraftProposal {

    static final int MAX_SECTIONS = 100;
    static final int MAX_GENERATED_CONTEXT = 4000;
    static final int MAX_GENERATED_INSTRUCTIONS = 1200;
    static final String OUTPUT_SCHEMA = """
            {"type":"object","required":["instructions","sections"],"additionalProperties":false,
             "properties":{
              "schemaVersion":{"type":"integer","minimum":1,"maximum":2},
              "title":{"type":"string"},
              "businessContext":{"type":"string"},
              "instructions":{"type":"string"},
              "sections":{"type":"array","minItems":1,"maxItems":100,"items":{"type":"object","required":["level","title"],
               "additionalProperties":false,"properties":{
                "level":{"type":"integer","minimum":1,"maximum":6},
                "title":{"type":"string"},
                "guidance":{"type":"string"},
                "rubric":{"type":"array","maxItems":20,"items":{"type":"object","required":["title","items"],
                 "additionalProperties":false,"properties":{
                  "title":{"type":"string"},"description":{"type":"string"},
                  "kind":{"type":"string","enum":["TEXT","DIAGRAM"]},
                  "items":{"type":"array","maxItems":20,"items":{"type":"object","required":["text","points"],
                   "additionalProperties":false,"properties":{"text":{"type":"string"},"points":{"type":"number"}}}}}}}}}}}}
            """;

    private AiDraftProposal() {
    }

    /** Chuẩn hóa đề xuất; {@code null} nếu sai định dạng hoàn toàn. */
    static ObjectNode normalize(JsonNode raw, boolean diagramEnabled) {
        if (raw == null || !raw.isObject()) {
            return null;
        }
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.put("schemaVersion", raw.path("schemaVersion").asInt(
                raw.has("title") && raw.has("businessContext") ? 2 : 1));
        out.put("title", cut(raw.path("title").asText("").strip(), Assignment.TITLE_MAX));
        out.put("businessContext", cut(raw.path("businessContext").asText(""), DocumentModelService.MAX_TEXT));
        out.put("instructions", cut(raw.path("instructions").asText(""), Assignment.INSTRUCTIONS_MAX));
        ArrayNode sections = out.putArray("sections");
        JsonNode rawSections = raw.path("sections");
        if (!rawSections.isArray()) {
            return out;
        }
        for (JsonNode section : rawSections) {
            if (sections.size() >= MAX_SECTIONS) {
                break;
            }
            String title = cut(section.path("title").asText("").strip(), DocumentModelService.MAX_HEADING);
            if (title.isEmpty()) {
                continue;
            }
            ObjectNode next = sections.addObject();
            next.put("level", Math.max(1, Math.min(6, section.path("level").asInt(1))));
            next.put("title", title);
            next.put("guidance", cut(section.path("guidance").asText(""), DocumentModelService.MAX_TEXT));
            ArrayNode rubric = next.putArray("rubric");
            for (JsonNode criterion : section.path("rubric")) {
                if (rubric.size() >= AssignmentRubric.CRITERIA_MAX) {
                    break;
                }
                ObjectNode c = rubric.addObject();
                c.put("title", cut(criterion.path("title").asText(""), AssignmentRubric.TITLE_MAX));
                c.put("description", cut(criterion.path("description").asText(""), AssignmentRubric.DESCRIPTION_MAX));
                c.put("kind", diagramEnabled && "DIAGRAM".equals(criterion.path("kind").asText()) ? "DIAGRAM" : "TEXT");
                ArrayNode items = c.putArray("items");
                for (JsonNode item : criterion.path("items")) {
                    if (items.size() >= AssignmentRubric.ITEMS_MAX) {
                        break;
                    }
                    ObjectNode i = items.addObject();
                    i.put("text", cut(item.path("text").asText(""), AssignmentRubric.ITEM_TEXT_MAX));
                    BigDecimal points = points(item.path("points"));
                    if (points != null) {
                        i.put("points", points);
                    }
                }
            }
        }
        return out;
    }

    /** Khung mới từ đề xuất và gợi ý rubric theo id block heading. */
    static Built build(JsonNode proposal) {
        List<DocumentBlock> blocks = new ArrayList<>();
        Map<UUID, List<RubricCriterion>> suggestions = new HashMap<>();
        String businessContext = proposal.path("businessContext").asText("").strip();
        if (!businessContext.isEmpty()) {
            addParagraphs(blocks, businessContext);
        }
        for (JsonNode section : proposal.path("sections")) {
            UUID headingId = UUID.randomUUID();
            ObjectNode heading = JsonNodeFactory.instance.objectNode();
            heading.put("style", "H" + section.path("level").asInt(1));
            heading.put("text", section.path("title").asText(""));
            blocks.add(new DocumentBlock(headingId, BlockOrigin.TEACHER, BlockType.HEADING, heading));
            String guidance = section.path("guidance").asText("");
            if (!guidance.isBlank()) {
                addParagraphs(blocks, guidance);
            }
            List<RubricCriterion> criteria = new ArrayList<>();
            for (JsonNode criterion : section.path("rubric")) {
                List<RubricItem> items = new ArrayList<>();
                for (JsonNode item : criterion.path("items")) {
                    items.add(new RubricItem(UUID.randomUUID(), item.path("text").asText(""),
                            points(item.path("points"))));
                }
                criteria.add(new RubricCriterion(UUID.randomUUID(), criterion.path("title").asText(""),
                        criterion.path("description").asText(""),
                        CriterionKind.valueOf(criterion.path("kind").asText("TEXT")), List.copyOf(items)));
            }
            if (!criteria.isEmpty()) {
                suggestions.put(headingId, List.copyOf(criteria));
            }
        }
        return new Built(new DocumentModel(List.copyOf(blocks)), suggestions);
    }

    private static void addParagraphs(List<DocumentBlock> blocks, String text) {
        // Preserve all content while bounding the number of new document blocks per field.
        for (String value : text.strip().split("\\n\\s*\\n", 8)) {
            ObjectNode paragraph = JsonNodeFactory.instance.objectNode();
            paragraph.putArray("runs").addObject().put("text", value.strip());
            blocks.add(new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.PARAGRAPH, paragraph));
        }
    }

    record Built(DocumentModel skeleton, Map<UUID, List<RubricCriterion>> rubricByHeading) {
    }

    private static BigDecimal points(JsonNode value) {
        if (value == null || !value.isNumber()) {
            return null;
        }
        BigDecimal points = value.decimalValue().setScale(2, RoundingMode.HALF_UP);
        return points.signum() <= 0 || points.compareTo(AssignmentRubric.POINTS_MAX) > 0 ? null : points;
    }

    private static String cut(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
