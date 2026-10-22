package edu.aiplatform.ai.application;

import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Handler kết quả theo loại task do unit gọi cài (U07–U10); mỗi loại task đúng một handler. */
@Component
public class AiTaskResultHandlerRegistry {

    private final Map<String, IAiTaskResultHandler> handlers = new HashMap<>();

    public AiTaskResultHandlerRegistry(List<IAiTaskResultHandler> handlers) {
        for (IAiTaskResultHandler handler : handlers) {
            if (this.handlers.putIfAbsent(handler.taskType(), handler) != null) {
                throw new IllegalStateException("Loại task AI có hai handler: " + handler.taskType());
            }
        }
    }

    public Optional<IAiTaskResultHandler> find(String taskType) {
        return Optional.ofNullable(handlers.get(taskType));
    }
}
