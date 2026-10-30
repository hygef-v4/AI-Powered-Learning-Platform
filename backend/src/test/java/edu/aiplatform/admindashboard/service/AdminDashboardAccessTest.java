package edu.aiplatform.admindashboard.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import org.junit.jupiter.api.Test;

class AdminDashboardAccessTest {
    @Test void nonAdminAndAnonymousCallsReadNoSourcesAndAuditDenial() {
        try (var fixture = new DashboardFixture()) {
            for (Role role : Role.values()) {
                if (role == Role.ADMIN) continue;
                assertProblem(() -> fixture.service.dashboard(new Actor(fixture.admin.accountId(), role)), 403);
                assertProblem(() -> fixture.service.backups(new Actor(fixture.admin.accountId(), role), 0, 20), 403);
            }
            assertProblem(() -> fixture.service.dashboard(null), 401);
            verifyNoInteractions(fixture.accounts, fixture.subjects, fixture.classes, fixture.backups, fixture.settings);
            verify(fixture.audit, atLeastOnce()).record(argThat(event -> event.action().equals("ACCESS_DENIED")
                    && event.result() == AuditResult.DENIED && event.objectId() == null && event.details().isEmpty()));
        }
    }
    @Test void deniedNullAndUnavailableAuthorizationNeverFanOut() {
        try (var fixture = new DashboardFixture()) {
            when(fixture.authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("ROLE_NOT_ALLOWED"));
            assertProblem(() -> fixture.service.dashboard(fixture.admin), 403);
            when(fixture.authorization.authorize(any(), anyString(), any())).thenReturn(null);
            assertProblem(() -> fixture.service.dashboard(fixture.admin), 503);
            when(fixture.authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("DEPENDENCY_UNAVAILABLE"));
            assertProblem(() -> fixture.service.backups(fixture.admin, 0, 20), 503);
            when(fixture.authorization.authorize(any(), anyString(), any())).thenThrow(new IllegalStateException("secret"));
            assertProblem(() -> fixture.service.dashboard(fixture.admin), 503);
            verifyNoInteractions(fixture.accounts, fixture.subjects, fixture.classes, fixture.backups, fixture.settings);
        }
    }
    @Test void failedDeniedAuditStillCannotExposeData() {
        try (var fixture = new DashboardFixture()) {
            doThrow(new IllegalStateException("secret")).when(fixture.audit).record(any());
            assertThatThrownBy(() -> fixture.service.dashboard(null)).isInstanceOfSatisfying(ApiException.class,
                    problem -> assertThat(problem.code()).isEqualTo("DASHBOARD_AUDIT_UNAVAILABLE"));
            verifyNoInteractions(fixture.accounts, fixture.subjects, fixture.classes, fixture.backups, fixture.settings);
        }
    }
    private void assertProblem(Runnable action, int status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class, problem -> {
            assertThat(problem.status().value()).isEqualTo(status);
            assertThat(problem.getMessage()).doesNotContain("secret");
        });
    }
}
