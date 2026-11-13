package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.AiImage;
import edu.aiplatform.ai.contract.AiProvider;
import java.time.Duration;
import java.util.List;

/**
 * Gọi một provider qua địa chỉ cố định trong code (BR-U06-02, NFR-U06-32). Lỗi ném {@link ProviderException} đã
 * phân loại theo cách tính token (BR-U06-14).
 */
public interface IProviderGateway {

    Response call(AiProvider provider, Request request);

    /**
     * {@code outputSchema} rỗng thì không ép JSON (dùng cho kiểm tra kết nối). {@code images} gửi trước phần chữ của lượt
     * user; rỗng thì body giống hệt khi chưa có ảnh (BR-U06-59).
     */
    record Request(String model, String apiKey, String system, String user, String outputSchema, int maxOutputTokens,
                   Duration timeout, List<AiImage> images) {

        public Request {
            images = images == null ? List.of() : List.copyOf(images);
        }

        public Request(String model, String apiKey, String system, String user, String outputSchema,
                       int maxOutputTokens, Duration timeout) {
            this(model, apiKey, system, user, outputSchema, maxOutputTokens, timeout, List.of());
        }

        @Override
        public String toString() {
            return "Request[model=" + model + ", maxOutputTokens=" + maxOutputTokens + ", images=" + images.size() + "]";
        }
    }

    /** Usage chuẩn hóa; trường không có thì rỗng. */
    record Usage(Long inputTokens, Long outputTokens, Long cachedTokens, Long reasoningTokens) {
    }

    record Response(String output, String requestId, Usage usage) {
    }

    /** Cách tính token khi lỗi (BR-U06-14). */
    enum FailureKind {
        /** Provider từ chối, không tính token: key sai, quá tần suất, model không hỗ trợ. */
        REJECTED,
        /** Provider đã trả phản hồi có usage nhưng không dùng được. */
        CHARGED,
        /** Không biết kết quả sau khi đã gửi: hết giờ, mất kết nối, lỗi phía provider. */
        UNKNOWN
    }

    class ProviderException extends RuntimeException {

        private final FailureKind kind;
        private final Usage usage;
        private final int status;

        public ProviderException(FailureKind kind, int status, String safeMessage, Usage usage) {
            super(safeMessage);
            this.kind = kind;
            this.usage = usage;
            this.status = status;
        }

        public FailureKind kind() {
            return kind;
        }

        public Usage usage() {
            return usage;
        }

        public int status() {
            return status;
        }
    }
}
