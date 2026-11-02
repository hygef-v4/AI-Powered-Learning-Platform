package edu.aiplatform.identity.service;

import edu.aiplatform.identity.config.IdentityProperties;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.model.EmailPolicy;
import edu.aiplatform.identity.model.PasswordPolicy;
import edu.aiplatform.identity.integration.Hashing;
import edu.aiplatform.identity.integration.JwtService;
import edu.aiplatform.identity.integration.RequestThrottle;
import edu.aiplatform.identity.integration.SessionStore;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Đăng nhập (F3), refresh, đăng xuất (F4). Mọi thất bại đăng nhập trả **cùng** một lỗi (BR-U01-40, Q05) và vẫn chạy
 * bcrypt với hash giả khi email không tồn tại để thời gian phản hồi đồng đều (NFR-U01-16). Không có MFA (BR-U01-47).
 */
@Service
public class AuthService {

    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String AUTH_FAILED_MESSAGE =
            "Email hoặc mật khẩu không đúng, hoặc tài khoản chưa sẵn sàng.";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
    /** Request khác (tab khác) vừa xoay cùng refresh token; cookie mới đã được đặt, client chỉ cần gửi lại. */
    public static final String REFRESH_CONCURRENT = "REFRESH_CONCURRENT";

    private final AccountRepository accounts;
    private final SessionStore sessions;
    private final JwtService jwt;
    private final RequestThrottle throttle;
    private final PasswordEncoder passwordEncoder;
    private final IAudit audit;
    private final IdentityProperties properties;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(AccountRepository accounts, SessionStore sessions, JwtService jwt, RequestThrottle throttle,
                       PasswordEncoder passwordEncoder, IAudit audit, IdentityProperties properties, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.jwt = jwt;
        this.throttle = throttle;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(Hashing.randomToken(16));
    }

    /** Phiên vừa cấp: thông tin cho frontend và hai token cho cookie. */
    public record LoginResult(SessionInfo info, String accessToken, String refreshToken) {
    }

    /** Token mới sau refresh. */
    public record RefreshResult(String accessToken, String refreshToken) {
    }

    public LoginResult login(String rawEmail, String rawPassword, String clientIp) {
        if (!throttle.loginAllowed(clientIp)) {
            throw authFailed();
        }
        String email = EmailPolicy.normalize(rawEmail);
        String password = rawPassword == null ? "" : rawPassword;
        Optional<Account> found = accounts.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            record(null, "LOGIN_FAILED", null, AuditResult.FAILURE, "UNKNOWN_EMAIL");
            throw failed(clientIp);
        }
        Account account = found.get();
        Instant now = clock.instant();
        boolean passwordMatches = passwordMatches(password, account);
        if (account.isLocked(now)) {
            record(account.id(), "LOGIN_BLOCKED_LOCKED", account.id(), AuditResult.DENIED, "LOCKED");
            throw failed(clientIp);
        }
        if (account.status() != AccountStatus.ACTIVE) {
            record(account.id(), "LOGIN_FAILED", account.id(), AuditResult.FAILURE, "STATUS_" + account.status());
            throw failed(clientIp);
        }
        if (!passwordMatches) {
            boolean locked = accounts.recordFailedLogin(account.id(), properties.loginLockThreshold(),
                    now.plus(properties.loginLockDuration()));
            record(account.id(), "LOGIN_FAILED", account.id(), AuditResult.FAILURE, "BAD_PASSWORD");
            if (locked) {
                record(account.id(), "ACCOUNT_TEMP_LOCKED", account.id(), AuditResult.DENIED, "TOO_MANY_FAILURES");
            }
            throw failed(clientIp);
        }
        accounts.resetLoginThrottle(account.id());
        return startSession(account);
    }

    /** Tạo phiên mới cho tài khoản {@code ACTIVE}; dùng cả khi kích hoạt xong (BR-U01-13). */
    public LoginResult startSession(Account account) {
        SessionStore.IssuedSession session = sessions.create(account.id());
        String accessToken = jwt.issue(account.id(), account.role(), session.sid());
        SessionInfo info = new SessionInfo(account.id(), account.displayName(), account.role(),
                HomePaths.of(account.role()));
        return new LoginResult(info, accessToken, session.refreshToken());
    }

    /** Xoay refresh token; tài khoản không còn {@code ACTIVE} thì thu hồi phiên (BR-U01-44, 46). */
    public RefreshResult refresh(String refreshToken) {
        SessionStore.IRotateResult result = sessions.rotate(refreshToken);
        if (result instanceof SessionStore.Rejected rejected && rejected.reason() == SessionStore.Reason.CONCURRENT) {
            throw new ApiException(HttpStatus.CONFLICT, REFRESH_CONCURRENT, "Phiên vừa được làm mới, hãy thử lại");
        }
        if (!(result instanceof SessionStore.Rotated rotated)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, SESSION_EXPIRED, "Phiên đăng nhập đã hết hạn");
        }
        Optional<Account> account = accounts.findById(rotated.accountId())
                .filter(a -> a.status() == AccountStatus.ACTIVE);
        if (account.isEmpty()) {
            sessions.delete(rotated.sid());
            throw new ApiException(HttpStatus.UNAUTHORIZED, SESSION_EXPIRED, "Phiên đăng nhập đã hết hạn");
        }
        String accessToken = jwt.issue(rotated.accountId(), account.get().role(), rotated.sid());
        return new RefreshResult(accessToken, rotated.refreshToken());
    }

    /**
     * Chỉ thu hồi phiên hiện tại; phiên khác giữ nguyên (BR-U01-45). Access token đã hết hạn thì không có {@code sid},
     * phiên được tìm qua refresh token để vẫn thu hồi trên server, không chỉ xóa cookie.
     */
    public void logout(String sid, String refreshToken) {
        if (sid != null) {
            sessions.delete(sid);
        }
        sessions.sidOf(refreshToken).filter(other -> !other.equals(sid)).ifPresent(sessions::delete);
    }

    /** Mật khẩu quá 72 byte không bao giờ đúng (NFR-U01-14); vẫn chạy bcrypt để thời gian đồng đều. */
    private boolean passwordMatches(String password, Account account) {
        boolean tooLong = password.getBytes(StandardCharsets.UTF_8).length > PasswordPolicy.MAX_BYTES;
        if (tooLong || !account.hasPassword()) {
            passwordEncoder.matches("", dummyHash);
            return false;
        }
        return passwordEncoder.matches(password, account.passwordHash());
    }

    /** Lần thất bại trừ một lượt của IP (BR-U01-43); đăng nhập đúng không trừ. */
    private ApiException failed(String clientIp) {
        throttle.recordLoginFailure(clientIp);
        return authFailed();
    }

    private static ApiException authFailed() {
        return new ApiException(HttpStatus.UNAUTHORIZED, AUTH_FAILED, AUTH_FAILED_MESSAGE);
    }

    /** Không ghi email hay mật khẩu vào audit (BR-U01-91). */
    private void record(UUID actorId, String action, UUID accountId, AuditResult result, String reason) {
        audit.record(new AuditEvent(UUID.randomUUID(), actorId, action, "ACCOUNT",
                accountId == null ? null : accountId.toString(), result, reason, Map.of(), clock.instant()));
    }
}
