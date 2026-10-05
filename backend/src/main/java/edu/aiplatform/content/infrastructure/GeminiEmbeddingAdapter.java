package edu.aiplatform.content.infrastructure;

import edu.aiplatform.content.port.*;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U05: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U05: Adapter Gemini.
 */
@Component
public class GeminiEmbeddingAdapter implements EmbeddingPort {

    @Override
    public List<float[]> embed(List<String> texts) {
        return List.of();
    }
}
