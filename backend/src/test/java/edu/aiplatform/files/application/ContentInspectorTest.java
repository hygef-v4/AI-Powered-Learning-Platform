package edu.aiplatform.files.application;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.files.FileFixtures;
import edu.aiplatform.files.domain.PurposePolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** BR-U03-03, P3: loại tệp theo nội dung, không theo đuôi. */
class ContentInspectorTest {

    private final ContentInspector inspector = new ContentInspector();

    @TempDir
    Path dir;

    @Test
    void detectsByContent() throws IOException {
        assertThat(detect(FileFixtures.png(), "a.png")).isEqualTo("image/png");
        assertThat(detect(FileFixtures.pdf(), "a.pdf")).isEqualTo(PurposePolicy.PDF);
        assertThat(detect(FileFixtures.svgWithScript(), "a.svg")).isEqualTo(PurposePolicy.SVG);
        assertThat(detect(FileFixtures.docx(), "bai-giang.docx")).isEqualTo(PurposePolicy.DOCX);
    }

    @Test
    void renamedFilesAreNotTrusted() throws IOException {
        assertThat(detect(FileFixtures.exe(), "anh.png")).isNotEqualTo("image/png");
        assertThat(detect(FileFixtures.png(), "tai-lieu.pdf")).isEqualTo("image/png");
    }

    private String detect(byte[] content, String name) throws IOException {
        Path file = Files.write(dir.resolve("f" + System.nanoTime()), content);
        return inspector.detect(file, name);
    }
}
