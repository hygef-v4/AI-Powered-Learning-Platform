package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.Optional;
import java.util.UUID;

/** Gửi task AI theo nguồn người dùng chọn, giữ chỗ quota, chạy nền, trả trạng thái. Dùng bởi U07, U08, U09, U10. */
public interface IAiTask {

    AiTaskView submit(AiTaskRequest request);

    Optional<AiTaskView> get(UUID taskId);

    void cancel(Actor actor, UUID taskId);
}
