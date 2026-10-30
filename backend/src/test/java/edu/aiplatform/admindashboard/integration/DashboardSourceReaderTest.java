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
            var ready = reader.submit(() -> 7);
            // Wait for completion, independent of a wall-clock timeout assertion.
            ready.future().get(1, TimeUnit.SECONDS);
            now.set(101);
            assertThat(reader.await(ready).value()).isEqualTo(7);
        }
    }

    @Test void lateCompletionAndProviderFailuresAreUnavailable() throws Exception {
        AtomicLong now = new AtomicLong();
        try (var reader = new DashboardSourceReader(pool(1, 2), Duration.ofNanos(100), now::get)) {
            var late = reader.submit(() -> { now.set(101); return 9; });
            late.future().get(1, TimeUnit.SECONDS);
            assertThat(reader.await(late).available()).isFalse();
            assertThat(reader.await(reader.submit(() -> { throw new IllegalStateException("secret"); })).available()).isFalse();
        }
    }

    @Test void queuedSourcesShareTheirOriginalDeadlineAndCannotRunOnCaller() throws Exception {
        AtomicLong now = new AtomicLong();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        var executor = pool(1, 1);
        try (var reader = new DashboardSourceReader(executor, Duration.ofNanos(100), now::get)) {
            var blocked = reader.submit(() -> { entered.countDown(); release.await(); return 1; });
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            var queued = reader.submit(() -> 2);
            assertThat(reader.await(reader.submit(() -> 3)).available()).isFalse();
            now.set(101);
            assertThat(reader.await(blocked).available()).isFalse();
            assertThat(reader.await(queued).available()).isFalse();
            assertThat(executor.getLargestPoolSize()).isEqualTo(1);
        } finally { release.countDown(); }
    }

    @Test void interruptionIsRetainedAndOutstandingWorkIsCancelled() {
        try (var reader = new DashboardSourceReader()) {
            var pending = reader.submit(() -> 1);
            Thread.currentThread().interrupt();
            assertThat(reader.await(pending).available()).isFalse();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }

    @Test void independentSourcesCanStartBeforeEitherIsAwaited() throws Exception {
        CountDownLatch both = new CountDownLatch(2);
        try (var reader = new DashboardSourceReader()) {
            Callable<Integer> work = () -> { both.countDown(); return both.await(1, TimeUnit.SECONDS) ? 1 : 0; };
            var first = reader.submit(work);
            var second = reader.submit(work);
            assertThat(reader.await(first).value()).isEqualTo(1);
            assertThat(reader.await(second).value()).isEqualTo(1);
        }
    }

    @Test void aCompletedExceptionCannotBecomeSuccessfulData() {
        try (var reader = new DashboardSourceReader()) {
            var pending = reader.submit(() -> { throw new IllegalStateException("secret"); });
            assertThatThrownBy(() -> pending.future().get(1, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);
            assertThat(reader.await(pending).available()).isFalse();
        }
    }

    @Test void closeRejectsFurtherWorkWithoutCallingIt() {
        var reader = new DashboardSourceReader();
        reader.close();
        assertThat(reader.await(reader.submit(() -> { throw new AssertionError("Must not run"); })).available()).isFalse();
    }
}
