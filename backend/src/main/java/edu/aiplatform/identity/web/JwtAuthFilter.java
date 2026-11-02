package edu.aiplatform.identity.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.identity.application.SessionAuthority;
import edu.aiplatform.identity.contract.Actor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Đọc cookie {@code access_token}, kiểm phiên qua {@link SessionAuthority} rồi đặt principal là {@link Actor}
 * (controller dùng {@code @AuthenticationPrincipal Actor}). Token sai, hết hạn hoặc phiên đã thu hồi thì không đặt gì,
 * Spring Security trả 401. Redis hoặc database lỗi thì trả 503 ngay (fail closed, Q04).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Thuộc tính request giữ {@code sid} của phiên hiện tại, dùng khi đăng xuất và đổi mật khẩu. */
    public static final String SID_ATTRIBUTE = JwtAuthFilter.class.getName() + ".sid";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final SessionAuthority authority;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(SessionAuthority authority, ObjectMapper objectMapper) {
        this.authority = authority;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<String> token = AuthCookies.read(request, AuthCookies.ACCESS);
        if (token.isPresent()) {
            Optional<SessionAuthority.AuthenticatedSession> session;
            try {
                session = authority.authenticate(token.get());
            } catch (DataAccessException e) {
                log.warn("Không kiểm được phiên: {}", e.getClass().getSimpleName());
                ProblemResponses.write(response, objectMapper, HttpStatus.SERVICE_UNAVAILABLE, "SYSTEM_BUSY",
                        "Hệ thống tạm bận, vui lòng thử lại sau");
                return;
            }
            session.ifPresent(s -> {
                Actor actor = s.actor();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(actor,
                        null, List.of(new SimpleGrantedAuthority("ROLE_" + actor.role().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(SID_ATTRIBUTE, s.sid());
            });
        }
        chain.doFilter(request, response);
    }
}
