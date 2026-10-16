package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;

/**
 * {@code provider} chỉ có khi {@code source = PERSONAL}; {@code targetRef} và {@code inputVersion} để unit gọi
 * kiểm kết quả còn khớp; {@code requestRef} chống gửi trùng.
 */
public record AiTaskRequest(Actor actor, AiSource source, AiProvider provider, String taskType, String targetRef,
                            long inputVersion, String requestRef, AiPrompt prompt, String outputSchema,
                            int maxOutputTokens) {
}
