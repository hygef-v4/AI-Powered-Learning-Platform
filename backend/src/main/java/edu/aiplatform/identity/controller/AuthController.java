package edu.aiplatform.identity.controller;

import edu.aiplatform.identity.service.AuthService;
import edu.aiplatform.identity.service.SessionInfo;
import edu.aiplatform.shared.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC 02 Login, UC 03 Logout và refresh phiên. Endpoint public, không cần CSRF token ({@code /api/v1/auth/**}). */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthService auth;
    private final AuthCookies cookies;

    public AuthController(AuthService auth, AuthCookies cookies) {
        this.auth = auth;
        this.cookies = cookies;
    }

    /** Giới hạn độ dài để request quá lớn không tới bcrypt; sai định dạng vẫn trả lỗi trung tính ở service. */
    public record LoginRequest(@NotNull @Size(max = 320) String email, @NotNull @Size(max = 1024) String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<SessionInfo> login(@RequestBody @Validated LoginRequest request,
                                             HttpServletRequest http) {
        AuthService.LoginResult result = auth.login(request.email(), request.password(), http.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.access(result.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.refresh(result.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearLegacyRefresh().toString())
                .body(result.info());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest http) {
        try {
            String refreshToken = AuthCookies.read(http, AuthCookies.REFRESH).orElse(null);
            AuthService.RefreshResult result = auth.refresh(refreshToken);
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, cookies.access(result.accessToken()).toString())
                    .header(HttpHeaders.SET_COOKIE, cookies.refresh(result.refreshToken()).toString())
                    .header(HttpHeaders.SET_COOKIE, cookies.clearLegacyRefresh().toString())
                    .build();
        } catch (ApiException e) {
            if (AuthService.REFRESH_CONCURRENT.equals(e.code())) {
                // Không xóa cookie: request thắng vừa đặt cookie mới, xóa ở đây sẽ ghi đè và đăng xuất người dùng.
                return ResponseEntity.status(e.status()).build();
            }
            return ResponseEntity.status(e.status())
                    .header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
                    .header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
                    .header(HttpHeaders.SET_COOKIE, cookies.clearLegacyRefresh().toString())
                    .build();
        }
    }

    /** Luôn xóa cookie; thu hồi phiên theo access token, hoặc theo refresh token khi access đã hết hạn (F4). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        auth.logout((String) http.getAttribute(JwtAuthFilter.SID_ATTRIBUTE),
                AuthCookies.read(http, AuthCookies.REFRESH).orElse(null));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearLegacyRefresh().toString())
                .build();
    }
}
