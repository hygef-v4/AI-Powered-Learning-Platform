package edu.aiplatform.jobs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.jobs.contract.IPendingSweeper;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.jobs.contract.PendingJob;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunnersTest {

    final Instant now = Instant.parse("2026-10-16T08:00:00Z");
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Test
    void sweeperResendsJobsPendingForMoreThanFiveMinutes() {
        IPendingSweeper sweeper = mock(IPendingSweeper.class);
        JobPublisher publisher = mock(JobPublisher.class);
        PendingJob pending = new PendingJob("k", Map.of("runId", "r"));
        when(sweeper.jobType()).thenReturn("BACKUP_RUN");
        when(sweeper.findStalePending(now.minusSeconds(300))).thenReturn(List.of(pending));

        new PendingSweepRunner(List.of(sweeper), publisher, clock).runOnce();

        verify(publisher).resend("BACKUP_RUN", pending);
    }

    @Test
    void oneFailingScannerDoesNotStopTheOthers() {
        IScheduledScanner broken = mock(IScheduledScanner.class);
        IScheduledScanner healthy = mock(IScheduledScanner.class);
        doThrow(new RuntimeException("lỗi")).when(broken).scan(any());

        new ScheduledScanRunner(List.of(broken, healthy), clock).runOnce();

        verify(healthy).scan(eq(now));
    }
}
