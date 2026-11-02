package edu.aiplatform.admindashboard.service;

import static org.mockito.Mockito.*;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.admindashboard.integration.DashboardSourceReader;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.backup.contract.IBackupDashboardStatus;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Page;

public class DashboardFixture implements AutoCloseable {
    public final IAccountLookup accounts = mock(IAccountLookup.class);
    public final ISubjectQuery subjects = mock(ISubjectQuery.class);
    public final IClassAccess classes = mock(IClassAccess.class);
    public final IBackupDashboardStatus backups = mock(IBackupDashboardStatus.class);
    public final ISettings settings = mock(ISettings.class);
    public final IAuthorization authorization = mock(IAuthorization.class);
    public final IAudit audit = mock(IAudit.class);
    public final DashboardSourceReader reader = new DashboardSourceReader();
    public final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    public final Instant now = Instant.parse("2026-10-20T03:00:00Z");
    public final AdminDashboardService service = new AdminDashboardService(accounts, subjects, classes, backups,
            settings, authorization, audit, reader, Clock.fixed(now, ZoneOffset.UTC));

    public DashboardFixture() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(accounts.countByRoleAndStatus()).thenReturn(Map.of());
        when(subjects.countSubjectsByStatus()).thenReturn(Map.of());
        when(classes.countClassesByStatus()).thenReturn(Map.of());
        when(backups.latestRun()).thenReturn(Optional.empty());
        when(backups.history(any())).thenAnswer(call -> Page.empty(call.getArgument(0)));
    }
    @Override public void close() { reader.close(); }
}
