package edu.aiplatform.shared.config;

import edu.aiplatform.identity.web.IdentityHttpSecurity;
import edu.aiplatform.identity.web.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình nền: không phiên server, không form login hay basic auth, chỉ {@code /health} mở công khai.
 * U01 gắn filter JWT, cookie CSRF, lỗi 401/403 và endpoint public qua {@link IdentityHttpSecurity}.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, IdentityHttpSecurity identity,
                                            JwtAuthFilter jwtAuthFilter) throws Exception {
        identity.configure(http, jwtAuthFilter);
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/health/**").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
