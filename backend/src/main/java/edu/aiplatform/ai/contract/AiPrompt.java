package edu.aiplatform.ai.contract;

import java.util.List;

/** {@code images}: tham chiếu ảnh gửi kèm lượt user, worker nạp bytes qua {@link IAiImageSource} (BR-U06-54). */
public record AiPrompt(String system, String user, List<AiImageRef> images) {

    public AiPrompt {
        images = images == null ? List.of() : List.copyOf(images);
    }

    public AiPrompt(String system, String user) {
        this(system, user, List.of());
    }

    public boolean hasImages() {
        return !images.isEmpty();
    }
}
