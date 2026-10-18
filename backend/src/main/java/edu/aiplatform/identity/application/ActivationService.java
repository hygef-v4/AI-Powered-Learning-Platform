package edu.aiplatform.identity.application;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.OtpPurpose;
import edu.aiplatform.identity.domain.PasswordPolicy;
import edu.aiplatform.identity.infrastructure.VerificationTicketStore;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Hoàn tất kích hoạt (F2 bước 2b): ticket còn hạn và đúng mục đích, mật khẩu đạt chính sách, tài khoản vẫn
 * {@code PENDING} → lưu mật khẩu, chuyển {@code ACTIVE}, ghi audit, xóa ticket, rồi tự đăng nhập (BR-U01-13).
 */
@Service
public class ActivationService {

    public static final String TICKET_EXPIRED = "TICKET_EXPIRED";
    public static final String TICKET_EXPIRED_MESSAGE = "Phiên xác minh đã hết hạn, hãy yêu cầu mã mới.";

    private final VerificationTicketStore tickets;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final AuthService auth;
    private final IAudit audit;
    private final TransactionOperations transactions;
    private final Clock clock;

    public ActivationService(VerificationTicketStore tickets, AccountRepository accounts,
                             PasswordEncoder passwordEncoder, AuthService auth, IAudit audit,
                             TransactionOperations transactions, Clock clock) {
        this.tickets = tickets;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.auth = auth;
        this.audit = audit;
        this.transactions = transactions;
        this.clock = clock;
    }

    public AuthService.LoginResult complete(String ticket, String newPassword) {
        UUID accountId = tickets.peek(ticket, OtpPurpose.ACTIVATION).orElseThrow(ActivationService::ticketExpired);
        Account account = accounts.findById(accountId)
                .filter(OtpPurpose.ACTIVATION::appliesTo)
                .orElseThrow(ActivationService::ticketExpired);
        Set<PasswordPolicy.Violation> violations = PasswordPolicy.check(newPassword, account.schoolEmail());
        if (!violations.isEmpty()) {
            // Ticket giữ nguyên để nhập lại (BR-U01-28).
            throw new PasswordPolicyException(violations);
        }
        String hash = passwordEncoder.encode(newPassword);
        Instant now = clock.instant();
        boolean activated = Boolean.TRUE.equals(transactions.execute(status -> {
            if (accounts.activate(accountId, hash, now) == 0) {
                return false;
            }
            audit.record(new AuditEvent(UUID.randomUUID(), accountId, "ACCOUNT_ACTIVATED", "ACCOUNT",
                    accountId.toString(), AuditResult.SUCCESS, null, Map.of(), now));
            return true;
        }));
        tickets.consume(ticket);
        if (!activated) {
            throw ticketExpired();
        }
        Account active = accounts.findById(accountId).orElseThrow(ActivationService::ticketExpired);
        return auth.startSession(active);
    }

    static ApiException ticketExpired() {
        return ApiException.badRequest(TICKET_EXPIRED, TICKET_EXPIRED_MESSAGE);
    }
}
