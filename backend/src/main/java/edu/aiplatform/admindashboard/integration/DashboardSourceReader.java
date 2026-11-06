package edu.aiplatform.admindashboard.integration;

import edu.aiplatform.admindashboard.dto.AdminDashboardViews.Section;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/** Bounded read-only fan-out. Each deadline includes queue time; cancelling a Future is best effort. */
@Component
public class DashboardSourceReader implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final long timeoutNanos;
    private final LongSupplier ticker;

    public DashboardSourceReader() {
        this(new ThreadPoolExecutor(8, 8, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(32),
                task -> {
                    Thread thread = new Thread(task, "u15-dashboard-source");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy()), Duration.ofSeconds(2), System::nanoTime);
    }

    DashboardSourceReader(ThreadPoolExecutor executor, Duration timeout, LongSupplier ticker) {
        if (timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException("Timeout must be positive");
        this.executor = executor;
        this.timeoutNanos = timeout.toNanos();
        this.ticker = ticker;
    }

    public record TimedResult<T>(T value, long completedAt) {
    }

    public record PendingSource<T>(Future<TimedResult<T>> future, long deadline) {
    }

    public record SourceResult<T>(boolean available, T value) {
        static <T> SourceResult<T> createUnavailableResult() {
            return new SourceResult<>(false, null);
        }
    }

    public <T> PendingSource<T> submitSource(Callable<T> work) {
        long deadline = ticker.getAsLong() + timeoutNanos;
        try {
            return new PendingSource<>(
                    executor.submit(() -> new TimedResult<>(work.call(), ticker.getAsLong())),
                    deadline);
        } catch (RejectedExecutionException rejected) {
            return new PendingSource<>(null, deadline);
        }
    }

    public <T> SourceResult<T> awaitSource(PendingSource<T> pending) {
        Future<TimedResult<T>> future = pending.future();
        if (future == null)
            return SourceResult.createUnavailableResult();
        try {
            if (Thread.currentThread().isInterrupted())
                return SourceResult.createUnavailableResult();
            long remaining = pending.deadline() - ticker.getAsLong();
            TimedResult<T> result;
            if (future.isDone())
                result = future.get();
            else if (remaining > 0)
                result = future.get(remaining, TimeUnit.NANOSECONDS);
            else
                return SourceResult.createUnavailableResult();
            return result.completedAt() - pending.deadline() <= 0
                    ? new SourceResult<>(true, result.value())
                    : SourceResult.createUnavailableResult();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return SourceResult.createUnavailableResult();
        } catch (ExecutionException | TimeoutException | CancellationException unavailable) {
            return SourceResult.createUnavailableResult();
        } finally {
            if (!future.isDone()) {
                future.cancel(true);
                executor.purge();
            }
        }
    }

    public <T, R> Section<R> readSection(PendingSource<T> pending, Function<T, R> mapper) {
        var result = awaitSource(pending);
        if (!result.available() || result.value() == null)
            return Section.unavailable();
        try {
            return Section.available(mapper.apply(result.value()));
        } catch (RuntimeException invalidData) {
            return Section.unavailable();
        }
    }

    @Override
    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }
}
