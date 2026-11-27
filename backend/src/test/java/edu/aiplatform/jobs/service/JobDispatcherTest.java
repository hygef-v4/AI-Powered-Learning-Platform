package edu.aiplatform.jobs.service;

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
import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.AfterEach;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class JobDispatcherTest {

    final IMessageSender sender = mock(IMessageSender.class);
    final List<JobDispatcher> dispatchers = new ArrayList<>();

    @AfterEach
    void closeHandlers() {
        dispatchers.forEach(JobDispatcher::closeHandlers);
    }

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

        // 200 ms làm handler ném lỗi ngay vẫn bị tính quá giờ khi các fork test chạy song song làm máy bận.
        @Override
        public Duration timeout() {
            return Duration.ofSeconds(1);
        }
    }

    JobDispatcher dispatcher(FakeHandler handler) {
        JobDispatcher dispatcher = new JobDispatcher(new JobHandlerRegistry(List.of(handler)), sender, Duration.ofSeconds(60));
        dispatchers.add(dispatcher);
        return dispatcher;
    }

    static JobMessage message(int attempt) {
        return new JobMessage(1, JobTypes.DRIVE_CLEANUP, "k", Map.of(), attempt, "c");
    }

    @Test
    void successIsDone() {
        assertThat(dispatcher(new FakeHandler(m -> { })).dispatch(message(0))).isEqualTo(JobDispatcher.Outcome.DONE);
    }

    @Test
    void usesMessageBudgetForMultiCallHandlers() {
        FakeHandler handler = new FakeHandler(m -> {
            // Dài hơn timeout mặc định của FakeHandler để chứng minh budget theo message được dùng.
            try { Thread.sleep(1_200); }
            catch (InterruptedException e) { throw new IllegalStateException(e); }
        }) {
            @Override
            public Duration timeout(JobMessage message) {
                return Duration.ofSeconds(3);
            }
        };
        assertThat(dispatcher(handler).dispatch(message(0))).isEqualTo(JobDispatcher.Outcome.DONE);
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
    void timedOutHandlerThatIgnoresInterruptBlocksASecondRunOfTheSameJob() throws Exception {
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger runs = new java.util.concurrent.atomic.AtomicInteger();
        FakeHandler handler = new FakeHandler(m -> {
            runs.incrementAndGet();
            while (release.getCount() > 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    // Handler không chịu dừng khi bị ngắt.
                }
            }
        });
        JobDispatcher dispatcher = dispatcher(handler);

        assertThat(dispatcher.dispatch(message(0))).isEqualTo(JobDispatcher.Outcome.RETRIED);
        assertThat(dispatcher.dispatch(message(1))).as("lần cũ còn chạy").isEqualTo(JobDispatcher.Outcome.RETRIED);
        assertThat(runs.get()).isEqualTo(1);

        release.countDown();
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3))
                .ignoreException(RejectedExecutionException.class).untilAsserted(() ->
                        assertThat(dispatcher.dispatch(message(0))).isEqualTo(JobDispatcher.Outcome.DONE));
        assertThat(runs.get()).isGreaterThan(1);
    }

    @Test
    void saturatedQueuePreservesUnstartedMessagesAndDoesNotBlockOtherQueues() {
        var release = new java.util.concurrent.CountDownLatch(1);
        var runs = new java.util.concurrent.atomic.AtomicInteger();
        FakeHandler stuck = new FakeHandler(message -> {
            runs.incrementAndGet();
            while (release.getCount() > 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    // Simulate a timed-out handler that is still consuming capacity.
                }
            }
        });
        FakeHandler email = new FakeHandler(message -> { }) {
            @Override
            public String jobType() {
                return JobTypes.EMAIL_SEND;
            }

            @Override
            public JobQueue queue() {
                return JobQueue.EMAIL;
            }
        };
        var dispatcher = new JobDispatcher(new JobHandlerRegistry(List.of(stuck, email)), sender, Duration.ofSeconds(60));
        dispatchers.add(dispatcher);
        try {
            assertThat(dispatcher.dispatch(message(0))).isEqualTo(JobDispatcher.Outcome.RETRIED);
            for (int index = 0; index < 20; index++) {
                var next = new JobMessage(1, JobTypes.DRIVE_CLEANUP, "different-" + index, Map.of(), 5, "c");
                assertThatThrownBy(() -> dispatcher.dispatch(next)).isInstanceOf(RejectedExecutionException.class);
            }
            assertThat(runs.get()).isEqualTo(1);
            assertThat(stuck.failedWith).isNull();
            assertThat(dispatcher.dispatch(new JobMessage(1, JobTypes.EMAIL_SEND, "email", Map.of(), 0, "c")))
                    .isEqualTo(JobDispatcher.Outcome.DONE);
        } finally {
            release.countDown();
        }
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3))
                .ignoreException(RejectedExecutionException.class).untilAsserted(() ->
                        assertThat(dispatcher.dispatch(new JobMessage(1, JobTypes.DRIVE_CLEANUP,
                                "different-0", Map.of(), 0, "c"))).isEqualTo(JobDispatcher.Outcome.DONE));
    }

    @Test
    void safeErrorHasNoStackTraceAndIsShort() {
        assertThat(JobDispatcher.describeSafely(new RuntimeException("x".repeat(500)))).hasSizeLessThan(230);
        assertThat(JobDispatcher.describeSafely(new java.util.concurrent.TimeoutException())).isEqualTo("Quá thời gian xử lý");
        verify(sender, never()).sendJob(any());
        verify(sender, never()).sendEvent(any());
    }
}
