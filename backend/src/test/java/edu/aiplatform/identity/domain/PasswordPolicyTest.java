package edu.aiplatform.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.identity.domain.PasswordPolicy.Violation;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private static final String EMAIL = "taivm@fpt.edu.vn";

    @Test
    void acceptsLettersAndDigitsFromEightCharacters() {
        assertThat(PasswordPolicy.check("hocbai2026", EMAIL)).isEmpty();
        assertThat(PasswordPolicy.check("Mật khẩu 1", EMAIL)).isEmpty();
    }

    @Test
    void requiresMinimumLength() {
        assertThat(PasswordPolicy.check("abc1234", EMAIL)).containsExactly(Violation.TOO_SHORT);
        assertThat(PasswordPolicy.check(null, EMAIL)).contains(Violation.TOO_SHORT);
    }

    @Test
    void requiresLetterAndDigit() {
        assertThat(PasswordPolicy.check("12345678", EMAIL)).containsExactly(Violation.NEED_LETTER);
        assertThat(PasswordPolicy.check("abcdefgh", EMAIL)).containsExactly(Violation.NEED_DIGIT);
    }

    @Test
    void rejectsEmailNameIgnoringCase() {
        assertThat(PasswordPolicy.check("xxTAIVM2026", EMAIL)).containsExactly(Violation.CONTAINS_EMAIL_NAME);
    }

    @Test
    void ignoresVeryShortEmailName() {
        assertThat(PasswordPolicy.check("abcdef12", "ab@fpt.edu.vn")).isEmpty();
    }

    @Test
    void rejectsMoreThan72BytesInsteadOfTruncating() {
        assertThat(PasswordPolicy.check("a1".repeat(36), EMAIL)).isEmpty();
        assertThat(PasswordPolicy.check("a1".repeat(36) + "x", EMAIL)).containsExactly(Violation.TOO_LONG);
        // "ệ" chiếm 3 byte UTF-8: 24 lần "ệ" + "1" chỉ 25 ký tự nhưng 73 byte.
        assertThat(PasswordPolicy.check("ệ".repeat(24) + "1", EMAIL)).containsExactly(Violation.TOO_LONG);
    }
}
