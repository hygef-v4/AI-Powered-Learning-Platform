package edu.aiplatform.identity.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StudentCodePolicyTest {

    @Test
    void normalizeTrimsAndUppercases() {
        assertThat(StudentCodePolicy.normalize("  se123456 ")).isEqualTo("SE123456");
        assertThat(StudentCodePolicy.normalize("   ")).isNull();
        assertThat(StudentCodePolicy.normalize(null)).isNull();
    }

    @Test
    void validCodeHasTwoLettersThenSixDigits() {
        assertThat(StudentCodePolicy.isValid("SE123456")).isTrue();
        assertThat(StudentCodePolicy.isValid("HE000001")).isTrue();
    }

    @Test
    void invalidCodesAreRejected() {
        assertThat(StudentCodePolicy.isValid(null)).isFalse();
        assertThat(StudentCodePolicy.isValid("se123456")).isFalse();
        assertThat(StudentCodePolicy.isValid("S1234567")).isFalse();
        assertThat(StudentCodePolicy.isValid("SEE12345")).isFalse();
        assertThat(StudentCodePolicy.isValid("SE12345")).isFalse();
        assertThat(StudentCodePolicy.isValid("SE1234567")).isFalse();
        assertThat(StudentCodePolicy.isValid("SE 123456")).isFalse();
        assertThat(StudentCodePolicy.isValid("ĐE123456")).isFalse();
    }

    @Test
    void looksLikeStudentCodeNormalizesFirst() {
        assertThat(StudentCodePolicy.looksLikeStudentCode(" se123456")).isTrue();
        assertThat(StudentCodePolicy.looksLikeStudentCode("a@b.c")).isFalse();
        assertThat(StudentCodePolicy.looksLikeStudentCode("se-123456")).isFalse();
    }
}
