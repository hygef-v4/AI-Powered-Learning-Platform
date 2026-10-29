package edu.aiplatform.subjects.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.domain.Subject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubjectAuditTest {
    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAudit audit = mock(IAudit.class);
    private final SubjectAuditRecorder recorder = new SubjectAuditRecorder(audit);
    private final SubjectAdminAccess access = new SubjectAdminAccess(authorization, recorder, Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    private final UUID id = UUID.randomUUID();

    @Test
    void anonymousDenialRecordsOneSafeEventWithoutAccountOrObjectId() {
        code(() -> access.require(null, "SUBJECT_LIST", ResourceRef.none()), "AUTHENTICATION_REQUIRED");
        AuditEvent event = event();
        assertThat(event.actorId()).isNull();
        assertThat(event.objectId()).isNull();
        assertThat(event.action()).isEqualTo("ACCESS_DENIED");
        assertThat(event.result()).isEqualTo(AuditResult.DENIED);
        assertThat(event.reason()).isEqualTo("AUTHENTICATION_REQUIRED");
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.details()).containsOnlyKeys("attemptedAction", "scope");
        assertThat(event.details()).containsEntry("attemptedAction", "SUBJECT_LIST").containsEntry("scope", "NONE");
        verifyNoInteractions(authorization);
    }

    @Test
    void nonAdminDenialHasActorAndSubjectButDoesNotCallAuthorization() {
        var student = new Actor(UUID.randomUUID(), Role.STUDENT);
        code(() -> access.require(student, "SUBJECT_UPDATE", ResourceRef.subject(id)), "SUBJECT_FORBIDDEN");
        assertThat(event().actorId()).isEqualTo(student.accountId());
        assertThat(event().objectId()).isEqualTo(id.toString());
        verifyNoInteractions(authorization);
    }

    @Test
    void deniedDecisionDoesNotCopyProviderReasonOrSecrets() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("secret-token-and-internal-reason"));
        code(() -> access.require(admin, "SUBJECT_VIEW", ResourceRef.subject(id)), "SUBJECT_FORBIDDEN");
        assertThat(event().reason()).isEqualTo("SUBJECT_FORBIDDEN");
        assertThat(event().details().toString()).doesNotContain("secret-token", "email", "description");
    }

    @Test
    void authorizationFailureIsAuditedAsRejectedWithSafeReason() {
        when(authorization.authorize(any(), anyString(), any())).thenThrow(new IllegalStateException("password=hidden"));
        code(() -> access.require(admin, "SUBJECT_CREATE", ResourceRef.none()), "SUBJECT_AUTHORIZATION_UNAVAILABLE");
        assertThat(event().reason()).isEqualTo("SUBJECT_AUTHORIZATION_UNAVAILABLE");
        assertThat(event().details().toString()).doesNotContain("password");
    }

    @Test
    void nullDecisionIsAlsoAuditedAndDenied() {
        code(() -> access.require(admin, "SUBJECT_MANAGER_CHANGE", ResourceRef.subject(id)), "SUBJECT_AUTHORIZATION_UNAVAILABLE");
        assertThat(event().result()).isEqualTo(AuditResult.DENIED);
    }

    @Test
    void auditFailureKeepsAccessClosedAndReturnsSafeUnavailable() {
        doThrow(new IllegalStateException("SQL internal")).when(audit).record(any());
        code(() -> access.require(null, "SUBJECT_LIST", ResourceRef.none()), "SUBJECT_AUDIT_UNAVAILABLE");
        verify(audit, times(1)).record(any());
        verifyNoInteractions(authorization);
    }

    @Test
    void allowedRequestDoesNotRecordDeniedOrReadAudit() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        access.require(admin, "SUBJECT_VIEW", ResourceRef.subject(id));
        verifyNoInteractions(audit);
    }

    @Test
    void distinctRejectedRequestsHaveDistinctEventIds() {
        code(() -> access.require(null, "SUBJECT_LIST", ResourceRef.none()), "AUTHENTICATION_REQUIRED");
        code(() -> access.require(null, "SUBJECT_LIST", ResourceRef.none()), "AUTHENTICATION_REQUIRED");
        var events = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit, times(2)).record(events.capture());
        assertThat(events.getAllValues()).extracting(AuditEvent::id).doesNotHaveDuplicates();
    }

    @Test
    void successPayloadsAreAllowlistedAndKeepExplicitNullForUnassignment() {
        Subject before = Subject.create(id, "ABC", "Name", "secret-description", UUID.randomUUID(), NOW.minusSeconds(10));
        Subject edited = before.updateInformation("New", "new-secret-description", NOW);
        recorder.informationUpdated(admin, before, edited);
        var update = event();
        assertThat(update.details()).containsOnlyKeys("changedFields");
        assertThat(update.details()).containsEntry("changedFields", List.of("name", "description"));
        assertThat(update.details().toString()).doesNotContain("secret-description");
        clearInvocations(audit);
        Subject unassigned = before.changeManager(null, NOW);
        recorder.managerChanged(admin, before, unassigned);
        assertThat(event().details()).containsOnlyKeys("oldManagerId", "newManagerId").containsEntry("newManagerId", null);
    }

    private AuditEvent event() {
        var result = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(result.capture());
        return result.getValue();
    }

    private void code(Runnable call, String expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, error -> {
            assertThat(error.code()).isEqualTo(expected);
            assertThat(error.getMessage()).doesNotContain("SQL", "password", "secret-token");
        });
    }
}
