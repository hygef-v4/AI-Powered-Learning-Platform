package edu.aiplatform.shared.security;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Chặn khóa nhạy cảm trong {@code details} của audit (U02) và payload việc nền (U03), BR-U02-05.
 * So khớp đúng tên khóa, không phân biệt hoa thường, kể cả trong map và danh sách lồng nhau.
 */
public final class ForbiddenKeyGuard {

    public static final Set<String> FORBIDDEN_KEYS = Set.of("password", "otp", "token", "secret", "phone");

    private ForbiddenKeyGuard() {
    }

    /** Ném {@link IllegalArgumentException} kèm đường dẫn khóa nếu gặp khóa bị cấm. */
    public static void check(Map<String, ?> data) {
        walk(data, "$");
    }

    private static void walk(Object node, String path) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (FORBIDDEN_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Khóa bị cấm: " + path + "." + key);
                }
                walk(entry.getValue(), path + "." + key);
            }
        } else if (node instanceof Collection<?> items) {
            int index = 0;
            for (Object item : items) {
                walk(item, path + "[" + index++ + "]");
            }
        }
    }
}
