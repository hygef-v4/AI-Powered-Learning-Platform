package edu.aiplatform.shared.content;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormattedTextTest {
    @Test
    void preservesVietnameseFormattingAndDerivesPlainText() throws Exception {
        var json = new ObjectMapper();
        var node = json.readTree("""
                {"blocks":[{"type":"PARAGRAPH","align":"CENTER","indent":0,
                  "runs":[{"text":"Nội dung chính","bold":true}]},
                  {"type":"UL","align":"LEFT","indent":1,"runs":[{"text":"Ví dụ"}]}]}
                """);
        var document = FormattedText.parseDocument(node);
        assertThat(document.validateLength(2000)).isEqualTo("Nội dung chính\nVí dụ");
        assertThat(document.blocks().get(0).runs().get(0).bold()).isTrue();
        assertThat(json.readValue(json.writeValueAsString(document), FormattedText.class)).isEqualTo(document);
    }

    @Test
    void rejectsBadDocumentShapeAndBoundedLengths() throws Exception {
        var json = new ObjectMapper();
        for (String content : List.of("{}", "{\"blocks\":[]}", "{\"blocks\":[null]}",
                "{\"blocks\":[{\"type\":\"SCRIPT\",\"runs\":[{\"text\":\"x\"}]}]}",
                "{\"blocks\":[{\"type\":\"PARAGRAPH\",\"indent\":7,\"runs\":[{\"text\":\"x\"}]}]}",
                "{\"blocks\":[{\"type\":\"PARAGRAPH\",\"runs\":[{\"text\":\"x\",\"html\":\"unsafe\"}]}]}")) {
            var node = json.readTree(content);
            assertThatThrownBy(() -> FormattedText.parseDocument(node)).isInstanceOf(ApiException.class);
        }
        var document = new FormattedText(List.of(new FormattedText.Block(FormattedText.Type.PARAGRAPH,
                FormattedText.Align.LEFT, 0, List.of(new FormattedText.Run("x".repeat(2001), null, null, null)))));
        assertThatThrownBy(() -> document.validateLength(2000)).isInstanceOf(ApiException.class);
    }
}
