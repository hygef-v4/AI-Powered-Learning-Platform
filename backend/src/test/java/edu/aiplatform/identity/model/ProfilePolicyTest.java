package edu.aiplatform.identity.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProfilePolicyTest {

    @Test
    void phoneIsOptional() {
        assertThat(ProfilePolicy.normalizePhone("  ")).isNull();
        assertThat(ProfilePolicy.checkPhone(null)).isEmpty();
    }
}
