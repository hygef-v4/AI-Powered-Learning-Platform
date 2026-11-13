package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler kết quả theo loại task do unit gọi cài (U07–U10); mỗi loại task đúng một handler. Handler thường phụ thuộc
 * service của unit đó, mà service lại dùng {@code IAiTask}; nên danh sách handler chỉ được lấy ở lần {@link #find}
 * đầu tiên, không lấy lúc dựng bean, để không tạo vòng phụ thuộc khi khởi động.
 */
@Component
public class AiTaskResultHandlerRegistry {

    private final Supplier<List<IAiTaskResultHandler>> source;
    private volatile Map<String, IAiTaskResultHandler> handlers;

    @Autowired
    public AiTaskResultHandlerRegistry(ObjectProvider<IAiTaskResultHandler> handlers) {
        this(() -> handlers.orderedStream().toList());
    }

    AiTaskResultHandlerRegistry(Supplier<List<IAiTaskResultHandler>> source) {
        this.source = source;
    }

    /** Dùng trong test với danh sách cố định. */
    static AiTaskResultHandlerRegistry of(List<IAiTaskResultHandler> handlers) {
        return new AiTaskResultHandlerRegistry(() -> handlers);
    }

    public Optional<IAiTaskResultHandler> find(String taskType) {
        return Optional.ofNullable(handlers().get(taskType));
    }

    private Map<String, IAiTaskResultHandler> handlers() {
        Map<String, IAiTaskResultHandler> current = handlers;
        if (current == null) {
            synchronized (this) {
                if (handlers == null) {
                    Map<String, IAiTaskResultHandler> built = new HashMap<>();
                    for (IAiTaskResultHandler handler : source.get()) {
                        if (built.putIfAbsent(handler.taskType(), handler) != null) {
                            throw new IllegalStateException("Loại task AI có hai handler: " + handler.taskType());
                        }
                    }
                    handlers = Map.copyOf(built);
                }
                current = handlers;
            }
        }
        return current;
    }
}
