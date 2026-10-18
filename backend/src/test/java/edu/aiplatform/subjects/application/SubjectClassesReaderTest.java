package edu.aiplatform.subjects.application;

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
    void partialOrInvalidCountsStayUnknownAndExtraKeysAreDiscarded() {
        ISubjectClasses provider = mock(ISubjectClasses.class);
        UUID zero = UUID.randomUUID(), missing = UUID.randomUUID(), negative = UUID.randomUUID(), extra = UUID.randomUUID();
        List<UUID> ids = List.of(zero, missing, negative);
        when(provider.countClasses(ids)).thenReturn(Map.of(zero, 0, negative, -1, extra, 10));
        var reader = new SubjectClassesReader(provider);
        try {
            assertThat(reader.counts(ids)).containsExactlyEntriesOf(Map.of(zero, 0));
            verify(provider).countClasses(ids);
        } finally {
            reader.close();
        }
    }

    @Test
    void nullCountsAreUnavailableAndNeverFabricatedZero() {
        var provider = mock(ISubjectClasses.class);
        when(provider.countClasses(any())).thenReturn(null);
        var reader = new SubjectClassesReader(provider);
        try {
            assertThat(reader.counts(List.of(UUID.randomUUID()))).isEmpty();
        } finally {
            reader.close();
        }
    }

    @Test
    void nullListIsUnavailable() {
        var provider = mock(ISubjectClasses.class);
        when(provider.listClassesOfSubject(any())).thenReturn(null);
        var reader = new SubjectClassesReader(provider);
        try {
            assertThat(reader.classes(UUID.randomUUID()).availability()).isEqualTo(SubjectAdminViews.Availability.UNAVAILABLE);
        } finally {
            reader.close();
        }
    }

    @Test
    void deadlineReturnsUnknownAndInterruptsBlockedProvider() throws Exception {
        var provider = mock(ISubjectClasses.class);
        CountDownLatch entered = new CountDownLatch(1), interrupted = new CountDownLatch(1);
        when(provider.countClasses(any())).thenAnswer(call -> {
            entered.countDown();
            try {
                new CountDownLatch(1).await();
                return Map.of();
            } catch (InterruptedException expected) {
                interrupted.countDown();
                throw expected;
            }
        });
        var reader = new SubjectClassesReader(provider, Duration.ofMillis(100));
        try {
            assertThat(reader.counts(List.of(UUID.randomUUID()))).isEmpty();
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            reader.close();
        }
    }

    @Test
    void interruptedCallerKeepsInterruptFlagAndGetsUnavailable() {
        var provider = mock(ISubjectClasses.class);
        when(provider.listClassesOfSubject(any())).thenAnswer(call -> {
            new CountDownLatch(1).await();
            return List.of();
        });
        var reader = new SubjectClassesReader(provider);
        try {
            Thread.currentThread().interrupt();
            assertThat(reader.classes(UUID.randomUUID()).availability()).isEqualTo(SubjectAdminViews.Availability.UNAVAILABLE);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
            reader.close();
        }
    }

    @Test
    void rejectedTaskAfterShutdownIsUnavailable() {
        var provider = mock(ISubjectClasses.class);
        var reader = new SubjectClassesReader(provider);
        reader.close();
        assertThat(reader.counts(List.of(UUID.randomUUID()))).isEmpty();
        assertThat(reader.classes(UUID.randomUUID()).items()).isNull();
        verifyNoInteractions(provider);
    }
}
