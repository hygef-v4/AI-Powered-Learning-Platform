package edu.aiplatform.backup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.backup.contract.*;
import edu.aiplatform.backup.model.*;
import edu.aiplatform.backup.repository.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

class BackupStatusServiceTest {
    final BackupRunRepository runs = mock(BackupRunRepository.class);
    final BackupStatusService service = new BackupStatusService(runs);
    final Instant to = Instant.parse("2026-10-22T10:00:00Z");
    final Instant from = to.minus(Duration.ofDays(7));

    BackupRun run(BackupState state, String error) {
        return new BackupRun(UUID.randomUUID(), LocalDate.of(2026, 10, 20), state, from,
                from.plusSeconds(1), "schema", "app", "private-path", "private-hash", 12L,
                state == BackupState.CREATED ? null : from.plusSeconds(2), from.plusSeconds(3), to, error);
    }

    @Test void dashboardMetadataPreservesStoredStartAndDoesNotChangeLegacyView() {
        var run = run(BackupState.SUCCESS, "private exception");
        when(runs.latest()).thenReturn(Optional.of(run));
        when(runs.page(any())).thenReturn(new PageImpl<>(List.of(run)));
        var view = service.latestRun().orElseThrow();
        assertThat(view.startedAt()).isEqualTo(from.plusSeconds(2));
        assertThat(view.snapshotAt()).isEqualTo(from.plusSeconds(1));
        assertThat(view.completedAt()).isEqualTo(from.plusSeconds(3));
        assertThat(view.safeError()).isNull();
        assertThat(service.history(PageRequest.of(0, 3)).getContent()).containsExactly(view);
        assertThat(service.latest().orElseThrow()).isEqualTo(run.view());
        when(runs.latest()).thenReturn(Optional.of(run(BackupState.CREATED, null)));
        assertThat(service.latestRun().orElseThrow().startedAt()).isNull();
    }

    @Test void summaryRejectsInvalidWindow() {
        assertThatThrownBy(() -> service.summarize(null, to)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.summarize(from, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.summarize(to, from)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.summarize(to, to)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(runs);
    }

    @Test void failedProjectionDoesNotExposeStoredException() {
        for (String error : Arrays.asList("password=secret /private/archive", null, "<script>")) {
            when(runs.latest()).thenReturn(Optional.of(run(BackupState.FAILED, error)));
            assertThat(service.latestRun().orElseThrow().safeError())
                    .isEqualTo("Backup thất bại. Vui lòng liên hệ người vận hành.");
        }
        for (String error : List.of("Quá thời gian xử lý", "Hết lượt thử lại: Quá thời gian xử lý")) {
            when(runs.latest()).thenReturn(Optional.of(run(BackupState.FAILED, error)));
            assertThat(service.latestRun().orElseThrow().safeError()).isEqualTo("Backup quá thời gian xử lý.");
        }
    }

    @Test void summaryEchoesWindowAndSanitizesFailure() {
        when(runs.summarize(from, to)).thenReturn(new BackupRunRepository.SummaryRow(from.minusSeconds(1), 2, "secret", false));
        var summary = service.summarize(from, to);
        assertThat(summary).isEqualTo(new BackupDashboardSummary(from, to, from.minusSeconds(1), 2,
                "Backup thất bại. Vui lòng liên hệ người vận hành."));
        when(runs.summarize(from, to)).thenReturn(new BackupRunRepository.SummaryRow(null, 0, null, false));
        assertThat(service.summarize(from, to)).isEqualTo(new BackupDashboardSummary(from, to, null, 0, null));
        when(runs.summarize(from, to)).thenReturn(new BackupRunRepository.SummaryRow(null, 0, null, true));
        assertThatThrownBy(() -> service.summarize(from, to)).isInstanceOf(ApiException.class);
    }
}
