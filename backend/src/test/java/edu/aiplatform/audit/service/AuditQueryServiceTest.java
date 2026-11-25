package edu.aiplatform.audit.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditFilter;
import edu.aiplatform.audit.contract.AuditLogView;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.audit.repository.AuditLogRepository;
import edu.aiplatform.audit.model.AuditRow;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

class AuditQueryServiceTest {

    static final Instant NOW = Instant.parse("2026-10-18T03:00:00Z");
    static final AuditFilter NO_FILTER = new AuditFilter(null, null, null, null, null, null, null);

    final AuditLogRepository repository = mock(AuditLogRepository.class);
    final IAuthorization authorization = mock(IAuthorization.class);
    final IAccountLookup accounts = mock(IAccountLookup.class);
    final IAudit audit = mock(IAudit.class);
    final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    AuditQueryService service;

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        service = new AuditQueryService(repository, authorization, accounts, audit,
                JsonMapper.builder().findAndAddModules().build(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    static AuditRow row(UUID actorId, String details) {
        return new AuditRow(UUID.randomUUID(), actorId, "ROLE_CHANGED", "ACCOUNT", "abc", "SUCCESS", null, details,
                NOW.minusSeconds(60));
    }

    static AccountContact contact(UUID id, String email) {
        return new AccountContact(id, email, "Tên", Role.ADMIN, AccountStatus.ACTIVE);
    }

    void assertForbiddenAndAudited(Actor actor) {
        assertThatThrownBy(() -> service.query(actor, NO_FILTER, PageRequest.of(0, 50)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.readStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.readCode()).isEqualTo("AUDIT_FORBIDDEN");
                });
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("ACCESS_DENIED");
        assertThat(event.getValue().result()).isEqualTo(AuditResult.DENIED);
        assertThat(event.getValue().reason()).isEqualTo("AUDIT_READ");
        verify(repository, never()).count(any());
    }

    @Test
    void nonAdminIsDeniedAndTheDenialIsAudited() {
        assertForbiddenAndAudited(new Actor(UUID.randomUUID(), Role.TEACHER));
    }

    @Test
    void authorizationErrorFailsClosed() {
        when(authorization.authorize(any(), any(), any())).thenThrow(new IllegalStateException("U01 down"));
        assertForbiddenAndAudited(admin);
    }

    @Test
    void adminGetsPageWithEmailsAndSystemRowsHaveNoEmail() {
        UUID a = UUID.randomUUID();
        when(repository.count(any())).thenReturn(3L);
        when(repository.findRows(any(), anyLong(), anyInt()))
                .thenReturn(List.of(row(a, "{\"before\":\"STUDENT\"}"), row(a, "{}"), row(null, "{}")));
        when(accounts.getContact(a)).thenReturn(Optional.of(contact(a, "phongdt@fpt.edu.vn")));

        Page<AuditLogView> page = service.query(admin, NO_FILTER, PageRequest.of(0, 50));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(AuditLogView::actorEmail)
                .containsExactly("phongdt@fpt.edu.vn", "phongdt@fpt.edu.vn", null);
        assertThat(page.getContent().get(0).event().details()).containsEntry("before", "STUDENT");
        verify(accounts, times(1)).getContact(a);
        verify(accounts, times(1)).getContact(any());
    }

    @Test
    void partialActorTextIsResolvedToMatchingAccounts() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(accounts.findAccounts("phong", EnumSet.allOf(Role.class), AuditQueryService.ACTOR_MATCH_LIMIT))
                .thenReturn(List.of(contact(first, "phongdt@fpt.edu.vn"), contact(second, "phongnv@fpt.edu.vn")));
        when(repository.count(any())).thenReturn(0L);

        service.query(admin, new AuditFilter(null, null, null, null, null, null, null, " phong ", null),
                PageRequest.of(0, 50));

        ArgumentCaptor<AuditFilter> used = ArgumentCaptor.forClass(AuditFilter.class);
        verify(repository).count(used.capture());
        assertThat(used.getValue().actorIds()).containsExactly(first, second);
        assertThat(used.getValue().actor()).isEqualTo("phong");
    }

    void assertInvalid(AuditFilter filter, Pageable page) {
        assertThatThrownBy(() -> service.query(admin, filter, page))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.readStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.readCode()).isEqualTo("AUDIT_FILTER_INVALID");
                });
    }

    @Test
    void malformedFiltersAreRejected() {
        assertInvalid(new AuditFilter(null, null, null, null, null, NOW, NOW.minusSeconds(1)), PageRequest.of(0, 50));
        assertInvalid(new AuditFilter(null, "A".repeat(101), null, null, null, null, null), PageRequest.of(0, 50));
        assertInvalid(new AuditFilter(null, null, "T".repeat(51), null, null, null, null), PageRequest.of(0, 50));
        assertInvalid(new AuditFilter(null, null, null, "I".repeat(201), null, null, null), PageRequest.of(0, 50));
        verify(repository, never()).count(any());
    }

    @Test
    void pageSizeIsLimitedTo100() {
        assertInvalid(NO_FILTER, PageRequest.of(0, 101));
        assertInvalid(NO_FILTER, Pageable.unpaged());
        assertInvalid(NO_FILTER, null);

        when(repository.count(any())).thenReturn(0L);
        service.query(admin, NO_FILTER, PageRequest.of(0, 100));
    }
}
