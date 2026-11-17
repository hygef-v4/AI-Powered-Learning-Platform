package edu.aiplatform.identity.model;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.identity.model.PasswordPolicy.Violation;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private static final String EMAIL = "taivm@fpt.edu.vn";

    @Test
    void acceptsLettersAndDigitsFromEightCharacters() {
        assertThat(PasswordPolicy.checkPassword("hocbai2026", EMAIL)).isEmpty();
        assertThat(PasswordPolicy.checkPassword("Mật khẩu 1", EMAIL)).isEmpty();
    }

    @Test
    void requiresMinimumLength() {
        assertThat(PasswordPolicy.checkPassword("abc1234", EMAIL)).containsExactly(Violation.TOO_SHORT);
        assertThat(PasswordPolicy.checkPassword(null, EMAIL)).contains(Violation.TOO_SHORT);
    }

    @Test
    void requiresLetterAndDigit() {
        assertThat(PasswordPolicy.checkPassword("12345678", EMAIL)).containsExactly(Violation.NEED_LETTER);
        assertThat(PasswordPolicy.checkPassword("abcdefgh", EMAIL)).containsExactly(Violation.NEED_DIGIT);
    }

    @Test
    void rejectsEmailNameIgnoringCase() {
        assertThat(PasswordPolicy.checkPassword("xxTAIVM2026", EMAIL)).containsExactly(Violation.CONTAINS_EMAIL_NAME);
    }

    @Test
    void ignoresVeryShortEmailName() {
        assertThat(PasswordPolicy.checkPassword("abcdef12", "ab@fpt.edu.vn")).isEmpty();
    }

    @Test
    void rejectsMoreThan72BytesInsteadOfTruncating() {
        assertThat(PasswordPolicy.checkPassword("a1".repeat(36), EMAIL)).isEmpty();
        assertThat(PasswordPolicy.checkPassword("a1".repeat(36) + "x", EMAIL)).containsExactly(Violation.TOO_LONG);
        // "ệ" chiếm 3 byte UTF-8: 24 lần "ệ" + "1" chỉ 25 ký tự nhưng 73 byte.
        assertThat(PasswordPolicy.checkPassword("ệ".repeat(24) + "1", EMAIL)).containsExactly(Violation.TOO_LONG);
    }
}
