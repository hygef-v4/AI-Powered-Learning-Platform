package edu.aiplatform.identity.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProfilePolicyTest {

    @Test
    void displayNameIsTrimmedAndLimited() {
        assertThat(ProfilePolicy.normalizeDisplayName("  Vũ Minh Tài  ")).isEqualTo("Vũ Minh Tài");
        assertThat(ProfilePolicy.checkDisplayName("Tài")).isEmpty();
        assertThat(ProfilePolicy.checkDisplayName(ProfilePolicy.normalizeDisplayName("   "))).contains("INVALID_DISPLAY_NAME");
        assertThat(ProfilePolicy.checkDisplayName("ệ".repeat(150))).isEmpty();
        assertThat(ProfilePolicy.checkDisplayName("a".repeat(151))).contains("INVALID_DISPLAY_NAME");
    }

    @Test
    void phoneIsOptional() {
        assertThat(ProfilePolicy.normalizePhone("  ")).isNull();
        assertThat(ProfilePolicy.checkPhone(null)).isEmpty();
    }

    @Test
    void phoneAllowsLeadingPlusAndEightToFifteenDigits() {
        assertThat(ProfilePolicy.checkPhone("0912345678")).isEmpty();
        assertThat(ProfilePolicy.checkPhone("+84912345678")).isEmpty();
        assertThat(ProfilePolicy.checkPhone("1234567")).contains("INVALID_PHONE_NUMBER");
        assertThat(ProfilePolicy.checkPhone("1234567890123456")).contains("INVALID_PHONE_NUMBER");
        assertThat(ProfilePolicy.checkPhone("091 234 5678")).contains("INVALID_PHONE_NUMBER");
        assertThat(ProfilePolicy.checkPhone("09123+45678")).contains("INVALID_PHONE_NUMBER");
    }
}
