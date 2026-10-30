package edu.aiplatform.jobs.integration;

import edu.aiplatform.jobs.service.IMessageSender;
import edu.aiplatform.jobs.service.JobRouting;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.JobMessage;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Gửi message persistent, chờ publisher confirm tối đa 2 giây (P12). */
@Component
public class RabbitMessageSender implements IMessageSender {

    static final long CONFIRM_TIMEOUT_MS = 2_000;

    private final RabbitTemplate rabbit;

    public RabbitMessageSender(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void sendJob(JobMessage message) {
        send(JobRouting.JOBS_EXCHANGE, message.jobType(), message, JobRouting.priorityOf(message.jobType()));
    }

    @Override
    public void sendRetry(JobMessage message, JobRouting.RetryDelay delay) {
        send(delay.exchange(), message.jobType(), message, JobRouting.priorityOf(message.jobType()));
    }

    @Override
    public void sendEvent(DomainEvent event) {
        send(JobRouting.EVENTS_EXCHANGE, event.eventType(), event, 0);
    }

    private void send(String exchange, String routingKey, Object body, int priority) {
        MessagePostProcessor persistent = message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setPriority(priority);
            return message;
        };
        rabbit.invoke(operations -> {
            operations.convertAndSend(exchange, routingKey, body, persistent);
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
            return null;
        });
    }
}
