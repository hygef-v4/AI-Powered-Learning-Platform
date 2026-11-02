package edu.aiplatform.identity.infrastructure;

import edu.aiplatform.identity.application.IdentityProperties;
import edu.aiplatform.identity.contract.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Access token JWT HS256, sống 15 phút, mang {@code sub} (accountId), {@code role}, {@code sid}, {@code jti}
 * (NFR-U01-10, P1). Role trong token chỉ để tham khảo; quyền luôn đọc role hiện hành từ database (NFR-U01-04).
 * Khóa ký lấy từ {@code U01_JWT_SECRET}, tối thiểu 32 byte; thiếu thì backend không khởi động (NFR-U01-51).
 */
@Component
public class JwtService {

    public static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration accessTtl;
    private final Clock clock;

    public JwtService(IdentityProperties properties, Clock clock) {
        String secret = properties.jwtSecret();
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("U01_JWT_SECRET phải có ít nhất " + MIN_SECRET_BYTES + " byte");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = properties.accessTtl();
        this.clock = clock;
    }

    public record AccessClaims(UUID accountId, Role role, String sid, Instant expiresAt) {
    }

    public String issue(UUID accountId, Role role, String sid) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(accountId.toString())
                .claim("role", role.name())
                .claim("sid", sid)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Token hợp lệ (đúng chữ ký, còn hạn, đủ claim); sai thì rỗng, không nói lý do. */
    public Optional<AccessClaims> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AccessClaims(UUID.fromString(claims.getSubject()),
                    Role.valueOf(claims.get("role", String.class)), claims.get("sid", String.class),
                    claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public Duration accessTtl() {
        return accessTtl;
    }
}
