package edu.aiplatform.jobs.infrastructure;

import edu.aiplatform.jobs.domain.JobRouting;
import edu.aiplatform.jobs.port.DomainEvent;
import edu.aiplatform.jobs.port.JobMessage;
import java.util.List;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Gửi message sang RabbitMQ, message persistent, có publisher confirm (P12). Không ném lỗi ra ngoài:
 * gửi việc nền lỗi thì sweeper của unit sở hữu gửi lại, event thông báo mất thì chấp nhận (BR-U03-58, 70).
 * Việc nền và event được gửi trên một luồng riêng có hàng đợi giới hạn để RabbitMQ chậm hoặc mất kết nối
 * (timeout kết nối 5 s) không chặn request quá 50 ms (NFR-U03-40, 52); hàng đợi đầy thì bỏ và log WARN.
 */
@Component
public class AmqpPublisher {

    private static final Logger log = LoggerFactory.getLogger(AmqpPublisher.class);
    static final long CONFIRM_TIMEOUT_SECONDS = 2;
    static final int SEND_QUEUE_CAPACITY = 1_000;

    private final RabbitTemplate rabbit;
    private final ExecutorService sender;

    public AmqpPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
        this.sender = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(SEND_QUEUE_CAPACITY), r -> {
                    Thread t = new Thread(r, "amqp-sender");
                    t.setDaemon(true);
                    return t;
                }, (task, executor) -> log.warn("AMQP send queue full; message dropped (sweeper will republish jobs)"));
    }

    @PreDestroy
    void shutdown() {
        sender.shutdown();
        try {
            sender.awaitTermination(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Gửi việc nền tới exchange `jobs`, routing key = `jobType`. Trả về ngay, không chờ RabbitMQ. */
    public void publishJob(JobMessage message) {
        sender.execute(() -> sendJob(message));
    }

    private void sendJob(JobMessage message) {
        try {
            CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
            rabbit.convertAndSend(AmqpTopology.JOBS_EXCHANGE, message.jobType(), message, jobProperties(message, null),
                    correlation);
            correlation.getFuture().orTimeout(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS).whenComplete((confirm, error) -> {
                if (error != null || confirm == null || !confirm.isAck()) {
                    log.warn("Job message not confirmed: jobType={} key={}", message.jobType(), message.idempotencyKey());
                }
            });
        } catch (RuntimeException e) {
            log.warn("Job message not sent: jobType={} key={} error={}", message.jobType(), message.idempotencyKey(),
                    e.toString());
        }
    }

    /**
     * Gửi bản sao vào queue thử lại; chờ confirm vì worker chỉ ack bản gốc khi bản sao đã được RabbitMQ nhận.
     *
     * @return {@code true} khi RabbitMQ xác nhận
     */
    public boolean publishRetry(JobMessage message, String retryQueue) {
        try {
            CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
            rabbit.convertAndSend(AmqpTopology.RETRY_EXCHANGE, retryQueue, message,
                    jobProperties(message, List.of(message.jobType())), correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return confirm.isAck();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Retry message not sent: jobType={} key={} error={}", message.jobType(), message.idempotencyKey(),
                    e.toString());
            return false;
        }
    }

    /** Gửi event thông báo tới `platform.events`, routing key = `eventType`. Trả về ngay, không chờ RabbitMQ. */
    public void publishEvent(DomainEvent event) {
        sender.execute(() -> sendEvent(event));
    }

    private void sendEvent(DomainEvent event) {
        try {
            rabbit.convertAndSend(AmqpTopology.EVENTS_EXCHANGE, event.eventType(), event, m -> {
                m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                m.getMessageProperties().setCorrelationId(event.correlationId());
                return m;
            });
        } catch (RuntimeException e) {
            log.warn("Event not sent: eventType={} eventId={} error={}", event.eventType(), event.eventId(), e.toString());
        }
    }

    private static MessagePostProcessor jobProperties(JobMessage message, List<String> cc) {
        int priority = JobRouting.routeOf(message.jobType()).priority();
        return m -> {
            m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            m.getMessageProperties().setCorrelationId(message.correlationId());
            if (priority > 0) {
                m.getMessageProperties().setPriority(priority);
            }
            if (cc != null) {
                m.getMessageProperties().setHeader("CC", cc);
            }
            return m;
        };
    }
}
