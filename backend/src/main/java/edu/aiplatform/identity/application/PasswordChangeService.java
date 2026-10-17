package edu.aiplatform.identity.application;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.PasswordPolicy;
import edu.aiplatform.identity.infrastructure.SessionStore;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Đổi mật khẩu (F6, UC 05): phải đúng mật khẩu hiện tại (BR-U01-33), mật khẩu mới đạt chính sách (BR-U01-30, 31).
 * Lưu xong thì xóa mọi phiên khác, giữ phiên hiện tại (BR-U01-34); credential MCP của U06 không bị thu hồi theo.
 */
@Service
public class PasswordChangeService {

    public static final String CURRENT_PASSWORD_INVALID = "CURRENT_PASSWORD_INVALID";

    private final AccountRepository accounts;
    private final IAuthorization authorization;
    private final SessionStore sessions;
    private final PasswordEncoder passwordEncoder;
    private final IAudit audit;
    private final TransactionOperations transactions;
    private final Clock clock;

    public PasswordChangeService(AccountRepository accounts, IAuthorization authorization, SessionStore sessions,
                                 PasswordEncoder passwordEncoder, IAudit audit, TransactionOperations transactions,
                                 Clock clock) {
        this.accounts = accounts;
        this.authorization = authorization;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.transactions = transactions;
        this.clock = clock;
    }

    public void change(Actor actor, String currentSid, String currentPassword, String newPassword) {
        if (actor == null || !authorization.authorize(actor, Actions.PASSWORD_CHANGE,
                ResourceRef.account(actor.accountId())).allowed()) {
            throw ApiException.forbidden("PASSWORD_CHANGE_FORBIDDEN");
        }
        Account account = accounts.findById(actor.accountId())
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND"));
        if (!currentMatches(currentPassword, account)) {
            throw ApiException.badRequest(CURRENT_PASSWORD_INVALID, "Mật khẩu hiện tại không đúng");
        }
        Set<PasswordPolicy.Violation> violations = PasswordPolicy.check(newPassword, account.schoolEmail());
        if (!violations.isEmpty()) {
            throw new PasswordPolicyException(violations);
        }
        String hash = passwordEncoder.encode(newPassword);
        Instant now = clock.instant();
        transactions.executeWithoutResult(status -> {
            accounts.updatePassword(account.id(), hash, now);
            audit.record(new AuditEvent(UUID.randomUUID(), account.id(), "PASSWORD_CHANGED", "ACCOUNT",
                    account.id().toString(), AuditResult.SUCCESS, null, Map.of(), now));
            sessions.deleteAllExcept(account.id(), currentSid);
        });
        // Lặp lại sau commit để không sót phiên tạo chen giữa (P1).
        sessions.deleteAllExcept(account.id(), currentSid);
    }

    private boolean currentMatches(String currentPassword, Account account) {
        if (currentPassword == null || !account.hasPassword()
                || currentPassword.getBytes(StandardCharsets.UTF_8).length > PasswordPolicy.MAX_BYTES) {
            return false;
        }
        return passwordEncoder.matches(currentPassword, account.passwordHash());
    }
}
