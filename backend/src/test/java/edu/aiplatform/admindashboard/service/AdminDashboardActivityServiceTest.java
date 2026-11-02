package edu.aiplatform.admindashboard.service;

import edu.aiplatform.admindashboard.dto.AdminDashboardViews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.admindashboard.dto.AdminDashboardViews.Availability;
import edu.aiplatform.admindashboard.dto.AdminDashboardViews.DayAssignments;
import edu.aiplatform.assignments.contract.AssignmentDayCount;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditLogView;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAuditStats;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdminDashboardActivityServiceTest {
    private final IAuditStats audit = mock(IAuditStats.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);

    private AdminDashboardActivityService service(DashboardFixture f) {
        return new AdminDashboardActivityService(f.service, audit, assignments, f.reader, Clock.fixed(f.now, ZoneOffset.UTC));
    }

    @Test
    void activeChangeTodayIsGainedMinusLostSinceLocalMidnight() {
        try (var f = new DashboardFixture()) {
            // 2026-10-20T03:00Z là 10:00 giờ Việt Nam: đầu ngày là 2026-10-19T17:00Z.
            Instant midnight = Instant.parse("2026-10-19T17:00:00Z");
            when(audit.countSucceeded(eq("ACCOUNT_ACTIVATED"), eq(midnight), eq(f.now), isNull(), isNull())).thenReturn(3L);
            when(audit.countSucceeded("ACCOUNT_ENABLED", midnight, f.now, "status", "ACTIVE")).thenReturn(1L);
            when(audit.countSucceeded("ACCOUNT_DISABLED", midnight, f.now, "before", "ACTIVE")).thenReturn(6L);

            var view = service(f).activity(f.admin);

            assertThat(view.activeAccountChangeToday().availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(view.activeAccountChangeToday().data()).isEqualTo(-2L);
        }
    }

    @Test
    void assignmentsCoverSevenDaysWithZerosForQuietDays() {
        try (var f = new DashboardFixture()) {
            when(assignments.countGivenByDay(any(), any(), any())).thenReturn(List.of(
                    new AssignmentDayCount(LocalDate.parse("2026-10-14"), "INDIVIDUAL", 2),
                    new AssignmentDayCount(LocalDate.parse("2026-10-14"), "PRACTICE", 1),
                    new AssignmentDayCount(LocalDate.parse("2026-10-20"), "GROUP", 4),
                    new AssignmentDayCount(LocalDate.parse("2026-10-01"), "GROUP", 9)));

            var days = service(f).activity(f.admin).assignmentsByDay().data();

            assertThat(days).hasSize(7);
            assertThat(days.get(0)).isEqualTo(new DayAssignments(LocalDate.parse("2026-10-14"), 2, 0, 1));
            assertThat(days.get(3)).isEqualTo(new DayAssignments(LocalDate.parse("2026-10-17"), 0, 0, 0));
            assertThat(days.get(6)).isEqualTo(new DayAssignments(LocalDate.parse("2026-10-20"), 0, 4, 0));
            verify(assignments).countGivenByDay(Instant.parse("2026-10-13T17:00:00Z"), f.now,
                    AdminDashboardActivityService.ZONE);
        }
    }

    @Test
    void securityEventsShowActorOrAffectedAccountForThirtyDays() {
        try (var f = new DashboardFixture()) {
            UUID target = UUID.randomUUID();
            when(audit.recentSecurityEvents(any(), anyInt())).thenReturn(List.of(
                    new AuditLogView(new AuditEvent(UUID.randomUUID(), null, "ACCOUNT_TEMP_LOCKED", "ACCOUNT",
                            target.toString(), AuditResult.DENIED, "TOO_MANY_FAILURES", Map.of(), f.now), null,
                            "gv03@fpt.edu.vn"),
                    new AuditLogView(new AuditEvent(UUID.randomUUID(), null, "LOGIN_FAILED", null, null,
                            AuditResult.FAILURE, "UNKNOWN_EMAIL", Map.of(), f.now), null, null)));

            var events = service(f).activity(f.admin).securityEvents().data();

            assertThat(events).extracting(e -> e.account()).containsExactly("gv03@fpt.edu.vn", null);
            assertThat(events.get(0).result()).isEqualTo("DENIED");
            verify(audit).recentSecurityEvents(Instant.parse("2026-09-20T17:00:00Z"), AdminDashboardActivityService.SECURITY_LIMIT);
        }
    }

    @Test
    void failingSourceOnlyHidesItsOwnSection() {
        try (var f = new DashboardFixture()) {
            when(assignments.countGivenByDay(any(), any(), any())).thenThrow(new IllegalStateException("down"));
            when(audit.recentSecurityEvents(any(), anyInt())).thenReturn(List.of());

            var view = service(f).activity(f.admin);

            assertThat(view.assignmentsByDay().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(view.securityEvents().availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(view.activeAccountChangeToday().data()).isZero();
        }
    }

    @Test
    void onlyAdminsSeeActivity() {
        try (var f = new DashboardFixture()) {
            assertThatThrownBy(() -> service(f).activity(new Actor(UUID.randomUUID(), Role.TEACHER)))
                    .isInstanceOf(ApiException.class);
        }
    }

    @Test
    void assignmentRowsAccumulateDuplicatesAndIgnoreNegativeUnknownAndOutOfRangeCounts() {
        try (var f = new DashboardFixture()) {
            LocalDate today = LocalDate.parse("2026-10-20");
            when(assignments.countGivenByDay(any(), any(), any())).thenReturn(List.of(
                    new AssignmentDayCount(today, "INDIVIDUAL", 2),
                    new AssignmentDayCount(today, "INDIVIDUAL", 3),
                    new AssignmentDayCount(today, "GROUP", -9),
                    new AssignmentDayCount(today, "UNKNOWN", 500),
                    new AssignmentDayCount(today.plusDays(1), "PRACTICE", 100)));

            var section = service(f).activity(f.admin).assignmentsByDay();

            assertThat(section.availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(section.data().get(6)).isEqualTo(new DayAssignments(today, 5, 0, 0));
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void nullOrUnmappableAssignmentSourceOnlyHidesAssignmentSection(boolean unmappable) {
        try (var f = new DashboardFixture()) {
            when(assignments.countGivenByDay(any(), any(), any())).thenReturn(unmappable
                    ? List.of(new AssignmentDayCount(LocalDate.parse("2026-10-20"), null, 1)) : null);

            var view = service(f).activity(f.admin);

            assertThat(view.assignmentsByDay().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(view.activeAccountChangeToday().availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(view.securityEvents().availability()).isEqualTo(Availability.AVAILABLE);
        }
    }

    @Test
    void activeChangeAuditFailureDoesNotHideIndependentSecurityOrAssignmentSources() {
        try (var f = new DashboardFixture()) {
            when(audit.countSucceeded(eq("ACCOUNT_ACTIVATED"), any(), any(), isNull(), isNull()))
                    .thenThrow(new IllegalStateException("counts unavailable"));

            var view = service(f).activity(f.admin);

            assertThat(view.activeAccountChangeToday().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(view.assignmentsByDay().availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(view.securityEvents().availability()).isEqualTo(Availability.AVAILABLE);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void securityAuditFailureOrMappingFailureDoesNotHideOtherSections(boolean mappingFailure) {
        try (var f = new DashboardFixture()) {
            if (mappingFailure) {
                when(audit.recentSecurityEvents(any(), anyInt())).thenReturn(List.of(new AuditLogView(null, null)));
            } else {
                when(audit.recentSecurityEvents(any(), anyInt())).thenThrow(new IllegalStateException("events unavailable"));
            }

            var view = service(f).activity(f.admin);

            assertThat(view.securityEvents().availability()).isEqualTo(Availability.UNAVAILABLE);
            assertThat(view.activeAccountChangeToday().availability()).isEqualTo(Availability.AVAILABLE);
            assertThat(view.assignmentsByDay().availability()).isEqualTo(Availability.AVAILABLE);
        }
    }
}
