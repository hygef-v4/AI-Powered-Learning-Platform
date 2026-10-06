package edu.aiplatform.jobs.worker;

import edu.aiplatform.jobs.domain.BackoffPolicy;
import edu.aiplatform.jobs.infrastructure.AmqpPublisher;
import edu.aiplatform.jobs.port.JobMessage;
import org.springframework.stereotype.Component;

/** Gửi bản sao `attempt + 1` vào queue thử lại theo lượt (P9, BR-U03-56). */
@Component
public class JobRetryPublisher {

    private final BackoffPolicy backoff;
    private final AmqpPublisher amqp;

    public JobRetryPublisher(BackoffPolicy backoff, AmqpPublisher amqp) {
        this.backoff = backoff;
        this.amqp = amqp;
    }

    public boolean canRetry(JobMessage message) {
        return backoff.canRetry(message.attempt());
    }

    /** @return {@code true} khi RabbitMQ đã nhận bản sao, bản gốc được ack */
    public boolean publishRetry(JobMessage message) {
        JobMessage next = new JobMessage(message.schemaVersion(), message.jobType(), message.idempotencyKey(),
                message.payload(), message.attempt() + 1, message.correlationId());
        return amqp.publishRetry(next, backoff.retryQueueFor(message.attempt()));
    }
}
