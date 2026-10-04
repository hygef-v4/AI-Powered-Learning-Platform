package edu.aiplatform.aiexecution.ai;

import edu.aiplatform.aiexecution.port.*;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U13: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U13: Adapter Gemini.
 */
@Component
public class GeminiAdapter implements AiGateway {

    @Override
    public AiResult generate(String task, AiPrompt prompt, String jsonSchema) {
        throw new UnsupportedOperationException("Chưa cài: U13");
    }
}
