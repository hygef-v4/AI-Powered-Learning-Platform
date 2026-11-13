package edu.aiplatform.admindashboard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class DashboardSourceReaderTest {
    private ThreadPoolExecutor pool(int workers, int queued) {
        return new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queued), new ThreadPoolExecutor.AbortPolicy());
    }

    @Test void healthyResultsCompletedBeforeDeadlineSurviveLateAwait() throws Exception {
        AtomicLong now = new AtomicLong();
        try (var reader = new DashboardSourceReader(pool(2, 2), Duration.ofNanos(100), now::get)) {
            var ready = reader.submitSource(() -> 7);
            // Wait for completion, independent of a wall-clock timeout assertion.
            ready.future().get(1, TimeUnit.SECONDS);
            now.set(101);
            assertThat(reader.awaitSource(ready).value()).isEqualTo(7);
        }
    }

    @Test void lateCompletionAndProviderFailuresAreUnavailable() throws Exception {
        AtomicLong now = new AtomicLong();
        try (var reader = new DashboardSourceReader(pool(1, 2), Duration.ofNanos(100), now::get)) {
            var late = reader.submitSource(() -> { now.set(101); return 9; });
            late.future().get(1, TimeUnit.SECONDS);
            assertThat(reader.awaitSource(late).available()).isFalse();
            assertThat(reader.awaitSource(reader.submitSource(() -> { throw new IllegalStateException("secret"); })).available()).isFalse();
        }
    }

    @Test void queuedSourcesShareTheirOriginalDeadlineAndCannotRunOnCaller() throws Exception {
        AtomicLong now = new AtomicLong();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        var executor = pool(1, 1);
        try (var reader = new DashboardSourceReader(executor, Duration.ofNanos(100), now::get)) {
            var blocked = reader.submitSource(() -> { entered.countDown(); release.await(); return 1; });
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            var queued = reader.submitSource(() -> 2);
            assertThat(reader.awaitSource(reader.submitSource(() -> 3)).available()).isFalse();
            now.set(101);
            assertThat(reader.awaitSource(blocked).available()).isFalse();
            assertThat(reader.awaitSource(queued).available()).isFalse();
            assertThat(executor.getLargestPoolSize()).isEqualTo(1);
        } finally { release.countDown(); }
    }

    @Test void interruptionIsRetainedAndOutstandingWorkIsCancelled() {
        try (var reader = new DashboardSourceReader()) {
            var pending = reader.submitSource(() -> 1);
            Thread.currentThread().interrupt();
            assertThat(reader.awaitSource(pending).available()).isFalse();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }

    @Test void independentSourcesCanStartBeforeEitherIsAwaited() throws Exception {
        CountDownLatch both = new CountDownLatch(2);
        try (var reader = new DashboardSourceReader()) {
            Callable<Integer> work = () -> { both.countDown(); return both.await(1, TimeUnit.SECONDS) ? 1 : 0; };
            var first = reader.submitSource(work);
            var second = reader.submitSource(work);
            assertThat(reader.awaitSource(first).value()).isEqualTo(1);
            assertThat(reader.awaitSource(second).value()).isEqualTo(1);
        }
    }

    @Test void aCompletedExceptionCannotBecomeSuccessfulData() {
        try (var reader = new DashboardSourceReader()) {
            var pending = reader.submitSource(() -> { throw new IllegalStateException("secret"); });
            assertThatThrownBy(() -> pending.future().get(1, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);
            assertThat(reader.awaitSource(pending).available()).isFalse();
        }
    }

    @Test void closeRejectsFurtherWorkWithoutCallingIt() {
        var reader = new DashboardSourceReader();
        reader.close();
        assertThat(reader.awaitSource(reader.submitSource(() -> { throw new AssertionError("Must not run"); })).available()).isFalse();
    }
}
