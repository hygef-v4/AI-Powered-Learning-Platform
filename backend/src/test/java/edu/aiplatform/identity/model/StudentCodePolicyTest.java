package edu.aiplatform.identity.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StudentCodePolicyTest {

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
}
