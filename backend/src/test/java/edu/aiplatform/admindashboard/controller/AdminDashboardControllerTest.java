package edu.aiplatform.admindashboard.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import edu.aiplatform.admindashboard.service.AdminDashboardService;
import edu.aiplatform.admindashboard.dto.AdminDashboardViews.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminDashboardControllerTest {
    AdminDashboardService service = mock(AdminDashboardService.class);
    Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    MockMvc mvc;
    @BeforeEach void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null));
        mvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(service, null))
                .setControllerAdvice(new AdminDashboardExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void returnsPublicProjectionAndNoStoreIncludingNullableEmptyBackup() throws Exception {
        var backup = new BackupSection(Section.available(null), Section.unavailable(), Section.available(List.of()),
                Section.unavailable(), Section.unavailable());
        when(service.dashboard(admin)).thenReturn(new AdminDashboardView(Instant.parse("2026-10-20T00:00:00Z"),
                Section.available(new AccountCounts(Map.of(), 5, 3)), Section.unavailable(), Section.unavailable(),
                Section.available(7L), backup));
        mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accounts.data.total").value(5))
                .andExpect(jsonPath("$.backup.latest.availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.backup.latest.data").isEmpty())
                .andExpect(jsonPath("$.backup.latest.data.id").doesNotExist())
                .andExpect(jsonPath("$.subjects.availability").value("UNAVAILABLE"));
        verify(service).dashboard(admin);
    }
    @Test void historyDefaultsAndExplicitPageUseServerPrincipal() throws Exception {
        when(service.backups(admin, 0, 20)).thenReturn(new BackupHistoryPage(List.of(), 0, 20, 0, 0));
        when(service.backups(admin, 1, 10)).thenReturn(new BackupHistoryPage(List.of(), 1, 10, 11, 2));
        mvc.perform(get("/api/v1/admin/dashboard/backups")).andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0));
        mvc.perform(get("/api/v1/admin/dashboard/backups?page=1&size=10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(11)).andExpect(jsonPath("$.totalPages").value(2));
        verify(service).backups(admin, 0, 20);
        verify(service).backups(admin, 1, 10);
    }
    @ParameterizedTest @ValueSource(strings = {"page=2147483648", "page=x", "page=1&page=2", "actor=forged", "sort=id"})
    void invalidQueriesAreSafeAndNeverCallService(String query) throws Exception {
        mvc.perform(get("/api/v1/admin/dashboard/backups?" + query)).andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("DASHBOARD_PAGE_INVALID"));
        verify(service).requireAdmin(admin);
        verify(service, never()).backups(any(), anyInt(), anyInt());
        verifyNoMoreInteractions(service);
    }
    @Test void deniedAndUnexpectedErrorsNeverExposeProviderExceptions() throws Exception {
        when(service.dashboard(admin)).thenThrow(ApiException.forbidden("DASHBOARD_FORBIDDEN"));
        mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("DASHBOARD_FORBIDDEN"));
        doThrow(new IllegalStateException("secret")).when(service).dashboard(admin);
        mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Không đọc được tổng quan hệ thống"));
    }
}
