package edu.aiplatform.ai.contract;

import edu.aiplatform.shared.error.ApiException;

/**
 * Guardrail hai tầng cho mọi prompt tự do trước khi gọi mô hình: Tầng 1 quy tắc/regex, Tầng 2 ngữ cảnh, quyền, trạng
 * thái và chính sách. Fail-closed: lỗi trong lúc kiểm cũng là từ chối. Dùng bởi U07, U08, U09, U10 và U06.
 */
public interface IPromptGuardrail {

    String REJECTED_CODE = "AI_PROMPT_REJECTED";

    GuardrailVerdict evaluate(UserPromptInput input);

    /** Ném HTTP 400 {@value #REJECTED_CODE} với câu thân thiện khi bị từ chối; không gọi AI, không tốn hạn mức. */
    default void requireValid(UserPromptInput input) {
        GuardrailVerdict verdict = evaluate(input);
        if (verdict == null || !verdict.isAllowed()) {
            String message = verdict == null || verdict.message() == null
                    ? "Yêu cầu chưa được gửi tới AI. Hãy điều chỉnh nội dung và thử lại." : verdict.message();
            throw ApiException.badRequest(REJECTED_CODE, message);
        }
    }
}
