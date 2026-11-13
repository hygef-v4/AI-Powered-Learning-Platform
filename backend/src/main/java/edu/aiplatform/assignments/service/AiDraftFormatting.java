package edu.aiplatform.assignments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/** The same bounded run attributes as the document editor; never accepts HTML or CSS. */
final class AiDraftFormatting {
    private static final List<String> FLAGS = List.of("bold", "italic", "underline", "strike", "code");
    private static final List<String> COLORS = List.of("color", "highlight");
    static final String SCHEMA = """
            {"type":"object","additionalProperties":false,"properties":{
             "align":{"type":"string","enum":["LEFT","CENTER","RIGHT","JUSTIFY"]},
             "bold":{"type":"boolean"},"italic":{"type":"boolean"},"underline":{"type":"boolean"},
             "strike":{"type":"boolean"},"code":{"type":"boolean"},
             "color":{"type":"string","pattern":"^#[0-9a-fA-F]{6}$"},
             "highlight":{"type":"string","pattern":"^#[0-9a-fA-F]{6}$"},
             "runs":{"type":"array","maxItems":100,"items":{"type":"object","additionalProperties":false,
              "required":["text"],"properties":{
               "text":{"type":"string","maxLength":20000},
               "bold":{"type":"boolean"},"italic":{"type":"boolean"},"underline":{"type":"boolean"},
               "strike":{"type":"boolean"},"code":{"type":"boolean"},
               "color":{"type":"string","pattern":"^#[0-9a-fA-F]{6}$"},
               "highlight":{"type":"string","pattern":"^#[0-9a-fA-F]{6}$"}}}}}}
            """;
    static final String INSTRUCTION = " Use optional format objects for requested text formatting: align, bold, "
            + "italic, underline, strike, code, color and highlight (#RRGGBB). For selective emphasis in headings "
            + "or paragraphs, format.runs must concatenate to exactly the plain text. Never output HTML or Markdown "
            + "formatting markers. Omit formatting not requested; false removes a text flag. "
            + "A format on a LIST or TABLE styles all of its text; keep their structure. ";

    private AiDraftFormatting() {
    }

    static ObjectNode normalize(JsonNode source, String text) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        if (!source.isObject()) return out;
        if (DocumentModelService.ALIGNS.contains(source.path("align").asText(""))) {
            out.put("align", source.path("align").asText());
        }
        copyStyle(source, out);
        JsonNode runs = source.path("runs");
        if (runs.isArray() && !runs.isEmpty() && runs.size() <= 100) {
            StringBuilder joined = new StringBuilder();
            ArrayNode clean = JsonNodeFactory.instance.arrayNode();
            for (JsonNode run : runs) {
                if (!run.path("text").isTextual() || run.path("text").asText().length() > DocumentModelService.MAX_TEXT) return out;
                joined.append(run.path("text").asText());
                ObjectNode next = clean.addObject().put("text", run.path("text").asText());
                copyStyle(run, next);
            }
            if (joined.toString().equals(text)) out.set("runs", clean);
        }
        return out;
    }

    private static void copyStyle(JsonNode source, ObjectNode target) {
        for (String flag : FLAGS) {
            if (source.path(flag).isBoolean()) target.put(flag, source.path(flag).asBoolean());
        }
        for (String color : COLORS) {
            if (source.path(color).asText("").matches("#[0-9a-fA-F]{6}")) target.put(color, source.path(color).asText());
        }
    }

    static void apply(ObjectNode content, JsonNode format) {
        if (!format.isObject() || format.isEmpty()) return;
        if (format.has("align")) content.set("align", format.get("align"));
        if (format.has("runs")) content.set("runs", format.get("runs").deepCopy());
        styleRuns(content.path("runs"), format);
        for (JsonNode item : content.path("items")) styleRuns(item.path("runs"), format);
        for (JsonNode row : content.path("rows")) {
            for (JsonNode cell : row) {
                if (format.has("align") && cell instanceof ObjectNode object) object.set("align", format.get("align"));
                styleRuns(cell.path("runs"), format);
            }
        }
    }

    private static void styleRuns(JsonNode runs, JsonNode format) {
        for (JsonNode run : runs) {
            if (run instanceof ObjectNode object) copyStyle(format, object);
        }
    }
}
