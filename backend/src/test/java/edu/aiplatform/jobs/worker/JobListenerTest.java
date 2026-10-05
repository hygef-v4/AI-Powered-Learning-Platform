package edu.aiplatform.jobs.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.jobs.application.JobProperties;
import edu.aiplatform.jobs.domain.BackoffPolicy;
import edu.aiplatform.jobs.infrastructure.AmqpPublisher;
import edu.aiplatform.jobs.port.JobHandler;
import edu.aiplatform.jobs.port.JobMessage;
import edu.aiplatform.jobs.port.JobQueue;
import edu.aiplatform.jobs.port.JobTypes;
import edu.aiplatform.jobs.port.PendingSweeper;
import edu.aiplatform.jobs.port.RetryableJobException;
import edu.aiplatform.jobs.port.ScheduledScanner;
import edu.aiplatform.jobs.worker.JobListener.Outcome;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/** BR-U03-51, 53, 56, 59, 71; NFR-U03-46; P9. */
class JobListenerTest {

    private static final List<Duration> BACKOFF = List.of(Duration.ofSeconds(30), Duration.ofMinutes(1),
            Duration.ofMinutes(2), Duration.ofMinutes(4), Duration.ofMinutes(8));

    private final ObjectMapper mapper = new ObjectMapper();
    private final AmqpPublisher amqp = mock(AmqpPublisher.class);
    private final RecordingHandler handler = new RecordingHandler();
    private final JobListener listener = listener(handler, Duration.ofSeconds(5));

    @AfterEach
    void shutdown() {
        listener.shutdown();
    }

    @Test
    void successIsAckedOnce() throws Exception {
        assertThat(listener.process(body(0))).isEqualTo(Outcome.ACK);
        assertThat(handler.handled).hasSize(1);
        assertThat(handler.failed).isEmpty();
    }

    @Test
    void transientErrorGoesToNextRetryQueueWithAttemptPlusOne() throws Exception {
        handler.behaviour = m -> { throw new RetryableJobException("SMTP busy", null); };
        when(amqp.publishRetry(any(), anyString())).thenReturn(true);

        assertThat(listener.process(body(2))).isEqualTo(Outcome.ACK);

        ArgumentCaptor<JobMessage> copy = ArgumentCaptor.forClass(JobMessage.class);
        verify(amqp).publishRetry(copy.capture(), eq("jobs.retry.2m"));
        assertThat(copy.getValue().attempt()).isEqualTo(3);
        assertThat(handler.failed).isEmpty();
    }

    @Test
    void exhaustedRetriesCallOnFailed() throws Exception {
        handler.behaviour = m -> { throw new RetryableJobException("still busy", null); };

        assertThat(listener.process(body(5))).isEqualTo(Outcome.ACK);
        verify(amqp, never()).publishRetry(any(), anyString());
        assertThat(handler.failed).hasSize(1);
    }

    @Test
    void permanentErrorCallsOnFailedWithoutRetry() throws Exception {
        handler.behaviour = m -> { throw new IllegalStateException("bad data"); };

        assertThat(listener.process(body(0))).isEqualTo(Outcome.ACK);
        verify(amqp, never()).publishRetry(any(), anyString());
        assertThat(handler.failed).hasSize(1);
    }

    @Test
    void timeoutIsTreatedAsTransient() throws Exception {
        handler.behaviour = m -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        when(amqp.publishRetry(any(), anyString())).thenReturn(true);
        JobListener fast = listener(handler, Duration.ofMillis(100));
        try {
            assertThat(fast.process(body(0))).isEqualTo(Outcome.ACK);
            verify(amqp).publishRetry(any(), eq("jobs.retry.30s"));
        } finally {
            fast.shutdown();
        }
    }

    @Test
    void brokerRefusingRetryCopyRequeuesOriginal() throws Exception {
        handler.behaviour = m -> { throw new RetryableJobException("busy", null); };
        when(amqp.publishRetry(any(), anyString())).thenReturn(false);

        assertThat(listener.process(body(0))).isEqualTo(Outcome.REQUEUE);
    }

    @Test
    void unsupportedSchemaOrGarbageIsDroppedWithoutCallingHandler() throws Exception {
        byte[] v2 = mapper.writeValueAsBytes(new JobMessage(2, JobTypes.OTP_DELIVERY, "k", Map.of(), 0, "c"));
        assertThat(listener.process(v2)).isEqualTo(Outcome.ACK);
        assertThat(listener.process("not json".getBytes())).isEqualTo(Outcome.ACK);
        assertThat(handler.handled).isEmpty();
    }

