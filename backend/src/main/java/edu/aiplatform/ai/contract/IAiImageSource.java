package edu.aiplatform.ai.contract;

import java.util.Optional;

/**
 * U06 khai báo, unit gọi cài (U07 nhập đề): nạp bytes ảnh theo tham chiếu trong prompt ngay trước khi gọi provider, để
 * {@code ai_tasks} không lưu bytes và U06 không phụ thuộc ngược unit gọi (BR-U06-55).
 */
public interface IAiImageSource {

    String source();

    Optional<AiImage> load(String ref);
}
