package edu.aiplatform.identity.application;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.OtpPurpose;
import edu.aiplatform.identity.domain.PasswordPolicy;
import edu.aiplatform.identity.infrastructure.SessionStore;
import edu.aiplatform.identity.infrastructure.VerificationTicketStore;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Hoàn tất đặt lại mật khẩu (F5 bước 5–6): lưu mật khẩu mới, xóa khóa tạm, ghi audit {@code PASSWORD_RESET}, xóa
 * **mọi** phiên (BR-U01-34). Không tự đăng nhập; người dùng đăng nhập lại bằng mật khẩu mới.
 */
@Service
public class PasswordResetService {

    private final VerificationTicketStore tickets;
    private final AccountRepository accounts;
    private final SessionStore sessions;
    private final PasswordEncoder passwordEncoder;
    private final IAudit audit;
    private final TransactionOperations transactions;
    private final Clock clock;

    public PasswordResetService(VerificationTicketStore tickets, AccountRepository accounts, SessionStore sessions,
                                PasswordEncoder passwordEncoder, IAudit audit, TransactionOperations transactions,
                                Clock clock) {
        this.tickets = tickets;
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.transactions = transactions;
        this.clock = clock;
    }

    public void complete(String ticket, String newPassword) {
        UUID accountId = tickets.peek(ticket, OtpPurpose.PASSWORD_RESET)
                .orElseThrow(ActivationService::ticketExpired);
        Account account = accounts.findById(accountId)
                .filter(OtpPurpose.PASSWORD_RESET::appliesTo)
                .orElseThrow(ActivationService::ticketExpired);
        Set<PasswordPolicy.Violation> violations = PasswordPolicy.check(newPassword, account.schoolEmail());
        if (!violations.isEmpty()) {
            throw new PasswordPolicyException(violations);
        }
        String hash = passwordEncoder.encode(newPassword);
        Instant now = clock.instant();
        transactions.executeWithoutResult(status -> {
            accounts.updatePassword(accountId, hash, now);
            audit.record(new AuditEvent(UUID.randomUUID(), accountId, "PASSWORD_RESET", "ACCOUNT",
                    accountId.toString(), AuditResult.SUCCESS, null, Map.of(), now));
            // Xóa phiên trước commit; lặp lại sau commit để không sót phiên tạo chen giữa (P1).
            sessions.deleteAll(accountId);
        });
        tickets.consume(ticket);
        sessions.deleteAll(accountId);
    }
}
