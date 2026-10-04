package edu.aiplatform.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Mặc định từ chối. Chỉ mở health, các endpoint đăng nhập/OTP và webhook PayOS (kiểm chữ ký riêng).
 * U01 thêm filter đọc JWT từ cookie `access_token` và mở các route đã xác thực.
 * CSRF: cookie `SameSite=Lax`, API chỉ nhận JSON (NFR-U01-12).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.deny()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health/**").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/auth/login", "/api/v1/auth/refresh",
                        "/api/v1/auth/activation/**", "/api/v1/auth/password-reset/**",
                        "/api/v1/payments/payos/webhook").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/files/download/*").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }
}
