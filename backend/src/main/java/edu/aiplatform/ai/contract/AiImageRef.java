package edu.aiplatform.ai.contract;

/**
 * Tham chiếu một ảnh trong prompt: {@code source} chọn {@link IAiImageSource} đã đăng ký, {@code ref} do unit gọi tự
 * hiểu, {@code mediaType} chỉ {@code image/jpeg}, {@code image/png} hoặc {@code image/webp} (BR-U06-54).
 */
public record AiImageRef(String source, String ref, String mediaType) {
}
