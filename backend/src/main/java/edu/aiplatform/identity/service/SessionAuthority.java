package edu.aiplatform.identity.service;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.integration.JwtService;
import edu.aiplatform.identity.integration.SessionStore;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Dựng {@link Actor} cho mỗi request từ access token (BR-U01-44, NFR-U01-04): chữ ký và hạn JWT, phiên {@code sid}
 * còn trên Redis và đúng chủ, tài khoản còn {@code ACTIVE}; role lấy từ database, không tin role trong token.
 * Redis hoặc database lỗi thì ném lỗi (fail closed), không coi là đăng nhập.
 */
@Service
public class SessionAuthority {

    private final JwtService jwtService;
    private final SessionStore sessions;
    private final AccountRepository accounts;

    public SessionAuthority(JwtService jwtService, SessionStore sessions, AccountRepository accounts) {
        this.jwtService = jwtService;
        this.sessions = sessions;
        this.accounts = accounts;
    }

    public record AuthenticatedSession(Actor actor, String sid) {
    }

    public Optional<AuthenticatedSession> authenticate(String accessToken) {
        Optional<JwtService.AccessClaims> claims = jwtService.parse(accessToken);
        if (claims.isEmpty()) {
            return Optional.empty();
        }
        JwtService.AccessClaims c = claims.get();
        Optional<SessionStore.SessionRecord> session = sessions.findSession(c.sid())
                .filter(s -> s.accountId().equals(c.accountId()));
        if (session.isEmpty()) {
            return Optional.empty();
        }
        Optional<Account> account = accounts.findById(c.accountId())
                .filter(a -> a.status() == AccountStatus.ACTIVE);
        if (account.isPresent() && !session.get().isPasswordCurrent(account.get().passwordChangedAt())) {
            session = sessions.findSession(c.sid())
                    .filter(s -> s.accountId().equals(c.accountId()))
                    .filter(s -> s.isPasswordCurrent(account.get().passwordChangedAt()));
            if (session.isEmpty()) {
                sessions.deleteSession(c.sid());
                return Optional.empty();
            }
        }
        return account.map(a -> new AuthenticatedSession(new Actor(a.id(), a.role()), c.sid()));
    }
}
