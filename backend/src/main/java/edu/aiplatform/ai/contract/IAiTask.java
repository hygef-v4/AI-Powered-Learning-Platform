package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Gửi task AI theo nguồn người dùng chọn, giữ chỗ quota, chạy nền, trả trạng thái. Dùng bởi U07, U08, U09, U10. */
public interface IAiTask {

    AiTaskView submit(AiTaskRequest request);

    /**
     * Kiểm nguồn người dùng chọn dùng được chưa (quyền, khóa API cá nhân, API trường) trước khi unit gọi tạo bản ghi
     * của mình; chưa dùng được thì ném cùng lỗi như {@link #submit}.
     */
    void requireSource(Actor actor, AiSource source, AiProvider provider);

    /**
     * Nguồn tự chọn cho một lượt (yêu cầu 2026-11-05), theo thứ tự thử: API chung trước nếu người gọi được dùng, API
     * trường đang bật và hạn mức còn lại đủ cho ước tính token của chính lượt này; sau đó API cá nhân đã lưu khóa.
     * Rỗng khi không còn nguồn nào dùng được.
     */
    default List<AiSourceChoice> sourceOrder(Actor actor, AiPrompt prompt, int maxOutputTokens) {
        return List.of();
    }

    Optional<AiTaskView> get(UUID taskId);

    void cancel(Actor actor, UUID taskId);
}
