package edu.aiplatform.jobs.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Channel;
import edu.aiplatform.jobs.service.IMessageSender;
import edu.aiplatform.jobs.service.JobDispatcher;
import edu.aiplatform.jobs.service.JobHandlerRegistry;
import edu.aiplatform.jobs.contract.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class JobListenerTest {
    final ObjectMapper mapper = new ObjectMapper();
    final Channel channel = mock(Channel.class);
    final JobDispatcher dispatcher = mock(JobDispatcher.class);
    final JobMessage job = new JobMessage(1, JobTypes.BACKUP_RUN, "backup:day", Map.of(), 0, "c");

    Message delivery(byte[] body) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(42L);
        return new Message(body, properties);
    }

    @Test
    void successfulDurableDispatchIsAcknowledged() throws Exception {
        when(dispatcher.dispatch(job)).thenReturn(JobDispatcher.Outcome.DONE);

        new JobListener(dispatcher, mapper).handle(delivery(mapper.writeValueAsBytes(job)), channel);

        verify(channel).basicAck(42L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void dispatchFailureRequeuesWithoutAcknowledging() throws Exception {
        when(dispatcher.dispatch(job)).thenThrow(new IllegalStateException("database unavailable"));

        new JobListener(dispatcher, mapper).handle(delivery(mapper.writeValueAsBytes(job)), channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(42L, false, true);
    }

    @Test
    void retryHandoffFailureRequeuesOriginalDeliveryWithoutAcknowledging() throws Exception {
        IMessageSender sender = mock(IMessageSender.class);
        doThrow(new IllegalStateException("broker unavailable")).when(sender).sendRetry(any(), any());
        IJobHandler handler = new IJobHandler() {
            public String jobType() { return JobTypes.BACKUP_RUN; }
            public JobQueue queue() { return JobQueue.BACKUP; }
            public Duration timeout() { return Duration.ofSeconds(1); }
            public void handle(JobMessage ignored) { throw new RetryableJobException("temporary failure"); }
            public void onFailed(JobMessage ignored, String error) { throw new AssertionError("must retry"); }
        };
        JobDispatcher realDispatcher = new JobDispatcher(new JobHandlerRegistry(List.of(handler)), sender,
                Duration.ofSeconds(1));

        new JobListener(realDispatcher, mapper).handle(delivery(mapper.writeValueAsBytes(job)), channel);

        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel).basicNack(42L, false, true);
    }

    @Test
    void malformedDeliveryIsDiscardedAndAcknowledged() throws Exception {
        new JobListener(dispatcher, mapper).handle(delivery("not json".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                channel);

        verifyNoInteractions(dispatcher);
        verify(channel).basicAck(42L, false);
    }

    @ParameterizedTest
    @MethodSource("malformedEnvelopes")
    void invalidEnvelopeIsDiscardedWithoutDispatchOrRequeue(String body) throws Exception {
        new JobListener(dispatcher, mapper).handle(delivery(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                channel);

        verifyNoInteractions(dispatcher);
        verify(channel).basicAck(42L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    static Stream<String> malformedEnvelopes() {
        ObjectMapper json = new ObjectMapper();
        ObjectNode valid = json.valueToTree(new JobMessage(1, JobTypes.BACKUP_RUN, "backup:day", Map.of(), 0, "c"));
        List<String> cases = new ArrayList<>(List.of("null", "{}"));
        for (String required : List.of("schemaVersion", "jobType", "idempotencyKey", "payload", "attempt",
                "correlationId")) {
            ObjectNode missing = valid.deepCopy();
            missing.remove(required);
            cases.add(missing.toString());
            ObjectNode nullValue = valid.deepCopy();
            nullValue.putNull(required);
            cases.add(nullValue.toString());
        }
        cases.add(valid.deepCopy().put("attempt", -1).toString());
        cases.add(valid.deepCopy().put("attempt", 6).toString());
        cases.add(valid.deepCopy().put("attempt", 0.5).toString());
        cases.add(valid.deepCopy().put("attempt", "0").toString());
        cases.add(valid.deepCopy().put("schemaVersion", 0).toString());
        cases.add(valid.deepCopy().put("schemaVersion", "1").toString());
        cases.add(valid.deepCopy().put("jobType", " ").toString());
        cases.add(valid.deepCopy().put("idempotencyKey", " ").toString());
        cases.add(valid.deepCopy().put("correlationId", " ").toString());
        ObjectNode arrayPayload = valid.deepCopy();
        arrayPayload.putArray("payload");
        cases.add(arrayPayload.toString());
        return cases.stream();
    }

    @Test
    void finalAllowedRetryAttemptStillReachesDispatcher() throws Exception {
        JobMessage finalAttempt = new JobMessage(1, JobTypes.BACKUP_RUN, "backup:day", Map.of(), 5, "c");
        when(dispatcher.dispatch(finalAttempt)).thenReturn(JobDispatcher.Outcome.FAILED);

        new JobListener(dispatcher, mapper).handle(delivery(mapper.writeValueAsBytes(finalAttempt)), channel);

        verify(dispatcher).dispatch(finalAttempt);
        verify(channel).basicAck(42L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
}
