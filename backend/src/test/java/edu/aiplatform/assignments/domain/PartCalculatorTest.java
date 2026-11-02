package edu.aiplatform.assignments.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** BR-U07-11: phần là heading nhỏ nhất của mỗi nhánh; Tiêu đề, Phụ đề là phần chung; không có heading là một phần. */
class PartCalculatorTest {

    static DocumentBlock heading(String style, String text) {
        ObjectNode content = JsonNodeFactory.instance.objectNode().put("style", style).put("text", text);
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.HEADING, content);
    }

    static DocumentBlock paragraph(String text) {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.putArray("runs").addObject().put("text", text);
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.PARAGRAPH, content);
    }

    @Test
    void documentWithoutHeadingsIsOnePart() {
        List<DocumentPart> parts = PartCalculator.compute(new DocumentModel(List.of(paragraph("a"), heading("TITLE", "Đề"))));
        assertThat(parts).singleElement().satisfies(p -> {
            assertThat(p.partId()).isEqualTo(PartCalculator.WHOLE_DOCUMENT);
            assertThat(p.headingBlockId()).isNull();
        });
    }

    @Test
    void leavesOfEachBranchArePartsAsInBusinessRuleExample() {
        DocumentBlock h1 = heading("H1", "H1");
        DocumentBlock h21 = heading("H2", "H2.1");
        DocumentBlock h31 = heading("H3", "H3.1");
        DocumentBlock h32 = heading("H3", "H3.2");
        DocumentBlock h22 = heading("H2", "H2.2");
        List<DocumentPart> parts = PartCalculator.compute(new DocumentModel(List.of(heading("TITLE", "Đề"), paragraph("chung"),
                h1, h21, h31, paragraph("x"), h32, h22)));
        assertThat(parts).extracting(DocumentPart::partId)
                .containsExactly(h31.id().toString(), h32.id().toString(), h22.id().toString());
        assertThat(parts.get(0).ancestorHeadingIds()).containsExactly(h1.id(), h21.id());
        assertThat(parts.get(0).title()).isEqualTo("H1 › H2.1 › H3.1");
        assertThat(parts.get(2).ancestorHeadingIds()).containsExactly(h1.id());
    }

    @Test
    void partIdFollowsHeadingBlockSoItSurvivesEdits() {
        DocumentBlock h1 = heading("H1", "Cũ");
        String before = PartCalculator.compute(new DocumentModel(List.of(h1))).get(0).partId();
        DocumentBlock renamed = new DocumentBlock(h1.id(), h1.origin(), h1.type(),
                JsonNodeFactory.instance.objectNode().put("style", "H1").put("text", "Mới"));
        String after = PartCalculator.compute(new DocumentModel(List.of(paragraph("thêm"), renamed))).get(0).partId();
        assertThat(after).isEqualTo(before);
    }

    @Test
    void blankHeadingGetsPlaceholderTitle() {
        assertThat(PartCalculator.compute(new DocumentModel(List.of(heading("H2", " ")))).get(0).title())
                .isEqualTo(PartCalculator.UNTITLED);
    }
}
