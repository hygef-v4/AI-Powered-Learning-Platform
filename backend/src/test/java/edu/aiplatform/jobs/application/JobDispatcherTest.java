package edu.aiplatform.jobs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.aiplatform.jobs.contract.IJobHandler;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobQueue;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.jobs.contract.RetryableJobException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class JobDispatcherTest {

    final IMessageSender sender = mock(IMessageSender.class);

    static class FakeHandler implements IJobHandler {
        final Consumer<JobMessage> body;
        String failedWith;

        FakeHandler(Consumer<JobMessage> body) {
            this.body = body;
        }

        @Override
        public String jobType() {
            return JobTypes.DRIVE_CLEANUP;
        }

        @Override
        public JobQueue queue() {
            return JobQueue.DRIVE;
        }

        @Override
        public void handle(JobMessage message) {
            body.accept(message);
        }

        @Override
        public void onFailed(JobMessage message, String error) {
            failedWith = error;
        }

        @Override
        public Duration timeout() {
            return Duration.ofMillis(200);
        }
    }

    JobDispatcher dispatcher(FakeHandler handler) {
        return new JobDispatcher(new JobHandlerRegistry(List.of(handler)), sender, Duration.ofSeconds(60));
    }

    static JobMessage message(int attempt) {
        return new JobMessage(1, JobTypes.DRIVE_CLEANUP, "k", Map.of(), attempt, "c");
    }

    @Test
    void successIsDone() {
        assertThat(dispatcher(new FakeHandler(m -> { })).dispatch(message(0))).isEqualTo(JobDispatcher.Outcome.DONE);
    }

    @Test
    void temporaryErrorIsRetriedWithBackoffAndNextAttempt() {
        FakeHandler handler = new FakeHandler(m -> {
            throw new RetryableJobException("429");
        });

        assertThat(dispatcher(handler).dispatch(message(2))).isEqualTo(JobDispatcher.Outcome.RETRIED);

        ArgumentCaptor<JobMessage> copy = ArgumentCaptor.forClass(JobMessage.class);
        verify(sender).sendRetry(copy.capture(), eq(JobRouting.RETRY_DELAYS.get(2)));
        assertThat(copy.getValue().attempt()).isEqualTo(3);
        assertThat(JobRouting.RETRY_DELAYS.get(2).delay()).isEqualTo(Duration.ofMinutes(2));
        assertThat(handler.failedWith).isNull();
    }

    @Test
    void afterTheFifthRetryTheJobFails() {
        FakeHandler handler = new FakeHandler(m -> {
            throw new RetryableJobException("vẫn lỗi");
        });

        assertThat(dispatcher(handler).dispatch(message(5))).isEqualTo(JobDispatcher.Outcome.FAILED);
        verify(sender, never()).sendRetry(any(), any());
        assertThat(handler.failedWith).contains("Hết lượt");
    }

    @Test
    void permanentErrorFailsAtOnce() {
        FakeHandler handler = new FakeHandler(m -> {
            throw new IllegalStateException("dữ liệu sai");
        });

        assertThat(dispatcher(handler).dispatch(message(0))).isEqualTo(JobDispatcher.Outcome.FAILED);
        assertThat(handler.failedWith).isNotBlank().doesNotContain("dữ liệu sai");
    }

    @Test
    void terminalFailurePersistenceErrorIsPropagatedWithoutLoggingItsMessage(CapturedOutput output) {
        RuntimeException persistenceFailure = new IllegalStateException("db-password-secret-SENTINEL");
        FakeHandler handler = new FakeHandler(m -> { throw new IllegalArgumentException("bad input"); }) {
            @Override
            public void onFailed(JobMessage message, String error) {
                throw persistenceFailure;
            }
        };

        assertThatThrownBy(() -> dispatcher(handler).dispatch(message(0))).isSameAs(persistenceFailure);
        assertThat(output.getAll()).doesNotContain("db-password-secret-SENTINEL");
    }

    @Test
    void arbitraryProviderErrorIsRedactedBeforeFailurePersistenceAndLogging(CapturedOutput output) {
        String secret = "opaque-provider-secret-SENTINEL";
        FakeHandler handler = new FakeHandler(m -> { throw new IllegalStateException(secret); });

        assertThat(dispatcher(handler).dispatch(message(0))).isEqualTo(JobDispatcher.Outcome.FAILED);

        assertThat(handler.failedWith).isNotBlank().doesNotContain(secret);
        assertThat(output.getAll()).doesNotContain(secret);
    }

    @Test
    void exhaustedRetryErrorIsRedactedBeforeFailurePersistenceAndLogging(CapturedOutput output) {
        String secret = "unlabelled-retry-secret-SENTINEL";
        FakeHandler handler = new FakeHandler(m -> { throw new RetryableJobException(secret); });

        assertThat(dispatcher(handler).dispatch(message(5))).isEqualTo(JobDispatcher.Outcome.FAILED);

        assertThat(handler.failedWith).contains("Hết lượt").doesNotContain(secret);
        assertThat(output.getAll()).doesNotContain(secret);
    }

    @Test
    void timeoutCountsAsTemporaryError() {
        FakeHandler handler = new FakeHandler(m -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertThat(dispatcher(handler).dispatch(message(0))).isEqualTo(JobDispatcher.Outcome.RETRIED);
    }

    @Test
    void unsupportedSchemaOrUnknownJobIsDiscarded() {
        JobDispatcher dispatcher = dispatcher(new FakeHandler(m -> { }));

        assertThat(dispatcher.dispatch(new JobMessage(2, JobTypes.DRIVE_CLEANUP, "k", Map.of(), 0, "c")))
                .isEqualTo(JobDispatcher.Outcome.DISCARDED);
        assertThat(dispatcher.dispatch(new JobMessage(1, JobTypes.EMAIL_SEND, "k", Map.of(), 0, "c")))
                .isEqualTo(JobDispatcher.Outcome.DISCARDED);
        verify(sender, never()).sendRetry(any(), any());
    }

    @Test
    void safeErrorHasNoStackTraceAndIsShort() {
        assertThat(JobDispatcher.safe(new RuntimeException("x".repeat(500)))).hasSizeLessThan(230);
        assertThat(JobDispatcher.safe(new java.util.concurrent.TimeoutException())).isEqualTo("Quá thời gian xử lý");
        verify(sender, never()).sendJob(any());
        verify(sender, never()).sendEvent(any());
    }
}
