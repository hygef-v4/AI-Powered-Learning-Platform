package edu.aiplatform.identity.controller;

import edu.aiplatform.identity.config.IdentityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie phiên (P2, NFR-U01-12): cả hai {@code HttpOnly}, không để trong local storage hay URL.
 * {@code access_token}: {@code SameSite=Lax}, {@code Path=/api}. {@code refresh_token}: {@code SameSite=Strict},
 * chỉ gửi tới {@code /api/v1/auth} (refresh và logout cần nó; logout dùng để thu hồi phiên khi access token đã hết hạn).
 * {@code Secure} tắt được chỉ ở local chạy HTTP.
 */
@Component
public class AuthCookies {

    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";
    public static final String ACCESS_PATH = "/api";
    public static final String REFRESH_PATH = "/api/v1/auth";
    /**
     * Path cũ của {@code refresh_token}. Cookie còn sót ở path này được trình duyệt gửi trước cookie mới (path dài hơn)
     * nên luôn xóa kèm mỗi lần đặt hoặc xóa refresh token.
     */
    static final String LEGACY_REFRESH_PATH = "/api/v1/auth/refresh";

    private final boolean secure;
    private final Duration accessTtl;
    private final Duration refreshMaxTtl;

    public AuthCookies(IdentityProperties properties) {
        this.secure = properties.cookieSecure();
        this.accessTtl = properties.accessTtl();
        this.refreshMaxTtl = properties.refreshMaxTtl();
    }

    public ResponseCookie access(String token) {
        return cookie(ACCESS, token, ACCESS_PATH, "Lax", accessTtl);
    }

    public ResponseCookie refresh(String token) {
        return cookie(REFRESH, token, REFRESH_PATH, "Strict", refreshMaxTtl);
    }

    public ResponseCookie clearAccess() {
        return cookie(ACCESS, "", ACCESS_PATH, "Lax", Duration.ZERO);
    }

    public ResponseCookie clearRefresh() {
        return cookie(REFRESH, "", REFRESH_PATH, "Strict", Duration.ZERO);
    }

    public ResponseCookie clearLegacyRefresh() {
        return cookie(REFRESH, "", LEGACY_REFRESH_PATH, "Strict", Duration.ZERO);
    }

    public static Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies).filter(c -> name.equals(c.getName())).map(Cookie::getValue)
                .filter(v -> !v.isBlank()).findFirst();
    }

    private ResponseCookie cookie(String name, String value, String path, String sameSite, Duration maxAge) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(secure).sameSite(sameSite).path(path)
                .maxAge(maxAge).build();
    }
}
