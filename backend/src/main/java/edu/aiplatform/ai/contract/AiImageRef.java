package edu.aiplatform.ai.contract;

/**
 * Tham chiếu một ảnh trong prompt: {@code source} chọn {@link IAiImageSource} đã đăng ký, {@code ref} do unit gọi tự
 * hiểu, {@code mediaType} chỉ {@code image/jpeg} hoặc {@code image/png} (BR-U06-54).
 */
public record AiImageRef(String source, String ref, String mediaType) {
}
