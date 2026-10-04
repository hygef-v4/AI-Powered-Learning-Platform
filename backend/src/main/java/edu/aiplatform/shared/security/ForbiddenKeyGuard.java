package edu.aiplatform.shared.security;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Chặn khóa nhạy cảm trong `details` của audit (U02) và payload việc nền (U03). */
public final class ForbiddenKeyGuard {

    private static final Set<String> FORBIDDEN = Set.of("password", "otp", "token", "secret", "phone");

    private ForbiddenKeyGuard() {}

    /** Ném {@link IllegalArgumentException} nếu gặp khóa cấm ở bất kỳ cấp nào. */
    public static void check(Map<String, ?> payload) {
        if (payload == null) {
            return;
        }
        payload.forEach((key, value) -> {
            String lower = key.toLowerCase(Locale.ROOT);
            for (String bad : FORBIDDEN) {
                if (lower.contains(bad)) {
                    throw new IllegalArgumentException("Forbidden key in payload: " + key);
                }
            }
            checkValue(value);
        });
    }

    @SuppressWarnings("unchecked")
    private static void checkValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            check((Map<String, ?>) map);
        } else if (value instanceof Collection<?> items) {
            items.forEach(ForbiddenKeyGuard::checkValue);
        }
    }
}
