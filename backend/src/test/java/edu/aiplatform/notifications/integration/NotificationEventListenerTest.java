package edu.aiplatform.notifications.integration;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.service.NotificationEventService;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import edu.aiplatform.notifications.config.NotificationTopology;
import org.mockito.ArgumentCaptor;

class NotificationEventListenerTest {
    NotificationEventService service;
    RabbitTemplate rabbit;
    Channel channel;
    ObjectMapper json;
    NotificationEventListener listener;

    @BeforeEach
    void setup() {
        service = mock(NotificationEventService.class);
        rabbit = mock(RabbitTemplate.class);
        channel = mock(Channel.class);
        json = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new NotificationEventListener(service, json, rabbit);
    }

    Message message(int version, int attempts) throws Exception {
        var event =
                new DomainEvent(
                        version,
                        UUID.randomUUID(),
                        EventTypes.BACKUP_FAILED,
                        "U03",
                        Instant.now(),
                        null,
                        Map.of("runId", UUID.randomUUID().toString()));
        var p = new MessageProperties();
        p.setDeliveryTag(1);
        p.setReceivedRoutingKey(EventTypes.BACKUP_FAILED);
        p.setHeader("u14-attempt", attempts);
        return new Message(json.writeValueAsBytes(event), p);
    }

    @Test
    void supportedEventAckedAfterService() throws Exception {
        listener.receive(message(1, 0), channel);
        var order = inOrder(service, channel);
        order.verify(service).accept(any());
        order.verify(channel).basicAck(1, false);
    }

    @Test
    void unknownSchemaAckedWithoutData() throws Exception {
        listener.receive(message(2, 0), channel);
        verifyNoInteractions(service);
        verify(channel).basicAck(1, false);
    }

    @Test
    void malformedRejectedWithoutHotRequeue() throws Exception {
        var p = new MessageProperties();
        p.setDeliveryTag(2);
        listener.receive(new Message("invalid".getBytes(), p), channel);
        verify(channel).basicReject(2, false);
    }

    @Test
    void temporaryFailureSchedulesBoundedRetry() throws Exception {
        doThrow(new IllegalStateException("unavailable")).when(service).accept(any());
        RabbitOperations operations = mock(RabbitOperations.class);
        doAnswer(call -> {
            RabbitOperations.OperationsCallback<?> callback = call.getArgument(0);
            return callback.doInRabbit(operations);
        }).when(rabbit).invoke(any());
        Message original = message(1, 0);
        listener.receive(original, channel);
        var published = ArgumentCaptor.forClass(Message.class);
        var order = inOrder(operations, channel);
        order.verify(operations).send(eq(NotificationTopology.retryExchange(0)),
                eq(EventTypes.BACKUP_FAILED), published.capture());
        order.verify(operations).waitForConfirmsOrDie(5000);
        order.verify(channel).basicAck(1, false);
        assertThat(published.getValue().getBody()).isEqualTo(original.getBody());
        assertThat(published.getValue().getMessageProperties().getHeaders())
                .containsEntry("u14-attempt", 1);
        assertThat(published.getValue().getMessageProperties().getDeliveryMode())
                .isEqualTo(MessageDeliveryMode.PERSISTENT);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void failedRetryConfirmationRequeuesOriginalWithoutAck() throws Exception {
        doThrow(new IllegalStateException("unavailable")).when(service).accept(any());
        RabbitOperations operations = mock(RabbitOperations.class);
        doThrow(new IllegalStateException("confirm failed")).when(operations).waitForConfirmsOrDie(5000);
        doAnswer(call -> {
            RabbitOperations.OperationsCallback<?> callback = call.getArgument(0);
            return callback.doInRabbit(operations);
        }).when(rabbit).invoke(any());
        listener.receive(message(1, 4), channel);
        verify(operations).send(eq(NotificationTopology.retryExchange(4)),
                eq(EventTypes.BACKUP_FAILED), argThat(m -> Integer.valueOf(5).equals(
                        m.getMessageProperties().getHeaders().get("u14-attempt"))));
        verify(channel).basicNack(1, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void exhaustedFailureParked() throws Exception {
        doThrow(new IllegalStateException()).when(service).accept(any());
        listener.receive(message(1, 5), channel);
        verify(channel).basicReject(1, false);
        verifyNoInteractions(rabbit);
    }
}
