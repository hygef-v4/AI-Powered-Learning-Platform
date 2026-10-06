package edu.aiplatform.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.shared.web.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Mặc định từ chối. Mở health, các endpoint đăng nhập/OTP và webhook PayOS (kiểm chữ ký riêng); mọi API khác dưới
 * `/api/v1` cần đăng nhập, quyền chi tiết do từng unit kiểm. U01 thêm filter đọc JWT từ cookie `access_token` và đặt
 * principal là {@link edu.aiplatform.shared.contract.ActorRef}. CSRF: cookie `SameSite=Lax`, API chỉ nhận JSON
 * hoặc multipart (NFR-U01-12). Lỗi 401/403 trả problem-details.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.deny()))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(res, objectMapper, HttpStatus.UNAUTHORIZED,
                        "UNAUTHENTICATED", "Vui lòng đăng nhập."))
                .accessDeniedHandler((req, res, ex) -> write(res, objectMapper, HttpStatus.FORBIDDEN,
                        "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này.")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health/**").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/auth/login", "/api/v1/auth/refresh",
                        "/api/v1/auth/activation/**", "/api/v1/auth/password-reset/**",
                        "/api/v1/payments/payos/webhook").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll());
        return http.build();
    }

    private static void write(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status, String code,
                              String message) throws IOException {
        ProblemDetail body = GlobalExceptionHandler.problem(status, code, message);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
