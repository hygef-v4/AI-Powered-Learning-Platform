package edu.aiplatform.shared.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;

/** A small text-only document: formatting never contains HTML, links or executable attributes. */
public record FormattedText(List<Block> blocks) {
    public enum Type { PARAGRAPH, UL, OL }
    public enum Align { LEFT, CENTER, RIGHT, JUSTIFY }

    public record Run(String text, Boolean bold, Boolean italic, Boolean underline) {
        public Run {
            if (text == null || text.length() > 5000) throw invalid();
        }
    }

    public record Block(Type type, Align align, Integer indent, List<Run> runs) {
        public Block {
            if (type == null || runs == null || runs.isEmpty() || runs.size() > 200
                    || runs.stream().anyMatch(java.util.Objects::isNull)) throw invalid();
            align = align == null ? Align.LEFT : align;
            indent = indent == null ? 0 : indent;
            if (indent < 0 || indent > 6) throw invalid();
            runs = List.copyOf(runs);
        }
    }

    public FormattedText {
        if (blocks == null || blocks.isEmpty() || blocks.size() > 100
                || blocks.stream().anyMatch(java.util.Objects::isNull)) throw invalid();
        blocks = List.copyOf(blocks);
        if (blocks.stream().mapToInt(block -> block.runs().size()).sum() > 1000) throw invalid();
        int length = blocks.size() - 1;
        for (Block block : blocks) for (Run run : block.runs()) length += run.text().length();
        if (length > 5000) throw invalid();
    }

    public String toPlainText() {
        return blocks.stream().map(block -> block.runs().stream().map(Run::text)
                .collect(java.util.stream.Collectors.joining())).collect(java.util.stream.Collectors.joining("\n"));
    }

    public String validateLength(int maximum) {
        String text = toPlainText();
        if (text.length() > maximum) throw invalid();
        return text;
    }

    /** The quiz endpoint reads JSON manually; use the same bounded document as normal material DTO binding. */
    public static FormattedText parseDocument(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            return new ObjectMapper().treeToValue(node, FormattedText.class);
        } catch (Exception unreadable) {
            throw invalid();
        }
    }

    private static ApiException invalid() {
        return ApiException.badRequest("FORMATTED_TEXT_INVALID", "Mô tả có định dạng không hợp lệ hoặc quá dài");
    }
}
