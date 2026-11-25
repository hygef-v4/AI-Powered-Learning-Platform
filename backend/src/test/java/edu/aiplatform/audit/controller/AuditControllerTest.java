package edu.aiplatform.audit.controller;

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
        when(auditQuery.query(any(), any(), any())).thenThrow(ApiException.createForbidden("AUDIT_FORBIDDEN"));

        mvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUDIT_FORBIDDEN"));
    }
}
