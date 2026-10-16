package edu.aiplatform.files.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileNameSanitizerTest {

    @Test
    void stripsPathAndControlCharacters() {
        assertThat(FileNameSanitizer.sanitize("C:\\fake\\..\\bai\u0000 tap.pdf")).isEqualTo("bai tap.pdf");
        assertThat(FileNameSanitizer.sanitize("/etc/passwd")).isEqualTo("passwd");
    }

    @Test
    void fallsBackWhenNothingLeft() {
        assertThat(FileNameSanitizer.sanitize(null)).isEqualTo("tep");
        assertThat(FileNameSanitizer.sanitize("dir/..")).isEqualTo("tep");
        assertThat(FileNameSanitizer.sanitize("  \t ")).isEqualTo("tep");
    }

    @Test
    void cutsTo255Characters() {
        assertThat(FileNameSanitizer.sanitize("a".repeat(300) + ".pdf")).hasSize(255);
    }
}
