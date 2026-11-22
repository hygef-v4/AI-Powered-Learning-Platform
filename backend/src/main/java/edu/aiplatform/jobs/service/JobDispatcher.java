package edu.aiplatform.jobs.service;

import edu.aiplatform.jobs.contract.IJobHandler;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobQueue;
import edu.aiplatform.jobs.contract.RetryableJobException;
import edu.aiplatform.jobs.contract.SafeJobErrors;
import java.time.Duration;
import jakarta.annotation.PreDestroy;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Xử lý một {@code JobMessage} trong worker (J2): gọi handler có timeout; lỗi tạm hoặc quá hạn thì gửi lại theo
 * backoff tới khi {@code attempt} đạt 5; lỗi vĩnh viễn hoặc hết lượt thì gọi {@code onFailed} (BR-U03-56).
 */
@Component
public class JobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);

    public enum Outcome { DONE, RETRIED, FAILED, DISCARDED }

    private final JobHandlerRegistry registry;
    private final IMessageSender sender;
    private final Duration defaultTimeout;
    /** Việc đang chạy theo {@code jobType:idempotencyKey}, gồm cả lần đã quá giờ mà handler chưa dừng. */
    private final Map<String, Object> running = new ConcurrentHashMap<>();
    private final Map<JobQueue, ExecutorService> executors = new EnumMap<>(JobQueue.class);

    public JobDispatcher(JobHandlerRegistry registry, IMessageSender sender,
                         @Value("${platform.jobs.handler-timeout:60s}") Duration defaultTimeout) {
        this.registry = registry;
        this.sender = sender;
        this.defaultTimeout = defaultTimeout;
        for (JobQueue queue : JobQueue.values()) {
            int capacity = switch (queue) {
                case TRIGGERED -> 2;
                case AI -> 4;
                default -> 1;
            };
            executors.put(queue, new ThreadPoolExecutor(capacity, capacity, 0, TimeUnit.MILLISECONDS,
                    new SynchronousQueue<>(), runnable -> {
                        Thread thread = new Thread(runnable, "job-handler-" + queue.name().toLowerCase(java.util.Locale.ROOT));
                        thread.setDaemon(true);
                        return thread;
                    }, new ThreadPoolExecutor.AbortPolicy()));
        }
    }

    public Outcome dispatch(JobMessage message) {
        if (message.schemaVersion() != JobPublisher.SCHEMA_VERSION) {
            log.warn("Bỏ việc {} có schemaVersion {} không hỗ trợ", message.jobType(), message.schemaVersion());
            return Outcome.DISCARDED;
        }
        IJobHandler handler = registry.findHandler(message.jobType()).orElse(null);
        if (handler == null) {
            log.error("Không có handler cho việc {}", message.jobType());
            return Outcome.DISCARDED;
        }
        try {
            runWithTimeout(handler, message);
            return Outcome.DONE;
        } catch (RetryableJobException | TimeoutException e) {
            if (message.attempt() < JobRouting.MAX_ATTEMPT) {
                JobRouting.RetryDelay delay = JobRouting.RETRY_DELAYS.get(message.attempt());
                log.warn("Việc {} key={} lỗi tạm, thử lại sau {}", message.jobType(), message.idempotencyKey(),
                        delay.label());
                sender.sendRetry(buildNextAttempt(message), delay);
                return Outcome.RETRIED;
            }
            return fail(handler, message, "Hết lượt thử lại: " + describeSafely(e));
        } catch (RejectedExecutionException e) {
            // Capacity is infrastructure backpressure: listener requeues the original message without
            // consuming a business retry or persisting a false terminal failure.
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Worker đang dừng", e);
        } catch (Exception e) {
            return fail(handler, message, describeSafely(e));
        }
    }

    private void runWithTimeout(IJobHandler handler, JobMessage message) throws Exception {
        Duration configuredTimeout = handler.timeout(message);
        Duration timeout = configuredTimeout == null ? defaultTimeout : configuredTimeout;
        // Hết giờ chỉ ngắt được handler chịu interrupt: lần chạy cũ còn sống thì bản gửi lại (retry, sweeper) chờ lượt
        // sau, không chạy song song cùng một việc.
        String key = message.jobType() + ":" + message.idempotencyKey();
        Object token = new Object();
        if (running.putIfAbsent(key, token) != null) {
            throw new RetryableJobException("Lần chạy trước của việc này chưa dừng");
        }
        Future<?> future;
        // 0 = waiting, 1 = entered handler, 2 = cancelled before entry.
        AtomicInteger execution = new AtomicInteger();
        try {
            future = executors.get(handler.queue()).submit(() -> {
                if (!execution.compareAndSet(0, 1)) {
                    return;
                }
                try {
                    handler.handle(message);
                } finally {
                    running.remove(key, token);
                }
            });
        } catch (RuntimeException e) {
            running.remove(key, token);
            throw e;
        }
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException e) {
            future.cancel(true);
            if (execution.compareAndSet(0, 2)) {
                running.remove(key, token);
            }
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException(cause);
        }
    }

    @PreDestroy
    public void closeHandlers() {
        executors.values().forEach(ExecutorService::shutdownNow);
    }

    private Outcome fail(IJobHandler handler, JobMessage message, String error) {
        log.error("Việc {} key={} thất bại: {}", message.jobType(), message.idempotencyKey(), error);
        // Persistence errors must reach the listener so that it requeues the original delivery.
        handler.onFailed(message, error);
        return Outcome.FAILED;
    }

    static JobMessage buildNextAttempt(JobMessage message) {
        return new JobMessage(message.schemaVersion(), message.jobType(), message.idempotencyKey(), message.payload(),
                message.attempt() + 1, message.correlationId());
    }

    /** Provider/DB exception messages are untrusted, including when they look short or harmless. */
    static String describeSafely(Throwable e) {
        return SafeJobErrors.from(e);
    }
}
