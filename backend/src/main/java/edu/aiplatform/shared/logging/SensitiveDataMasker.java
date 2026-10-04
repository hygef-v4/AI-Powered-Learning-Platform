package edu.aiplatform.shared.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Che giá trị của khóa nhạy cảm trong chuỗi log, cả dạng JSON và key=value. */
public final class SensitiveDataMasker {

    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)(\"?(?:password|otp|token|secret|phone|phoneNumber|authorization)\"?\\s*[:=]\\s*\"?)([^\",\\s}]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+");

    private SensitiveDataMasker() {}

    public static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        String masked = BEARER.matcher(message).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + "***"));
        return KEY_VALUE.matcher(masked).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + "***"));
    }
}
