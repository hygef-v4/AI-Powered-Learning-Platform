package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;

/**
 * {@code provider} chỉ có khi {@code source = PERSONAL}; {@code targetRef} và {@code inputVersion} để unit gọi
 * kiểm kết quả còn khớp; {@code requestRef} chống gửi trùng. {@code userInput} là chữ tự do người dùng nhập (nếu có);
 * U06 kiểm lại bằng {@link IPromptGuardrail} trước khi giữ quota, và bắt buộc với loại task nhận chữ tự do.
 */
public record AiTaskRequest(Actor actor, AiSource source, AiProvider provider, String taskType, String targetRef,
                            long inputVersion, String requestRef, AiPrompt prompt, String outputSchema,
                            int maxOutputTokens, UserPromptInput userInput) {

    /** Task không có chữ tự do của người dùng (chấm bài, góp ý luyện tập). */
    public AiTaskRequest(Actor actor, AiSource source, AiProvider provider, String taskType, String targetRef,
                         long inputVersion, String requestRef, AiPrompt prompt, String outputSchema,
                         int maxOutputTokens) {
        this(actor, source, provider, taskType, targetRef, inputVersion, requestRef, prompt, outputSchema,
                maxOutputTokens, null);
    }
}
