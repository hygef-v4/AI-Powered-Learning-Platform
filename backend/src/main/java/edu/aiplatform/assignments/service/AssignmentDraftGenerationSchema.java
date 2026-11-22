package edu.aiplatform.assignments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.GradingMode;
import java.util.List;

/** Generation is deliberately flat: each requested question owns its rubric, before the longer shared text. */
final class AssignmentDraftGenerationSchema {
    private static final ObjectMapper JSON = new ObjectMapper();
    static final int MAX_AUTHORING_WARNINGS = 5;
    static final int MAX_AUTHORING_WARNING_MESSAGE = 300;
    static final List<String> AUTHORING_WARNING_CODES = List.of(
            "TEMPLATE_CONFLICT", "MISSING_REQUIRED_REFERENCE", "QUESTION_COUNT_CONFLICT");

    private AssignmentDraftGenerationSchema() {
    }

    static String buildSchema(AssignmentCommands.DraftOptions options, GradingMode mode) {
        try {
            ObjectNode schema = (ObjectNode) JSON.readTree(AiDraftProposal.OUTPUT_SCHEMA);
            ObjectNode original = (ObjectNode) schema.path("properties");
            ObjectNode properties = JSON.createObjectNode();
            // Gemini follows property order. Complete questions/rubrics before spending tokens on prose.
            for (String name : new String[]{"sections", "title", "businessContext", "instructions", "schemaVersion", "businessContextFormat"}) {
                properties.set(name, original.get(name));
            }
            schema.set("properties", properties);
            schema.putArray("required").add("sections").add("title").add("businessContext")
                    .add("instructions").add("schemaVersion");
            ((ObjectNode) properties.path("schemaVersion")).putArray("enum").add(2);
            addBoundedText(properties, "title", 1, 200);
            addBoundedText(properties, "businessContext", 1, AiDraftProposal.MAX_GENERATED_CONTEXT);
            addBoundedText(properties, "instructions", 0, AiDraftProposal.MAX_GENERATED_INSTRUCTIONS);
            ObjectNode sections = (ObjectNode) properties.path("sections");
            Integer count = options == null ? null : options.questionCount();
            sections.put("minItems", count == null ? 1 : count);
            sections.put("maxItems", count == null ? 10 : count);
            ObjectNode section = (ObjectNode) sections.path("items");
            section.putArray("required").add("level").add("title").add("guidance").add("rubric");
            ObjectNode fields = (ObjectNode) section.path("properties");
            ((ObjectNode) fields.path("level")).putArray("enum").add(1);
            addBoundedText(fields, "title", 1, 200);
            addBoundedText(fields, "guidance", 1, 3000);
            ObjectNode rubric = (ObjectNode) fields.path("rubric");
            rubric.put("minItems", mode == GradingMode.GRADED ? 1 : 0).put("maxItems", 6);
            ObjectNode criterion = (ObjectNode) rubric.path("items").path("properties");
            addBoundedText(criterion, "title", 1, 200);
            addBoundedText(criterion, "description", 0, 1000);
            ObjectNode items = (ObjectNode) criterion.path("items");
            items.put("minItems", 1).put("maxItems", 6);
            ObjectNode item = (ObjectNode) items.path("items").path("properties");
            addBoundedText(item, "text", 1, 1000);
            ((ObjectNode) item.path("points")).put("exclusiveMinimum", 0).put("maximum", 1000);
            ObjectNode warning = properties.putObject("authoringWarnings").put("type", "array")
                    .put("maxItems", MAX_AUTHORING_WARNINGS).putObject("items")
                    .put("type", "object").put("additionalProperties", false);
            warning.putArray("required").add("code").add("message");
            var warningFields = warning.putObject("properties");
            var codes = warningFields.putObject("code").put("type", "string").putArray("enum");
            AUTHORING_WARNING_CODES.forEach(codes::add);
            warningFields.putObject("message").put("type", "string").put("minLength", 1)
                    .put("maxLength", MAX_AUTHORING_WARNING_MESSAGE);
            return schema.toString();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được schema tạo đề", e);
        }
    }

    private static void addBoundedText(ObjectNode fields, String name, int minimum, int maximum) {
        ((ObjectNode) fields.path(name)).put("minLength", minimum).put("maxLength", maximum)
                .put("description", "At most " + maximum + " characters. Be concise, with no repetitive filler.");
    }
}
