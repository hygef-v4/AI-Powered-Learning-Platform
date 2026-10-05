package edu.aiplatform.jobs.worker;

import edu.aiplatform.jobs.domain.JobRouting;
import edu.aiplatform.jobs.port.JobHandler;
import edu.aiplatform.jobs.port.PendingSweeper;
import edu.aiplatform.jobs.port.ScheduledScanner;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Ánh xạ `jobType` → handler, cùng danh sách sweeper và scanner. Bean {@link JobHandler},
 * {@link PendingSweeper}, {@link ScheduledScanner} của các unit tự được đăng ký khi khởi động.
 */
@Component
public class JobHandlerRegistry implements edu.aiplatform.jobs.port.JobHandlerRegistry {

    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();
    private final List<PendingSweeper> sweepers = new CopyOnWriteArrayList<>();
    private final List<ScheduledScanner> scanners = new CopyOnWriteArrayList<>();

    public JobHandlerRegistry(ObjectProvider<JobHandler> handlerBeans, ObjectProvider<PendingSweeper> sweeperBeans,
                              ObjectProvider<ScheduledScanner> scannerBeans) {
        handlerBeans.orderedStream().forEach(this::register);
        sweeperBeans.orderedStream().forEach(this::registerSweeper);
        scannerBeans.orderedStream().forEach(this::registerScanner);
    }

    /** Handler phải chọn đúng queue của loại việc (BR-U03-63); mỗi loại việc một handler. */
    @Override
    public void register(JobHandler handler) {
        String jobType = handler.jobType();
        if (!JobRouting.isKnown(jobType)) {
            throw new IllegalStateException("Unknown jobType: " + jobType);
        }
        if (JobRouting.routeOf(jobType).queue() != handler.queue()) {
            throw new IllegalStateException("Handler of " + jobType + " must use queue "
                    + JobRouting.routeOf(jobType).queue().queueName());
        }
        JobHandler existing = handlers.putIfAbsent(jobType, handler);
        if (existing != null && existing != handler) {
            throw new IllegalStateException("Duplicate handler for " + jobType);
        }
    }

    @Override
    public void registerSweeper(PendingSweeper sweeper) {
        if (!sweepers.contains(sweeper)) {
            sweepers.add(sweeper);
        }
    }

    @Override
    public void registerScanner(ScheduledScanner scanner) {
        if (!scanners.contains(scanner)) {
            scanners.add(scanner);
        }
    }

    public Optional<JobHandler> handlerFor(String jobType) {
        return Optional.ofNullable(handlers.get(jobType));
    }

    public List<PendingSweeper> sweepers() {
        return List.copyOf(sweepers);
    }

    public List<ScheduledScanner> scanners() {
        return List.copyOf(scanners);
    }
}
