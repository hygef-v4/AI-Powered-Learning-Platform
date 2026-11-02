package edu.aiplatform.subjects.service;

import edu.aiplatform.subjects.dto.SubjectAdminViews;

import edu.aiplatform.subjects.contract.ISubjectClasses;
import edu.aiplatform.subjects.contract.SubjectClassSummary;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Chỉ dùng cho hiển thị Admin; command archive phải có guard/transaction riêng ở B09. */
@Component
public class SubjectClassesReader {
    private final ISubjectClasses provider;
    private final ThreadPoolExecutor executor;
    private final Duration timeout;

    @Autowired
    public SubjectClassesReader(ISubjectClasses provider) {
        this(provider, Duration.ofSeconds(1));
    }

    SubjectClassesReader(ISubjectClasses provider, Duration timeout) {
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout phải dương");
        }
        this.provider = provider;
        this.timeout = timeout;
        // Không chạy provider trên caller khi đầy; provider bỏ qua interrupt cũng không tạo vô hạn thread.
        this.executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(16), task -> {
                    Thread thread = new Thread(task, "u04-subject-classes");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    /** Missing key, null hoặc số âm đều là unknown, không suy ra 0 lớp. */
    public Map<UUID, Integer> counts(Collection<UUID> subjectIds) {
        List<UUID> ids = List.copyOf(subjectIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> result = read(() -> {
            Map<UUID, Integer> raw = provider.countClasses(ids);
            if (raw == null) {
                return Map.of();
            }
            Map<UUID, Integer> known = new HashMap<>();
            for (UUID id : ids) {
                Integer count = raw.get(id);
                if (count != null && count >= 0) {
                    known.put(id, count);
                }
            }
            return Map.copyOf(known);
        });
        return result == null ? Map.of() : result;
    }

    public SubjectAdminViews.Classes classes(UUID subjectId) {
        List<SubjectClassSummary> items = read(() -> {
            var raw = provider.listClassesOfSubject(subjectId);
            return raw == null ? null : List.copyOf(raw);
        });
        return new SubjectAdminViews.Classes(items == null ? SubjectAdminViews.Availability.UNAVAILABLE
                : SubjectAdminViews.Availability.AVAILABLE, items);
    }

    private <T> T read(Callable<T> call) {
        Future<T> future = null;
        try {
            future = executor.submit(call);
            return future.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException | RejectedExecutionException unavailable) {
            return null;
        } finally {
            if (future != null && !future.isDone()) {
                future.cancel(true);
                executor.purge();
            }
        }
    }

    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }
}
