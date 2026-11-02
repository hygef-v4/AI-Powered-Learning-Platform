package edu.aiplatform.jobs.application;

import edu.aiplatform.jobs.contract.IJobHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Gom các {@link IJobHandler}; mỗi loại việc đúng một handler và đúng queue trong bảng định tuyến. */
@Component
public class JobHandlerRegistry {

    private final Map<String, IJobHandler> handlers = new HashMap<>();

    public JobHandlerRegistry(List<IJobHandler> handlers) {
        for (IJobHandler handler : handlers) {
            String jobType = handler.jobType();
            var queue = JobRouting.queueOf(jobType)
                    .orElseThrow(() -> new IllegalStateException("Loại việc chưa có trong bảng định tuyến: " + jobType));
            if (queue != handler.queue()) {
                throw new IllegalStateException("Handler " + jobType + " khai báo sai queue " + handler.queue());
            }
            if (this.handlers.putIfAbsent(jobType, handler) != null) {
                throw new IllegalStateException("Loại việc có hai handler: " + jobType);
            }
        }
    }

    public Optional<IJobHandler> find(String jobType) {
        return Optional.ofNullable(handlers.get(jobType));
    }
}
