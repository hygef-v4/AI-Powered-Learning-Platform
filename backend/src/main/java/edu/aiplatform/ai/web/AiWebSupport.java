package edu.aiplatform.ai.web;

import edu.aiplatform.ai.application.AiAccess;
import edu.aiplatform.ai.contract.AiProvider;
import java.util.Locale;

/** Đọc tham số đường dẫn chung của API U06; provider lạ trả {@code 404} như tài nguyên không có. */
final class AiWebSupport {

    private AiWebSupport() {
    }

    static AiProvider provider(String value) {
        try {
            return AiProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw AiAccess.notFound();
        }
    }
}
