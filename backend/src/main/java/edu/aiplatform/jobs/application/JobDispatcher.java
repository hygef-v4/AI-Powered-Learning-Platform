package edu.aiplatform.jobs.application;

import edu.aiplatform.jobs.contract.IJobHandler;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final ExecutorService runner = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "job-handler");
        thread.setDaemon(true);
        return thread;
    });

    public JobDispatcher(JobHandlerRegistry registry, IMessageSender sender,
                         @Value("${platform.jobs.handler-timeout:60s}") Duration defaultTimeout) {
        this.registry = registry;
        this.sender = sender;
        this.defaultTimeout = defaultTimeout;
    }

    public Outcome dispatch(JobMessage message) {
        if (message.schemaVersion() != JobPublisher.SCHEMA_VERSION) {
            log.warn("Bỏ việc {} có schemaVersion {} không hỗ trợ", message.jobType(), message.schemaVersion());
            return Outcome.DISCARDED;
        }
        IJobHandler handler = registry.find(message.jobType()).orElse(null);
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
                sender.sendRetry(next(message), delay);
                return Outcome.RETRIED;
            }
            return fail(handler, message, "Hết lượt thử lại: " + safe(e));
        } catch (Exception e) {
            return fail(handler, message, safe(e));
        }
    }

    private void runWithTimeout(IJobHandler handler, JobMessage message) throws Exception {
        Duration timeout = handler.timeout() == null ? defaultTimeout : handler.timeout();
        Future<?> future = runner.submit(() -> handler.handle(message));
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException(cause);
        }
    }

    private Outcome fail(IJobHandler handler, JobMessage message, String error) {
        log.error("Việc {} key={} thất bại: {}", message.jobType(), message.idempotencyKey(), error);
        try {
            handler.onFailed(message, error);
        } catch (RuntimeException e) {
            log.error("onFailed của việc {} lỗi", message.jobType(), e);
        }
        return Outcome.FAILED;
    }

    static JobMessage next(JobMessage message) {
        return new JobMessage(message.schemaVersion(), message.jobType(), message.idempotencyKey(), message.payload(),
                message.attempt() + 1, message.correlationId());
    }

    /** Lỗi đã che: chỉ loại lỗi và thông điệp ngắn, không có stack trace. */
    static String safe(Throwable e) {
        if (e instanceof TimeoutException) {
            return "Quá thời gian xử lý";
        }
        String message = e.getMessage() == null ? "" : e.getMessage();
        return e.getClass().getSimpleName() + (message.isEmpty() ? "" : ": "
                + message.substring(0, Math.min(200, message.length())));
    }
}
