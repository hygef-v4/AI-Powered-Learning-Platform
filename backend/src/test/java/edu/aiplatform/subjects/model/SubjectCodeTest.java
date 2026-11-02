package edu.aiplatform.subjects.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.shared.error.ApiException;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SubjectCodeTest {

    @Test
    void normalizesAndAcceptsTheLengthBoundary() {
        assertThat(new SubjectCode("  swr-302  ").value()).isEqualTo("SWR-302");
        assertThat(new SubjectCode("A".repeat(20)).value()).hasSize(20);
        // Q09 không thêm điều kiện phải bắt đầu bằng chữ.
        assertThat(new SubjectCode("1-A").value()).isEqualTo("1-A");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "SWR 302", "SWR_302", "SWR/302", "MÔN", "ABCDEFGHIJKLMNOPQRSTU"})
    void rejectsInvalidCodes(String value) {
        assertThatThrownBy(() -> new SubjectCode(value)).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("SUBJECT_CODE_INVALID");
    }

    @Test
    void normalizationDoesNotDependOnServerLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(new SubjectCode("it-101").value()).isEqualTo("IT-101");
        } finally {
            Locale.setDefault(previous);
        }
    }
}
