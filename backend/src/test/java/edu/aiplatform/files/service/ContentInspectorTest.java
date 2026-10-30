package edu.aiplatform.files.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.files.model.FileType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContentInspectorTest {

    @TempDir
    Path dir;

    final ContentInspector inspector = new ContentInspector();

    @Test
    void detectsByContentNotByExtension() throws Exception {
        assertThat(inspector.detect(write("anh.pdf", TestFiles.PNG), "anh.pdf")).contains(FileType.PNG);
        assertThat(inspector.detect(write("de.pdf", TestFiles.PDF), "de.pdf")).contains(FileType.PDF);
        assertThat(inspector.detect(write("so-do.svg", TestFiles.SVG), "so-do.svg")).contains(FileType.SVG);
        assertThat(inspector.detect(write("bai.docx", TestFiles.docx()), "bai.docx")).contains(FileType.DOCX);
    }

    @Test
    void textRenamedAsPdfIsRecognizedByItsActualContent() throws Exception {
        assertThat(inspector.detect(write("gia.pdf", TestFiles.TEXT), "gia.pdf")).contains(FileType.TXT);
    }

    @Test
    void detectsPlainTextForAssignmentSources() throws Exception {
        assertThat(inspector.detect(write("exam.txt", "Exam context and requirements".getBytes()), "exam.txt"))
                .contains(FileType.TXT);
    }

    private Path write(String name, byte[] content) throws Exception {
        return Files.write(dir.resolve(name), content);
    }
}
