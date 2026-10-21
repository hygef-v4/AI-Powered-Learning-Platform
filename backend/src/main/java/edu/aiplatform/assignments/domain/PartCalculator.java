package edu.aiplatform.assignments.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Tính phần (BR-U07-11): phần là heading nhỏ nhất của mỗi nhánh trong cây Tiêu đề 1–6. Tiêu đề, Phụ đề và block
 * trước heading đầu tiên là phần chung; khung không có Tiêu đề 1–6 là một phần {@code DOCUMENT}.
 */
public final class PartCalculator {

    public static final String WHOLE_DOCUMENT = "DOCUMENT";
    public static final String WHOLE_DOCUMENT_TITLE = "Toàn bài";
    public static final String UNTITLED = "(Chưa đặt tên)";
    public static final String SEPARATOR = " › ";

    private PartCalculator() {
    }

    public static List<DocumentPart> compute(DocumentModel document) {
        List<Heading> headings = headings(document);
        if (headings.isEmpty()) {
            return List.of(new DocumentPart(WHOLE_DOCUMENT, null, List.of(), WHOLE_DOCUMENT_TITLE));
        }
        List<DocumentPart> parts = new ArrayList<>();
        Deque<Heading> branch = new ArrayDeque<>();
        for (int i = 0; i < headings.size(); i++) {
            Heading heading = headings.get(i);
            while (!branch.isEmpty() && branch.peekLast().level() >= heading.level()) {
                branch.removeLast();
            }
            List<UUID> ancestors = branch.stream().map(Heading::id).toList();
            List<String> path = new ArrayList<>(branch.stream().map(Heading::title).toList());
            branch.addLast(heading);
            path.add(heading.title());
            boolean leaf = i + 1 == headings.size() || headings.get(i + 1).level() <= heading.level();
            if (leaf) {
                parts.add(new DocumentPart(heading.id().toString(), heading.id(), ancestors,
                        String.join(SEPARATOR, path)));
            }
        }
        return List.copyOf(parts);
    }

    /** Cấp 1–6 của block Tiêu đề 1–6; Tiêu đề, Phụ đề và block khác trả 0. */
    public static int level(DocumentBlock block) {
        if (block.type() != BlockType.HEADING || block.content() == null) {
            return 0;
        }
        String style = block.content().path("style").asText("");
        if (style.length() == 2 && style.charAt(0) == 'H' && style.charAt(1) >= '1' && style.charAt(1) <= '6') {
            return style.charAt(1) - '0';
        }
        return 0;
    }

    private static List<Heading> headings(DocumentModel document) {
        List<Heading> headings = new ArrayList<>();
        if (document == null || document.blocks() == null) {
            return headings;
        }
        for (DocumentBlock block : document.blocks()) {
            int level = level(block);
            if (level > 0 && block.id() != null) {
                JsonNode text = block.content().path("text");
                String title = text.isTextual() && !text.asText().isBlank() ? text.asText().strip() : UNTITLED;
                headings.add(new Heading(block.id(), level, title));
            }
        }
        return headings;
    }

    private record Heading(UUID id, int level, String title) {
    }
}
