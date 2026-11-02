package edu.aiplatform.identity.application;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.infrastructure.JwtService;
import edu.aiplatform.identity.infrastructure.SessionStore;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Dựng {@link Actor} cho mỗi request từ access token (BR-U01-44, NFR-U01-04): chữ ký và hạn JWT, phiên {@code sid}
 * còn trên Redis và đúng chủ, tài khoản còn {@code ACTIVE}; role lấy từ database, không tin role trong token.
 * Redis hoặc database lỗi thì ném lỗi (fail closed), không coi là đăng nhập.
 */
@Service
public class SessionAuthority {

    private final JwtService jwt;
    private final SessionStore sessions;
    private final AccountRepository accounts;

    public SessionAuthority(JwtService jwt, SessionStore sessions, AccountRepository accounts) {
        this.jwt = jwt;
        this.sessions = sessions;
        this.accounts = accounts;
    }

    public record AuthenticatedSession(Actor actor, String sid) {
    }

    public Optional<AuthenticatedSession> authenticate(String accessToken) {
        Optional<JwtService.AccessClaims> claims = jwt.parse(accessToken);
        if (claims.isEmpty()) {
            return Optional.empty();
        }
        JwtService.AccessClaims c = claims.get();
        boolean sessionAlive = sessions.find(c.sid())
                .filter(session -> session.accountId().equals(c.accountId()))
                .isPresent();
        if (!sessionAlive) {
            return Optional.empty();
        }
        Optional<Account> account = accounts.findById(c.accountId())
                .filter(a -> a.status() == AccountStatus.ACTIVE);
        return account.map(a -> new AuthenticatedSession(new Actor(a.id(), a.role()), c.sid()));
    }
}
