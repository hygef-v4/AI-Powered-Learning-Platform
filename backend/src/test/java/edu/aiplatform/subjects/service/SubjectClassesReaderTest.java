package edu.aiplatform.subjects.service;

import edu.aiplatform.subjects.dto.SubjectAdminViews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import edu.aiplatform.subjects.contract.ISubjectClasses;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SubjectClassesReaderTest {
    @Test
    void saturationRejectsWithoutCallerContextAndTimedOutQueueRecovers() throws Exception {
        var subjectClasses = mock(ISubjectClasses.class);
        var started = new CountDownLatch(4);
        var release = new java.util.concurrent.Semaphore(0);
        var recovering = new java.util.concurrent.atomic.AtomicBoolean();
        var leakedContext = new java.util.concurrent.atomic.AtomicBoolean();
        var executions = new java.util.concurrent.atomic.AtomicInteger();
        UUID subject = UUID.randomUUID();
        when(subjectClasses.countClasses(any())).thenAnswer(call -> {
            executions.incrementAndGet();
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
                    || org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
                leakedContext.set(true);
            }
            if (!recovering.get()) {
                started.countDown();
                release.acquireUninterruptibly();
            }
            return Map.of(subject, 0);
        });
        var subjectClassesReader = new SubjectClassesReader(subjectClasses);
        var callers = java.util.concurrent.Executors.newFixedThreadPool(20);
        var field = SubjectClassesReader.class.getDeclaredField("executor");
        field.setAccessible(true);
        var pool = (java.util.concurrent.ThreadPoolExecutor) field.get(subjectClassesReader);
        try {
            var pending = java.util.stream.IntStream.range(0, 20).mapToObj(index -> callers.submit(() -> {
                org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("caller", "unused"));
                try {
                    return subjectClassesReader.countClasses(List.of(subject));
                } finally {
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                    org.springframework.transaction.support.TransactionSynchronizationManager.clear();
                }
            })).toList();
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
            while (pool.getQueue().size() < 16 && System.nanoTime() < deadline) Thread.yield();
            assertThat(pool.getPoolSize()).isEqualTo(4);
            assertThat(pool.getQueue()).hasSize(16);
            long rejectionStarted = System.nanoTime();
            assertThat(subjectClassesReader.countClasses(List.of(subject))).isEmpty();
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - rejectionStarted)).isLessThan(500);
            for (var caller : pending) assertThat(caller.get(2, TimeUnit.SECONDS)).isEmpty();
            assertThat(pool.getQueue()).isEmpty();
            assertThat(executions.get()).isEqualTo(4);
            assertThat(leakedContext.get()).isFalse();
            recovering.set(true);
            release.release(4);
            assertThat(
                    subjectClassesReader.countClasses(List.of(subject))).containsEntry(subject, 0);
        } finally {
            recovering.set(true);
            release.release(20);
            subjectClassesReader.close();
            callers.shutdownNow();
        }
    }

    @Test
    void partialOrInvalidCountsStayUnknownAndExtraKeysAreDiscarded() {
        ISubjectClasses subjectClasses = mock(ISubjectClasses.class);
        UUID zero = UUID.randomUUID(),
                missing = UUID.randomUUID(),
                negative = UUID.randomUUID(),
                extra = UUID.randomUUID();
        List<UUID> ids = List.of(zero, missing, negative);
        when(subjectClasses.countClasses(ids)).thenReturn(Map.of(zero, 0, negative, -1, extra, 10));
        var subjectClassesReader = new SubjectClassesReader(subjectClasses);
        try {
            assertThat(
                    subjectClassesReader
                            .countClasses(ids)).containsExactlyEntriesOf(Map
                            .of(zero, 0));
            verify(subjectClasses).countClasses(ids);
        } finally {
            subjectClassesReader.close();
        }
    }

    @Test
    void nullCountsAreUnavailableAndNeverFabricatedZero() {
        var subjectClasses = mock(ISubjectClasses.class);
        when(subjectClasses.countClasses(any())).thenReturn(null);
        var subjectClassesReader = new SubjectClassesReader(subjectClasses);
        try {
            assertThat(subjectClassesReader.countClasses(List.of(UUID.randomUUID()))).isEmpty();
        } finally {
            subjectClassesReader.close();
        }
    }

    @Test
    void nullListIsUnavailable() {
        var subjectClasses = mock(ISubjectClasses.class);
        when(subjectClasses.listClassesOfSubject(any())).thenReturn(null);
        var subjectClassesReader = new SubjectClassesReader(subjectClasses);
        try {
            assertThat(subjectClassesReader.listClasses(UUID.randomUUID()).availability())
                    .isEqualTo(SubjectAdminViews.Availability.UNAVAILABLE);
        } finally {
            subjectClassesReader.close();
        }
    }

    @Test
    void deadlineReturnsUnknownAndInterruptsBlockedProvider() throws Exception {
        var subjectClasses = mock(ISubjectClasses.class);
        CountDownLatch entered = new CountDownLatch(1), interrupted = new CountDownLatch(1);
        when(subjectClasses.countClasses(any()))
                .thenAnswer(
                        call -> {
                            entered.countDown();
                            try {
                                new CountDownLatch(1).await();
                                return Map.of();
                            } catch (InterruptedException expected) {
                                interrupted.countDown();
                                throw expected;
                            }
                        });
        var subjectClassesReader = new SubjectClassesReader(subjectClasses, Duration.ofMillis(100));
        try {
            assertThat(subjectClassesReader.countClasses(List.of(UUID.randomUUID()))).isEmpty();
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            subjectClassesReader.close();
        }
    }

    @Test
    void interruptedCallerKeepsInterruptFlagAndGetsUnavailable() {
        var subjectClasses = mock(ISubjectClasses.class);
        when(subjectClasses.listClassesOfSubject(any()))
                .thenAnswer(
                        call -> {
                            new CountDownLatch(1).await();
                            return List.of();
                        });
        var subjectClassesReader = new SubjectClassesReader(subjectClasses);
        try {
            Thread.currentThread().interrupt();
            assertThat(subjectClassesReader.listClasses(UUID.randomUUID()).availability())
                    .isEqualTo(SubjectAdminViews.Availability.UNAVAILABLE);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
            subjectClassesReader.close();
        }
    }

    @Test
    void rejectedTaskAfterShutdownIsUnavailable() {
        var subjectClasses = mock(ISubjectClasses.class);
        var subjectClassesReader = new SubjectClassesReader(subjectClasses);
        subjectClassesReader.close();
        assertThat(subjectClassesReader.countClasses(List.of(UUID.randomUUID()))).isEmpty();
        assertThat(subjectClassesReader.listClasses(UUID.randomUUID()).items()).isNull();
        verifyNoInteractions(subjectClasses);
    }
}
