package edu.aiplatform.admindashboard.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.admindashboard.application.DashboardFixture;
import edu.aiplatform.backup.contract.*;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.identity.contract.*;
import java.time.LocalDate;
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

    @Test void neverSucceededSerializesAvailableNullInsteadOfUnavailable() throws Exception {
        try (var f = new DashboardFixture()) {
            when(f.backups.summarize(any(), any())).thenReturn(new BackupDashboardSummary(
                    f.now.minus(java.time.Duration.ofDays(7)), f.now, null, 0, null));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(f.admin, null));
            var mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(f.service, null))
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
            String json = mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode backup = Jackson2ObjectMapperBuilder.json().build().readTree(json).path("backup");
            assertThat(backup.path("latestSuccessfulAt").path("availability").asText()).isEqualTo("AVAILABLE");
            assertThat(backup.path("latestSuccessfulAt").has("data")).isTrue();
            assertThat(backup.path("latestSuccessfulAt").path("data").isNull()).isTrue();
            assertThat(backup.path("failuresLast7Days").path("data").path("count").asInt(-1)).isZero();
        }
    }

    @Test void actualServiceProjectsBackupWithoutLeakingProviderIdOrAccountContact() throws Exception {
        try (var f = new DashboardFixture()) {
            UUID privateId = UUID.randomUUID();
            var run = new BackupDashboardRunView(LocalDate.of(2026, 10, 20), BackupState.SUCCESS, f.now, f.now.minusSeconds(1), f.now, 12L, null, null);
            when(f.backups.latestRun()).thenReturn(Optional.of(run));
            doAnswer(call -> new PageImpl<>(List.of(run), call.getArgument(0), 1)).when(f.backups).history(any());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(f.admin, null));
            var mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(f.service, null))
                    .setControllerAdvice(new AdminDashboardExceptionHandler())
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
            ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
            for (String endpoint : List.of("/api/v1/admin/dashboard", "/api/v1/admin/dashboard/backups")) {
                String json = mvc.perform(get(endpoint)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                assertThat(json).doesNotContain(privateId.toString(), f.admin.accountId().toString());
                noPrivateFields(mapper.readTree(json));
            }
            verify(f.accounts, never()).getContact(any());
        }
    }

    @Test void actualRoleGuardRejectsEveryNonAdminWithoutReadingSources() throws Exception {
        try (var f = new DashboardFixture()) {
            var mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(f.service, null))
                    .setControllerAdvice(new AdminDashboardExceptionHandler())
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
            for (Role role : Role.values()) {
                if (role == Role.ADMIN) continue;
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new Actor(UUID.randomUUID(), role), null));
                mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isForbidden());
                mvc.perform(get("/api/v1/admin/dashboard/backups")).andExpect(status().isForbidden());
            }
            SecurityContextHolder.clearContext();
            mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isUnauthorized());
            verifyNoInteractions(f.accounts, f.subjects, f.classes, f.backups, f.settings);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"page=-1", "size=0", "size=101"})
    void paginationRangesAreValidatedAfterAccessAndDeniedAudit(String query) throws Exception {
        try (var f = new DashboardFixture()) {
            var mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(f.service, null))
                    .setControllerAdvice(new AdminDashboardExceptionHandler())
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
            for (Role role : Role.values()) {
                if (role == Role.ADMIN) continue;
                Actor actor = new Actor(UUID.randomUUID(), role);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
                mvc.perform(get("/api/v1/admin/dashboard/backups?" + query)).andExpect(status().isForbidden());
                verify(f.audit).record(argThat(event -> actor.accountId().equals(event.actorId())
                        && event.action().equals("ACCESS_DENIED") && event.result() == AuditResult.DENIED));
            }
            SecurityContextHolder.clearContext();
            mvc.perform(get("/api/v1/admin/dashboard/backups?" + query)).andExpect(status().isUnauthorized());
            verify(f.audit).record(argThat(event -> event.actorId() == null
                    && event.action().equals("ACCESS_DENIED") && event.result() == AuditResult.DENIED));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(f.admin, null));
            mvc.perform(get("/api/v1/admin/dashboard/backups?" + query)).andExpect(status().isBadRequest());
            verifyNoInteractions(f.accounts, f.subjects, f.classes, f.backups, f.settings);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"unknown=1", "page=x", "page=1&page=2", "page=2147483648"})
    void malformedQueriesRejectNonAdminWithForbiddenAndDeniedAudit(String query) throws Exception {
        try (var f = new DashboardFixture()) {
            var mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(f.service, null))
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
            var mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(f.service, null))
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
