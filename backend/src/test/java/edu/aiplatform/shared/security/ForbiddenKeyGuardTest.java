package edu.aiplatform.shared.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ForbiddenKeyGuardTest {

    @Test
    void allowsOrdinaryKeys() {
        assertThatCode(() -> ForbiddenKeyGuard.check(Map.of("assignmentId", "a1", "inputTokens", 120)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsForbiddenKeyIgnoringCase() {
        assertThatThrownBy(() -> ForbiddenKeyGuard.check(Map.of("Password", "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("$.Password");
    }

    @Test
    void rejectsForbiddenKeyInNestedMapsAndLists() {
        Map<String, Object> data = Map.of("items", List.of(Map.of("ok", 1), Map.of("meta", Map.of("otp", "123456"))));
        assertThatThrownBy(() -> ForbiddenKeyGuard.check(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("$.items[1].meta.otp");
    }
}
