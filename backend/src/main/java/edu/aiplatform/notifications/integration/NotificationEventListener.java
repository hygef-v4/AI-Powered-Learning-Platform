package edu.aiplatform.notifications.integration;

import edu.aiplatform.notifications.config.NotificationTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.notifications.service.NotificationEventService;
import java.io.IOException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
public class NotificationEventListener {
    private final NotificationEventService notificationEventService;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    public NotificationEventListener(
            NotificationEventService notificationEventService,
            ObjectMapper objectMapper,
            RabbitTemplate rabbitTemplate) {
        this.notificationEventService = notificationEventService;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = NotificationTopology.QUEUE, ackMode = "MANUAL", concurrency = "1")
    public void receiveEventMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        DomainEvent event;
        try {
            if (message.getBody().length > 262144)
                throw new IllegalArgumentException("Event too large");
            event = objectMapper.readValue(message.getBody(), DomainEvent.class);
            if (event == null
                    || event.schemaVersion() != 1
                    || event.eventType() == null
                    || !NotificationEventService.SOURCES.containsKey(event.eventType())) {
                channel.basicAck(tag, false);
                return;
            }
            if (!event.eventType().equals(message.getMessageProperties().getReceivedRoutingKey()))
                throw new IllegalArgumentException("Event routing mismatch");
        } catch (IOException | IllegalArgumentException malformed) {
            channel.basicReject(tag, false);
            return;
        }
        try {
            notificationEventService.handleEvent(event);
            channel.basicAck(tag, false);
        } catch (IllegalArgumentException malformed) {
            channel.basicReject(tag, false);
        } catch (RuntimeException unavailable) {
            Object header = message.getMessageProperties().getHeaders().get("u14-attempt");
            int attempt = header instanceof Number number ? number.intValue() : 0;
            if (attempt < 0 || attempt >= NotificationTopology.RETRY_MS.length) {
                channel.basicReject(tag, false);
                return;
            }
            var properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            properties.setHeader("u14-attempt", attempt + 1);
            try {
                rabbitTemplate.invoke(
                        operations -> {
                            operations.send(
                                    NotificationTopology.buildRetryExchange(attempt),
                                    event.eventType(),
                                    new Message(message.getBody(), properties));
                            operations.waitForConfirmsOrDie(5000);
                            return null;
                        });
                channel.basicAck(tag, false);
            } catch (RuntimeException publishFailed) {
                channel.basicNack(tag, false, true);
            }
        }
    }
}
