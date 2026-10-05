package edu.aiplatform.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

    @Test
    void masksJsonValues() {
        assertThat(SensitiveDataMasker.mask("{\"email\":\"a@b.edu\",\"password\":\"Secret123\"}"))
                .isEqualTo("{\"email\":\"a@b.edu\",\"password\":\"***\"}");
    }

    @Test
    void masksKeyValueAndBearer() {
        assertThat(SensitiveDataMasker.mask("otp=123456 Authorization: Bearer abc.def.ghi"))
                .doesNotContain("123456")
                .doesNotContain("abc.def.ghi");
    }

    @Test
    void leavesPlainMessages() {
        assertThat(SensitiveDataMasker.mask("Enrollment created for class 42")).isEqualTo("Enrollment created for class 42");
    }
}