    @Test
    void channelIsAckedOnlyAfterHandlerAndNackedForRequeue() throws Exception {
        com.rabbitmq.client.Channel channel = mock(com.rabbitmq.client.Channel.class);
        org.springframework.amqp.core.MessageProperties props = new org.springframework.amqp.core.MessageProperties();
        props.setDeliveryTag(42L);

        listener.onMessage(new org.springframework.amqp.core.Message(body(0), props), channel);
        verify(channel).basicAck(42L, false);
        assertThat(handler.handled).hasSize(1);

        handler.behaviour = m -> { throw new RetryableJobException("busy", null); };
        when(amqp.publishRetry(any(), anyString())).thenReturn(false);
        listener.onMessage(new org.springframework.amqp.core.Message(body(0), props), channel);
        verify(channel).basicNack(42L, false, true);
    }

    @Test
    void onFailedErrorStillAcks() throws Exception {
        handler.behaviour = m -> { throw new IllegalStateException("bad"); };
        handler.failOnFailed = true;
        assertThat(listener.process(body(0))).isEqualTo(Outcome.ACK);
    }

    @Test
    void sweeperRepublishesStaleJobsAndScannerErrorsAreIsolated() {
        JobMessage stale = new JobMessage(1, JobTypes.OTP_DELIVERY, "k", Map.of(), 0, "c");
        PendingSweeper sweeper = new PendingSweeper() {
            public String jobType() { return JobTypes.OTP_DELIVERY; }
            public List<JobMessage> findStalePending(java.time.Instant olderThan) { return List.of(stale); }
        };
        List<String> ran = new ArrayList<>();
        ScheduledScanner broken = scanner("broken", () -> { throw new IllegalStateException("db"); });
        ScheduledScanner ok = scanner("ok", () -> ran.add("ok"));
        JobHandlerRegistry registry = registry(handler, sweeper, broken, ok);

        new PendingSweepRunner(registry, amqp, Duration.ofMinutes(5), java.time.Clock.systemUTC()).run();
        new ScheduledScanRunner(registry, java.time.Clock.systemUTC()).run();

        verify(amqp).publishJob(stale);
        assertThat(ran).containsExactly("ok");
    }

    private JobListener listener(JobHandler h, Duration timeout) {
        JobProperties props = new JobProperties(5, BACKOFF, Duration.ofMinutes(1), Duration.ofMinutes(5),
                Duration.ofMinutes(1), timeout, 4);
        return new JobListener(registry(h), new JobRetryPublisher(new BackoffPolicy(BACKOFF), amqp), mapper, props);
    }

    private static JobHandlerRegistry registry(Object... beans) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        for (int i = 0; i < beans.length; i++) {
            factory.addBean("bean" + i, beans[i]);
        }
        return new JobHandlerRegistry(factory.getBeanProvider(JobHandler.class),
                factory.getBeanProvider(PendingSweeper.class), factory.getBeanProvider(ScheduledScanner.class));
    }

    private static ScheduledScanner scanner(String name, Runnable action) {
        return new ScheduledScanner() {
            public String name() { return name; }
            public void scan(java.time.Instant now) { action.run(); }
        };
    }

    private byte[] body(int attempt) throws Exception {
        return mapper.writeValueAsBytes(new JobMessage(1, JobTypes.OTP_DELIVERY, "a1:ACTIVATION",
                Map.of("accountId", "a1"), attempt, "corr-1234"));
    }

    static class RecordingHandler implements JobHandler {
        final List<JobMessage> handled = new ArrayList<>();
        final List<JobMessage> failed = new ArrayList<>();
        Consumer<JobMessage> behaviour = m -> {};
        boolean failOnFailed;

        public String jobType() { return JobTypes.OTP_DELIVERY; }
        public JobQueue queue() { return JobQueue.EMAIL; }
        public void handle(JobMessage message) {
            behaviour.accept(message);
            handled.add(message);
        }
        public void onFailed(JobMessage message, Throwable lastError) {
            failed.add(message);
            if (failOnFailed) {
                throw new IllegalStateException("cannot mark failed");
            }
        }
        public Optional<Duration> timeout() { return Optional.empty(); }
    }
}
