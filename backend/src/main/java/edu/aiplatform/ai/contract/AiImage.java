package edu.aiplatform.ai.contract;

/** Bytes một ảnh nạp lúc gọi provider; không ghi vào log hay {@code toString} (BR-U06-60). */
public record AiImage(String mediaType, byte[] bytes) {

    @Override
    public String toString() {
        return "AiImage[mediaType=" + mediaType + ", bytes=" + (bytes == null ? 0 : bytes.length) + "]";
    }
}
