package edu.aiplatform.grading.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.UUID;
import java.time.Clock;

class GradingAccessTest {
    @Test
    void revokedClassAuthorityFailsClosedAndIsAudited() {
        var auth = mock(IAuthorization.class);
        var audit = mock(IAudit.class);
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        when(auth.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.deny("OUT_OF_CLASS_SCOPE"));
        var access = new GradingAccess(auth, mock(IClassAccess.class), audit, Clock.systemUTC());
        assertThatThrownBy(() -> access.requireTeacher(actor, UUID.randomUUID(), Actions.GRADING_WRITE))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status().value()).isEqualTo(404));
        verify(audit).record(argThat(event -> event.result() == AuditResult.DENIED));
    }

    @Test
    void missingAccessProviderIsUnavailableRatherThanEmptyData() {
        var auth = mock(IAuthorization.class);
        when(auth.authorize(any(), any(), any())).thenThrow(new IllegalStateException("internal detail"));
        var access = new GradingAccess(auth, mock(IClassAccess.class), mock(IAudit.class), Clock.systemUTC());
        assertThatThrownBy(() -> access.requireTeacher(new Actor(UUID.randomUUID(), Role.TEACHER), UUID.randomUUID(),
                Actions.GRADING_REVIEW))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status().value()).isEqualTo(503))
                .hasMessageNotContaining("internal detail");
    }

    @Test
    void unavailableEnrollmentDoesNotMasqueradeAsRemovedStudent() {
        var auth = mock(IAuthorization.class);
        when(auth.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        var classes = mock(IClassAccess.class);
        when(classes.isActiveStudent(any(), any())).thenThrow(new IllegalStateException());
        var access = new GradingAccess(auth, classes, mock(IAudit.class), Clock.systemUTC());
        assertThatThrownBy(() -> access.requireStudent(new Actor(UUID.randomUUID(), Role.STUDENT), UUID.randomUUID()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status().value()).isEqualTo(503));
    }
}
