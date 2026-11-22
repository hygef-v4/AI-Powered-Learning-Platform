package edu.aiplatform.files.integration;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import com.google.api.services.drive.Drive;
import edu.aiplatform.files.config.FilesProperties;
import java.time.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DriveHealthIndicatorTest {
    final FilesProperties properties = mock(FilesProperties.class);
    final DriveClientFactory clients = mock(DriveClientFactory.class);
    final DriveFolders folders = mock(DriveFolders.class);
    final Clock clock = mock(Clock.class);
    final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-16T00:00:00Z"));
    final DriveHealthIndicator indicator = new DriveHealthIndicator(properties, clients, folders, clock);

    DriveHealthIndicatorTest() {
        when(clock.instant()).thenAnswer(call -> now.get());
        when(properties.hasDrive()).thenReturn(true);
        when(folders.resolveRoot()).thenReturn("root");
    }

    @AfterEach
    void closeProbe() {
        indicator.closeProbe();
    }

    @Test
    void slowDependencyDoesNotBlockHealthOrLaunchOverlappingProbes() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(clients.createDrive()).thenAnswer(call -> {
            entered.countDown();
            while (release.getCount() > 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    // Simulate provider I/O that does not stop on interruption.
                }
            }
            throw new IllegalStateException("test unavailable");
        });
        try {
            assertThat(indicator.health().getDetails()).containsEntry("drive", "UNKNOWN");
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            now.set(now.get().plusSeconds(3));
            assertTimeoutPreemptively(() -> {
                for (int index = 0; index < 100; index++) {
                    assertThat(indicator.health().getDetails()).containsEntry("drive", "DEGRADED");
                }
            });
            verify(clients, times(1)).createDrive();
        } finally {
            release.countDown();
        }
    }

    private static void assertTimeoutPreemptively(org.junit.jupiter.api.function.Executable action) {
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), action);
    }

    @Test
    void successfulProbeIsCachedThenRefreshed() {
        when(clients.createDrive()).thenReturn(mock(Drive.class, RETURNS_DEEP_STUBS));
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(indicator.health().getDetails()).containsEntry("drive", "UP"));
        for (int index = 0; index < 20; index++) {
            indicator.health();
        }
        verify(clients, times(1)).createDrive();
        now.set(now.get().plusSeconds(61));
        indicator.health();
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> verify(clients, times(2)).createDrive());
    }

    @Test
    void unconfiguredStorageDoesNotCallDrive() {
        when(properties.hasDrive()).thenReturn(false);
        when(properties.requireDrive()).thenReturn(true);
        assertThat(indicator.health().getDetails()).containsEntry("drive", "NOT_CONFIGURED");
        verifyNoInteractions(clients, folders);
    }
}
