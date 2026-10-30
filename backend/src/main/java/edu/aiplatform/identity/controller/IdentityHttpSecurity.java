package edu.aiplatform.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.identity.config.IdentityProperties;
import edu.aiplatform.identity.service.SessionAuthority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Phần bảo mật web U01 gắn vào {@code SecurityConfig} dùng chung: filter JWT, CSRF bằng cookie {@code XSRF-TOKEN}
 * + header {@code X-XSRF-TOKEN} (Q16), lỗi 401/403 dạng Problem Detail, endpoint public của U01 (NFR-U01-53).
 */
@Configuration
public class IdentityHttpSecurity {

    /** Đăng nhập, refresh, đăng xuất, kích hoạt, đặt lại mật khẩu: không cần access token, không cần CSRF token. */
    public static final String PUBLIC_AUTH = "/api/v1/auth/**";

    private final boolean secureCookies;
    private final ObjectMapper objectMapper;

    public IdentityHttpSecurity(IdentityProperties properties, ObjectMapper objectMapper) {
        this.secureCookies = properties.cookieSecure();
        this.objectMapper = objectMapper;
    }

    @Bean
    JwtAuthFilter jwtAuthFilter(SessionAuthority authority) {
        return new JwtAuthFilter(authority, objectMapper);
    }

    /** Filter chỉ chạy trong chuỗi Spring Security, không để Spring Boot đăng ký thêm lần nữa ở servlet. */
    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** Gọi trước khi {@code SecurityConfig} khai báo {@code anyRequest()}. */
    public void configure(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(secureCookies).sameSite("Lax"));
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // JwtAuthFilter dựng Authentication cho từng request; nếu để mặc định, SessionManagementFilter
                        // coi đó là lần đăng nhập mới và xóa cookie XSRF-TOKEN sau mỗi request, làm request ghi kế tiếp
                        // bị 403. Không có phiên server nên không xoay token theo xác thực.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .ignoringRequestMatchers(PUBLIC_AUTH))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, e) -> ProblemResponses.write(response,
                                objectMapper, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Bạn cần đăng nhập"))
                        .accessDeniedHandler((request, response, e) -> {
                            boolean csrf = e instanceof CsrfException;
                            ProblemResponses.write(response, objectMapper, HttpStatus.FORBIDDEN,
                                    csrf ? "CSRF_INVALID" : "FORBIDDEN",
                                    csrf ? "Phiên làm việc không hợp lệ, hãy tải lại trang"
                                            : "Không có quyền thực hiện thao tác này");
                        }))
                .authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_AUTH).permitAll());
    }

    /** Buộc nạp CSRF token để cookie {@code XSRF-TOKEN} luôn được đặt (Spring Security 6 nạp trễ). */
    static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
