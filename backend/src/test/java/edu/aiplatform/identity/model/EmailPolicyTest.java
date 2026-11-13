package edu.aiplatform.identity.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailPolicyTest {

    @Test
    void normalizesBeforeComparing() {
        assertThat(EmailPolicy.normalize("  TaiVM@FPT.edu.VN \t")).isEqualTo("taivm@fpt.edu.vn");
        assertThat(EmailPolicy.normalize(null)).isEmpty();
    }

    /** BR-U01-03: không giới hạn tên miền. */
    @Test
    void acceptsAnyDomain() {
        assertThat(EmailPolicy.isValid("taivm@fpt.edu.vn")).isTrue();
        assertThat(EmailPolicy.isValid("taivm@gmail.com")).isTrue();
        assertThat(EmailPolicy.isValid("taivm@sub.fpt.edu.vn")).isTrue();
    }

    @Test
    void rejectsMalformedOrTooLongEmail() {
        assertThat(EmailPolicy.isValid("")).isFalse();
        assertThat(EmailPolicy.isValid("taivm")).isFalse();
        assertThat(EmailPolicy.isValid("taivm@gmail")).isFalse();
        assertThat(EmailPolicy.isValid("tai vm@fpt.edu.vn")).isFalse();
        assertThat(EmailPolicy.isValid("a".repeat(250) + "@fpt.edu.vn")).isFalse();
    }
}
