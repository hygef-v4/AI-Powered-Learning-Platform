package edu.aiplatform.jobs.application;

import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.JobMessage;

/** Gửi message lên RabbitMQ; tách ra để test không cần broker. */
public interface IMessageSender {

    void sendJob(JobMessage message);

    void sendRetry(JobMessage message, JobRouting.RetryDelay delay);

    void sendEvent(DomainEvent event);
}
