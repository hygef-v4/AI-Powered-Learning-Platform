package edu.aiplatform.identity.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.application.AccountAdminService;
import edu.aiplatform.identity.application.AccountImportService;
import edu.aiplatform.identity.application.AuthService;
import edu.aiplatform.identity.application.RoleChangeBlockedException;
import edu.aiplatform.identity.application.IdentityProperties;
import edu.aiplatform.identity.application.PasswordChangeService;
import edu.aiplatform.identity.application.ProfileService;
import edu.aiplatform.identity.application.SessionAuthority;
import edu.aiplatform.identity.application.SessionInfo;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.config.SecurityConfig;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chuỗi bảo mật thật (SecurityConfig + IdentityHttpSecurity + JwtAuthFilter) với service giả. */
@WebMvcTest(controllers = {AuthController.class, MeController.class, AdminAccountController.class,
        IdentityWebSecurityTest.EchoController.class})
@Import({SecurityConfig.class, IdentityHttpSecurity.class, AuthCookies.class, GlobalExceptionHandler.class,
        IdentityWebSecurityTest.EchoController.class,
        IdentityWebSecurityTest.Config.class})
class IdentityWebSecurityTest {

    private static final Actor STUDENT = new Actor(UUID.randomUUID(), Role.STUDENT);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    ProfileService profileService;

    @MockitoBean
    SessionAuthority sessionAuthority;

    @MockitoBean
    PasswordChangeService passwordChangeService;

    @MockitoBean
    AccountAdminService accountAdminService;

    @MockitoBean
    AccountImportService accountImportService;

    @TestConfiguration
    static class Config {

        @Bean
        IdentityProperties identityProperties() {
            return IdentityTestData.properties();
        }
    }

    /** Endpoint đổi trạng thái giả để kiểm CSRF. */
    @RestController
    static class EchoController {

        @PostMapping("/api/v1/test/echo")
        String echo(@AuthenticationPrincipal Actor actor) {
            return actor.role().name();
        }
    }

    private void signedIn() {
        when(sessionAuthority.authenticate("good"))
                .thenReturn(Optional.of(new SessionAuthority.AuthenticatedSession(STUDENT, "sid-1")));
    }

