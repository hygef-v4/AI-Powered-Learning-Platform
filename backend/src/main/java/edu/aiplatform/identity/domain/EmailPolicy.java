package edu.aiplatform.identity.domain;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Chuẩn hóa và kiểm email trường (BR-U01-02, 03). Danh sách tên miền rỗng thì không email nào hợp lệ
 * (mặc định từ chối khi quên cấu hình {@code U01_ALLOWED_EMAIL_DOMAINS}).
 */
public final class EmailPolicy {

    public static final int MAX_LENGTH = 254;

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final Set<String> allowedDomains;

    public EmailPolicy(Collection<String> allowedDomains) {
        this.allowedDomains = allowedDomains.stream()
                .map(EmailPolicy::normalize)
                .filter(domain -> !domain.isEmpty())
                .map(domain -> domain.startsWith("@") ? domain.substring(1) : domain)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Cắt khoảng trắng và đổi chữ thường trước mọi so sánh (BR-U01-02). */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
    }

    /** Phần trước {@code @}, dùng cho BR-U01-31. */
    public static String localPart(String email) {
        int at = email.indexOf('@');
        return at < 0 ? email : email.substring(0, at);
    }

    public EmailCheck check(String normalizedEmail) {
        if (normalizedEmail.isEmpty() || normalizedEmail.length() > MAX_LENGTH
                || !FORMAT.matcher(normalizedEmail).matches()) {
            return EmailCheck.INVALID_EMAIL;
        }
        String domain = normalizedEmail.substring(normalizedEmail.lastIndexOf('@') + 1);
        return allowedDomains.contains(domain) ? EmailCheck.OK : EmailCheck.EMAIL_DOMAIN_NOT_ALLOWED;
    }

    public enum EmailCheck {
        OK, INVALID_EMAIL, EMAIL_DOMAIN_NOT_ALLOWED
    }
}
