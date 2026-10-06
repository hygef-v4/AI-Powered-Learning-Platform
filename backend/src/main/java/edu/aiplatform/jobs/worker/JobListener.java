package edu.aiplatform.jobs.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import edu.aiplatform.jobs.application.JobProperties;
import edu.aiplatform.jobs.application.JobPublisher;
import edu.aiplatform.jobs.port.JobHandler;
import edu.aiplatform.jobs.port.JobMessage;
import edu.aiplatform.jobs.port.RetryableJobException;
import edu.aiplatform.shared.web.CorrelationIdFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

/**
 * Xử lý một message việc nền (J2): gọi handler của unit sở hữu, ack thủ công sau khi handler xong (BR-U03-53).
 * Lỗi tạm hoặc quá timeout và còn lượt → gửi vào queue thử lại rồi ack; lỗi vĩnh viễn hoặc hết lượt → `onFailed`,
 * log ERROR, ack (BR-U03-56). Message không đọc được hoặc sai `schemaVersion` → log WARN, ack (BR-U03-71).
 */
public class JobListener implements ChannelAwareMessageListener {

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);

    public enum Outcome { ACK, REQUEUE }

    private final JobHandlerRegistry registry;
    private final JobRetryPublisher retryPublisher;
    private final ObjectMapper objectMapper;
    private final Duration defaultTimeout;
    private final ExecutorService executor;

    public JobListener(JobHandlerRegistry registry, JobRetryPublisher retryPublisher, ObjectMapper objectMapper,
                       JobProperties properties) {
        this.registry = registry;
        this.retryPublisher = retryPublisher;
        this.objectMapper = objectMapper;
        this.defaultTimeout = properties.handlerTimeout();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "job-handler");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        if (process(message.getBody()) == Outcome.ACK) {
            channel.basicAck(tag, false);
        } else {
            channel.basicNack(tag, false, true);
        }
    }

    public Outcome process(byte[] body) {
        JobMessage message;
        try {
            message = objectMapper.readValue(body, JobMessage.class);
        } catch (IOException e) {
            log.warn("Unreadable job message dropped: {}", e.getMessage());
            return Outcome.ACK;
        }
        if (message.schemaVersion() != JobPublisher.SCHEMA_VERSION) {
            log.warn("Unsupported job schemaVersion {} for jobType={}", message.schemaVersion(), message.jobType());
            return Outcome.ACK;
        }
        JobHandler handler = registry.handlerFor(message.jobType()).orElse(null);
        if (handler == null) {
            log.error("No handler registered for jobType={} key={}", message.jobType(), message.idempotencyKey());
            return Outcome.ACK;
        }
        if (message.correlationId() != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, message.correlationId());
        }
        try {
            runWithTimeout(handler, message);
            return Outcome.ACK;
        } catch (RetryableJobException | TimeoutException e) {
            return onTransientFailure(handler, message, e);
        } catch (Exception e) {
            return fail(handler, message, e);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    private Outcome onTransientFailure(JobHandler handler, JobMessage message, Exception error) {
        if (!retryPublisher.canRetry(message)) {
            return fail(handler, message, error);
        }
        if (retryPublisher.publishRetry(message)) {
            log.info("Job retry scheduled: jobType={} key={} attempt={}", message.jobType(), message.idempotencyKey(),
                    message.attempt() + 1);
            return Outcome.ACK;
        }
        log.warn("Retry not accepted by broker, requeueing jobType={} key={}", message.jobType(),
                message.idempotencyKey());
        return Outcome.REQUEUE;
    }

    private Outcome fail(JobHandler handler, JobMessage message, Exception error) {
        log.error("Job failed: jobType={} key={} attempt={} error={}", message.jobType(), message.idempotencyKey(),
                message.attempt(), error.toString());
        try {
            handler.onFailed(message, error);
        } catch (RuntimeException e) {
            // Dòng nghiệp vụ vẫn ở trạng thái chờ; sweeper của unit sở hữu sẽ gửi lại.
            log.error("onFailed threw for jobType={} key={}: {}", message.jobType(), message.idempotencyKey(),
                    e.toString());
        }
        return Outcome.ACK;
    }

    private void runWithTimeout(JobHandler handler, JobMessage message) throws Exception {
        Duration timeout = handler.timeout().orElse(defaultTimeout);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        Future<?> future = executor.submit(() -> {
            if (correlationId != null) {
                MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
            }
            try {
                handler.handle(message);
            } finally {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            }
        });
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RetryableJobException("Interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw e;
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
