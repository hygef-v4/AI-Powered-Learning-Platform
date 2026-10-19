package edu.aiplatform.audit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditFilter;
import edu.aiplatform.audit.contract.AuditLogView;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAuditQuery;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuditControllerTest {

    final IAuditQuery auditQuery = mock(IAuditQuery.class);
    final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
        mvc = MockMvcBuilders.standaloneSetup(new AuditController(auditQuery))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsParametersAndReturnsPage() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        AuditEvent event = new AuditEvent(id, actorId, "ROLE_CHANGED", "ACCOUNT", "abc", AuditResult.SUCCESS, null,
                Map.of("actorType", "USER"), Instant.parse("2026-10-14T10:55:12Z"));
        when(auditQuery.query(any(), any(), any())).thenReturn(
                new PageImpl<>(List.of(new AuditLogView(event, "phongdt@fpt.edu.vn")), PageRequest.of(1, 20), 21));

        mvc.perform(get("/api/v1/admin/audit-logs")
                        .param("actorId", actorId.toString()).param("action", "ROLE_CHANGED")
                        .param("objectType", "ACCOUNT").param("objectId", "abc").param("result", "SUCCESS")
                        .param("from", "2026-10-07T00:00:00Z").param("to", "2026-10-14T23:59:59Z")
                        .param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].actorEmail").value("phongdt@fpt.edu.vn"))
                .andExpect(jsonPath("$.items[0].result").value("SUCCESS"))
                .andExpect(jsonPath("$.items[0].details.actorType").value("USER"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(2));

        ArgumentCaptor<AuditFilter> filter = ArgumentCaptor.forClass(AuditFilter.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(auditQuery).query(eq(admin), filter.capture(), page.capture());
        assertThat(filter.getValue()).isEqualTo(new AuditFilter(actorId, "ROLE_CHANGED", "ACCOUNT", "abc",
                AuditResult.SUCCESS, Instant.parse("2026-10-07T00:00:00Z"), Instant.parse("2026-10-14T23:59:59Z")));
        assertThat(page.getValue().getPageNumber()).isEqualTo(1);
        assertThat(page.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void defaultsToFirstPageOf50() throws Exception {
        when(auditQuery.query(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mvc.perform(get("/api/v1/admin/audit-logs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(auditQuery).query(any(), any(), page.capture());
        assertThat(page.getValue().getPageNumber()).isZero();
        assertThat(page.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void malformedParametersReturnSafe400() throws Exception {
        for (String[] param : new String[][] {{"actorId", "not-a-uuid"}, {"result", "MAYBE"},
                {"from", "hôm qua"}, {"page", "x"}, {"page", "-1"}, {"size", "0"}}) {
            mvc.perform(get("/api/v1/admin/audit-logs").param(param[0], param[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("AUDIT_FILTER_INVALID"));
        }
        verify(auditQuery, never()).query(any(), any(), any());
    }

    @Test
    void forbiddenFromQueryBecomes403ProblemDetail() throws Exception {
        when(auditQuery.query(any(), any(), any())).thenThrow(ApiException.forbidden("AUDIT_FORBIDDEN"));

        mvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUDIT_FORBIDDEN"));
    }
}
