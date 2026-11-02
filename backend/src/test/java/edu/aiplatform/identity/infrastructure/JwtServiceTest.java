package edu.aiplatform.identity.infrastructure;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.application.IdentityProperties;
import edu.aiplatform.identity.contract.Role;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static JwtService at(java.time.Instant instant, IdentityProperties properties) {
        return new JwtService(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void issuedTokenParsesBackToClaims() {
        JwtService jwt = at(NOW, IdentityTestData.properties());
        UUID accountId = UUID.randomUUID();
        String token = jwt.issue(accountId, Role.TEACHER, "sid-1");

        JwtService.AccessClaims claims = jwt.parse(token).orElseThrow();
        assertThat(claims.accountId()).isEqualTo(accountId);
        assertThat(claims.role()).isEqualTo(Role.TEACHER);
        assertThat(claims.sid()).isEqualTo("sid-1");
        assertThat(claims.expiresAt()).isEqualTo(NOW.plusSeconds(15 * 60));
    }

    @Test
    void rejectsExpiredToken() {
        String token = at(NOW, IdentityTestData.properties()).issue(UUID.randomUUID(), Role.STUDENT, "s");
        assertThat(at(NOW.plusSeconds(15 * 60 + 1), IdentityTestData.properties()).parse(token)).isEmpty();
    }

    @Test
    void rejectsTamperedTokenAndOtherKey() {
        JwtService jwt = at(NOW, IdentityTestData.properties());
        String token = jwt.issue(UUID.randomUUID(), Role.STUDENT, "s");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
        assertThat(jwt.parse(tampered)).isEmpty();
        assertThat(jwt.parse("not-a-jwt")).isEmpty();
        assertThat(jwt.parse(null)).isEmpty();

        IdentityProperties other = withSecret("ffffffffffffffffffffffffffffffff");
        assertThat(at(NOW, other).parse(token)).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> at(NOW, withSecret("short"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> at(NOW, withSecret(null))).isInstanceOf(IllegalStateException.class);
    }

    private static IdentityProperties withSecret(String secret) {
        IdentityProperties p = IdentityTestData.properties();
        return new IdentityProperties(p.allowedEmailDomains(), secret, p.accessTtl(), p.refreshIdleTtl(),
                p.refreshMaxTtl(), p.bcryptCost(), p.otpTtl(), p.otpMaxAttempts(), p.loginLockThreshold(),
                p.loginLockDuration(), p.cookieSecure(), p.mailFrom(), p.bootstrapAdminEmail(),
                p.bootstrapAdminName(), p.rateLimit());
    }
}
