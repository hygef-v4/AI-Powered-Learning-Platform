package edu.aiplatform.admindashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.admindashboard.service.DashboardFixture;
import edu.aiplatform.admindashboard.service.AdminDashboardActivityService;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.audit.contract.IAuditStats;
import edu.aiplatform.backup.contract.*;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.identity.contract.*;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminDashboardApiContractTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest @ValueSource(strings = {"unknown=1", "page=x", "page=1&page=2", "page=2147483648"})
    void malformedQueriesRejectNonAdminWithForbiddenAndDeniedAudit(String query) throws Exception {
        try (var f = new DashboardFixture()) {
            var mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(f.adminDashboardService, null))
                    .setControllerAdvice(new AdminDashboardExceptionHandler())
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
            Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(teacher, null));
            mvc.perform(get("/api/v1/admin/dashboard/backups?" + query)).andExpect(status().isForbidden());
            verify(f.audit).record(argThat(event -> teacher.accountId().equals(event.actorId())
                    && event.action().equals("ACCESS_DENIED") && event.result() == AuditResult.DENIED));
        }
    }

    @Test void unknownQueryOnDashboardRejectsNonAdminWithForbidden() throws Exception {
        try (var f = new DashboardFixture()) {
            var mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(f.adminDashboardService, null))
                    .setControllerAdvice(new AdminDashboardExceptionHandler())
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
            Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(teacher, null));
            mvc.perform(get("/api/v1/admin/dashboard?unknown=1")).andExpect(status().isForbidden());
            verify(f.audit).record(argThat(event -> teacher.accountId().equals(event.actorId())
                    && event.action().equals("ACCESS_DENIED") && event.result() == AuditResult.DENIED));
        }
    }

    private void noPrivateFields(JsonNode node) {
        if (node.isObject()) {
            for (String field : List.of("id", "name", "email", "archiveRef", "archiveSha256", "key", "stackTrace")) {
                assertThat(node.has(field)).as("Forbidden field %s", field).isFalse();
            }
        }
        node.forEach(this::noPrivateFields);
    }
}
