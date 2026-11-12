package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;

/**
 * Văn bản người dùng tự nhập kèm ngữ cảnh do server xác định. {@code text} là đúng chữ người dùng gõ, không phải prompt
 * đã ghép; tài liệu nguồn, bài nộp và dữ liệu hệ thống không đi qua đây mà được đóng gói bằng {@link UntrustedPrompt}.
 */
public record UserPromptInput(Actor actor, PromptContext context, PromptAction action, PromptFlowState state,
                              String text) {

    /** Ngữ cảnh lấy theo thao tác, tránh bên gọi khai báo lệch nhau. */
    public static UserPromptInput of(Actor actor, PromptAction action, PromptFlowState state, String text) {
        return new UserPromptInput(actor, action == null ? null : action.context(), action, state, text);
    }
}
