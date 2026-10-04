package edu.aiplatform.shared.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ForbiddenKeyGuardTest {

    @Test
    void allowsIdsAndReferences() {
        assertThatCode(() -> ForbiddenKeyGuard.check(Map.of("accountId", "a1", "purpose", "ACTIVATION")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTopLevelForbiddenKey() {
        assertThatThrownBy(() -> ForbiddenKeyGuard.check(Map.of("otpCode", "123456")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNestedForbiddenKeyInMapsAndLists() {
        Map<String, Object> payload = Map.of("items", List.of(Map.of("meta", Map.of("refreshToken", "x"))));
        assertThatThrownBy(() -> ForbiddenKeyGuard.check(payload)).isInstanceOf(IllegalArgumentException.class);
    }
}
