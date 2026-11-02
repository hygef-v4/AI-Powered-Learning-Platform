package edu.aiplatform.admindashboard.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.admindashboard.application.AdminDashboardViews.Availability;
import edu.aiplatform.backup.contract.*;
import edu.aiplatform.settings.contract.SettingKey;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

class AdminDashboardBackupTest {
    @Test void noHistoryIsAvailableButMissingMetadataRemainsUnavailable() {
        try (var f = new DashboardFixture()) {
            var backup = f.service.dashboard(f.admin).backup();
            assertThat(backup.latest().availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(backup.latest().data()).isNull();
            assertThat(backup.recentRuns().data()).isEmpty();
            assertThat(backup.latestSuccessfulAt().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(backup.failuresLast7Days().data()).isNull();
            assertThat(backup.schedule().data().enabled()).isFalse();
            assertThat(backup.schedule().data().nextPlannedAt()).isNull();
        }
    }
    @Test void allRunStatesAndPurgedMetadataPreserveStoredStartedAt() {
        try (var f = new DashboardFixture()) {
            for (BackupState state : BackupState.values()) {
                var run = new BackupDashboardRunView(LocalDate.of(2026, 10, 20), state, f.now, f.now.minusSeconds(1), f.now, 123L, "Lỗi đã che", f.now);
                when(f.backups.latestRun()).thenReturn(Optional.of(run));
                doAnswer(call -> new PageImpl<>(List.of(run), call.getArgument(0), 1)).when(f.backups).history(any());
                var backup = f.service.dashboard(f.admin).backup();
                assertThat(backup.latest().data().state()).isEqualTo(state);
                assertThat(backup.latest().data().startedAt()).isEqualTo(f.now.minusSeconds(1));
                assertThat(backup.latest().data().purgedAt()).isEqualTo(f.now);
                assertThat(f.service.backups(f.admin, 0, 20).items()).hasSize(1);
            }
        }
    }
    @Test void summaryUsesOneFixedWindowAndPreservesEmptySuccess() {
        try (var f = new DashboardFixture()) {
            Instant from = f.now.minus(Duration.ofDays(7));
            when(f.backups.summarize(from, f.now)).thenReturn(new BackupDashboardSummary(from, f.now, null, 0, null));
            var backup = f.service.dashboard(f.admin).backup();
            assertThat(backup.latestSuccessfulAt().availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(backup.latestSuccessfulAt().data()).isNull();
            assertThat(backup.failuresLast7Days().data().count()).isZero();
            verify(f.backups).summarize(from, f.now);
        }
    }
    @Test void invalidSummaryOnlyDegradesSummarySections() {
        try (var f = new DashboardFixture()) {
            Instant from = f.now.minus(Duration.ofDays(7));
            for (var invalid : List.of(
                    new BackupDashboardSummary(from.plusSeconds(1), f.now, null, 0, null),
                    new BackupDashboardSummary(from, f.now, null, -1, null),
                    new BackupDashboardSummary(from, f.now, f.now, 0, null))) {
                when(f.backups.summarize(from, f.now)).thenReturn(invalid);
                var dashboard = f.service.dashboard(f.admin);
                assertThat(dashboard.backup().latestSuccessfulAt().availability()).isEqualTo(Availability.UNAVAILABLE);
                assertThat(dashboard.backup().failuresLast7Days().availability()).isEqualTo(Availability.UNAVAILABLE);
                assertThat(dashboard.backup().latest().availability()).isEqualTo(Availability.AVAILABLE);
                assertThat(dashboard.accounts().availability()).isEqualTo(Availability.AVAILABLE);
            }
        }
    }
    @Test void failedSummaryKeepsHealthySectionsAndHistoricalSuccessIsNotWindowLimited() {
        try (var f = new DashboardFixture()) {
            Instant from = f.now.minus(Duration.ofDays(7));
            when(f.backups.summarize(from, f.now)).thenReturn(new BackupDashboardSummary(
                    from, f.now, from.minusSeconds(1), 2, "Backup quá thời gian xử lý."));
            var backup = f.service.dashboard(f.admin).backup();
            assertThat(backup.latestSuccessfulAt().data()).isEqualTo(from.minusSeconds(1));
            assertThat(backup.failuresLast7Days().data().count()).isEqualTo(2);
            when(f.backups.summarize(from, f.now)).thenThrow(new IllegalStateException("private exception"));
            var dashboard = f.service.dashboard(f.admin);
            assertThat(dashboard.backup().latestSuccessfulAt().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(dashboard.backup().failuresLast7Days().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(dashboard.backup().recentRuns().availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(dashboard.accounts().availability()).isEqualTo(Availability.AVAILABLE);
        }
    }
    @Test void settingsUseVietnamTimeAndNoDefaultDailyHour() {
        try (var f = new DashboardFixture()) {
            when(f.settings.get(new SettingKey<>("backup.enabled", Boolean.class))).thenReturn(Optional.of(true));
            var missingHour = f.service.dashboard(f.admin).backup().schedule();
            assertThat(missingHour.availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(missingHour.data().enabled()).isTrue();
            assertThat(missingHour.data().nextPlannedAt()).as("bật mà chưa đặt giờ thì chưa có lần chạy").isNull();
            when(f.settings.get(new SettingKey<>("backup.dailyAt", LocalTime.class))).thenReturn(Optional.of(LocalTime.of(11, 0)));
            var scheduled = f.service.dashboard(f.admin).backup().schedule().data();
            assertThat(scheduled.timeZone()).isEqualTo("Asia/Ho_Chi_Minh");
            assertThat(scheduled.nextPlannedAt()).isEqualTo(Instant.parse("2026-10-20T04:00:00Z"));
            when(f.settings.get(new SettingKey<>("backup.dailyAt", LocalTime.class))).thenReturn(Optional.of(LocalTime.of(9, 0)));
            assertThat(f.service.dashboard(f.admin).backup().schedule().data().nextPlannedAt()).isEqualTo(Instant.parse("2026-10-21T02:00:00Z"));
        }
    }
    @Test void failedHistoryIsUnavailableNotSuccessfulEmptyAndInvalidPagingCannotRead() {
        try (var f = new DashboardFixture()) {
            doThrow(new IllegalStateException("secret")).when(f.backups).history(any());
            assertThat(f.service.dashboard(f.admin).backup().recentRuns().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThatThrownBy(() -> f.service.backups(f.admin, 0, 20)).isInstanceOfSatisfying(ApiException.class,
                    e -> assertThat(e.code()).isEqualTo("DASHBOARD_BACKUP_UNAVAILABLE"));
            reset(f.backups);
            assertThatThrownBy(() -> f.service.backups(f.admin, -1, 20)).isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> f.service.backups(f.admin, 0, 101)).isInstanceOf(ApiException.class);
            verifyNoInteractions(f.backups);
        }
    }
}