    @Test
    void protectedEndpointWithoutCookieIs401ProblemDetail() throws Exception {
        mvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void revokedOrInvalidTokenIs401() throws Exception {
        when(sessionAuthority.authenticate(anyString())).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/me/profile").cookie(new Cookie(AuthCookies.ACCESS, "bad")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validCookieInjectsActor() throws Exception {
        signedIn();
        when(profileService.get(STUDENT)).thenReturn(new ProfileService.ProfileView(STUDENT.accountId(),
                "taivm@fpt.edu.vn", "Tài", null, Role.STUDENT, AccountStatus.ACTIVE, "/classes"));

        mvc.perform(get("/api/v1/me/profile").cookie(new Cookie(AuthCookies.ACCESS, "good")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homePath").value("/classes"))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("XSRF-TOKEN="))));
    }

    @Test
    void redisDownIs503NotAnonymous() throws Exception {
        when(sessionAuthority.authenticate("good")).thenThrow(new DataAccessResourceFailureException("redis"));

        mvc.perform(get("/api/v1/me/profile").cookie(new Cookie(AuthCookies.ACCESS, "good")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SYSTEM_BUSY"));
    }

    /** Hồi quy: request đã đăng nhập không được xóa cookie XSRF-TOKEN (lỗi tìm thấy khi chạy T3 với stack thật). */
    @Test
    void authenticatedRequestKeepsCsrfCookie() throws Exception {
        signedIn();

        mvc.perform(get("/api/v1/me/profile")
                        .cookie(new Cookie(AuthCookies.ACCESS, "good"), new Cookie("XSRF-TOKEN", "abc")))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void stateChangingRequestNeedsCsrfHeader() throws Exception {
        signedIn();

        mvc.perform(post("/api/v1/test/echo").cookie(new Cookie(AuthCookies.ACCESS, "good")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mvc.perform(post("/api/v1/test/echo")
                        .cookie(new Cookie(AuthCookies.ACCESS, "good"), new Cookie("XSRF-TOKEN", "abc"))
                        .header("X-XSRF-TOKEN", "abc"))
                .andExpect(status().isOk());
    }

    @Test
    void loginIsPublicWithoutCsrfAndSetsHttpOnlyCookies() throws Exception {
        when(authService.login(eq("taivm@fpt.edu.vn"), eq("hocbai2026"), any())).thenReturn(
                new AuthService.LoginResult(new SessionInfo(STUDENT.accountId(), "Tài", Role.STUDENT, "/classes"),
                        "access", "refresh"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"taivm@fpt.edu.vn\",\"password\":\"hocbai2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homePath").value("/classes"))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("access_token=access"))))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("HttpOnly"))))
                .andExpect(header().stringValues("Set-Cookie",
                        hasItem(containsString("Path=/api/v1/auth/refresh"))));
    }

    @Test
    void profileUpdateRejectsFieldsOtherThanNameAndPhone() throws Exception {
        signedIn();

        mvc.perform(patch("/api/v1/me/profile")
                        .cookie(new Cookie(AuthCookies.ACCESS, "good"), new Cookie("XSRF-TOKEN", "abc"))
                        .header("X-XSRF-TOKEN", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Tài\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verify(profileService, never()).update(any(), any(), any());
    }

    @Test
    void malformedJsonGetsIdentityErrorCode() throws Exception {
        signedIn();

        mvc.perform(patch("/api/v1/me/profile")
                        .cookie(new Cookie(AuthCookies.ACCESS, "good"), new Cookie("XSRF-TOKEN", "abc"))
                        .header("X-XSRF-TOKEN", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void changePasswordPassesCurrentSessionId() throws Exception {
        signedIn();

        mvc.perform(post("/api/v1/me/password")
                        .cookie(new Cookie(AuthCookies.ACCESS, "good"), new Cookie("XSRF-TOKEN", "abc"))
                        .header("X-XSRF-TOKEN", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"hocbai2026\",\"newPassword\":\"matkhaumoi9\"}"))
                .andExpect(status().isNoContent());
        verify(passwordChangeService).change(STUDENT, "sid-1", "hocbai2026", "matkhaumoi9");
    }

    @Test
    void importEndpointWithColonAcceptsCsvUpload() throws Exception {
        signedIn();
        when(accountImportService.validate(eq(STUDENT), any())).thenReturn(
                new AccountImportService.ImportResult("abc", java.util.List.of(), 0, 0, false));

        mvc.perform(multipart("/api/v1/admin/accounts/imports:validate")
                        .file(new MockMultipartFile("file", "tk.csv", "text/csv", "email,display_name,role\n".getBytes()))
                        .cookie(new Cookie(AuthCookies.ACCESS, "good"), new Cookie("XSRF-TOKEN", "abc"))
                        .header("X-XSRF-TOKEN", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileChecksum").value("abc"));

        mvc.perform(multipart("/api/v1/admin/accounts/imports:validate")
                        .file(new MockMultipartFile("file", "tk.xlsx", "application/octet-stream", new byte[] {1}))
                        .cookie(new Cookie(AuthCookies.ACCESS, "good"), new Cookie("XSRF-TOKEN", "abc"))
                        .header("X-XSRF-TOKEN", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_INVALID"));
    }

    @Test
    void roleChangeBlockedListsAssignments() throws Exception {
        signedIn();
        UUID subject = UUID.randomUUID();
        when(accountAdminService.changeRole(eq(STUDENT), any(), eq(Role.TEACHER), eq(2L)))
                .thenThrow(new RoleChangeBlockedException(java.util.List.of(subject), java.util.List.of()));

        mvc.perform(put("/api/v1/admin/accounts/" + UUID.randomUUID() + "/role")
                        .cookie(new Cookie(AuthCookies.ACCESS, "good"), new Cookie("XSRF-TOKEN", "abc"))
                        .header("X-XSRF-TOKEN", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEACHER\",\"version\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROLE_CHANGE_BLOCKED"))
                .andExpect(jsonPath("$.managedSubjects[0]").value(subject.toString()));
    }

    @Test
    void logoutRevokesCurrentSessionAndClearsCookies() throws Exception {
        signedIn();

        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie(AuthCookies.ACCESS, "good")))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("access_token=;"))));
        verify(authService).logout("sid-1");
    }
}
