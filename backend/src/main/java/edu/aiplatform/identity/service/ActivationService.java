package edu.aiplatform.identity.service;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.model.OtpPurpose;
import edu.aiplatform.identity.model.PasswordPolicy;
import edu.aiplatform.identity.integration.VerificationTicketStore;
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
 * {@code PENDING} → lấy và xóa ticket (dùng một lần), lưu mật khẩu, chuyển {@code ACTIVE}, ghi audit, rồi tự đăng nhập
 * (BR-U01-13).
 */
@Service
public class ActivationService {

    public static final String TICKET_EXPIRED = "TICKET_EXPIRED";
    public static final String TICKET_EXPIRED_MESSAGE = "Phiên xác minh đã hết hạn, hãy yêu cầu mã mới.";

    private final VerificationTicketStore tickets;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final IAudit audit;
    private final TransactionOperations transactions;
    private final Clock clock;

    public ActivationService(VerificationTicketStore tickets, AccountRepository accounts,
                             PasswordEncoder passwordEncoder, AuthService authService, IAudit audit,
                             TransactionOperations transactions, Clock clock) {
        this.tickets = tickets;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.audit = audit;
        this.transactions = transactions;
        this.clock = clock;
    }

    public AuthService.LoginResult complete(String ticket, String newPassword) {
        UUID accountId = tickets.peek(ticket, OtpPurpose.ACTIVATION).orElseThrow(ActivationService::createTicketExpiredError);
        Account account = accounts.findById(accountId)
                .filter(OtpPurpose.ACTIVATION::appliesTo)
                .orElseThrow(ActivationService::createTicketExpiredError);
        Set<PasswordPolicy.Violation> violations = PasswordPolicy.checkPassword(newPassword, account.schoolEmail());
        if (!violations.isEmpty()) {
            // Ticket giữ nguyên để nhập lại (BR-U01-28).
            throw new PasswordPolicyException(violations);
        }
        if (!tickets.take(ticket, OtpPurpose.ACTIVATION, accountId)) {
            throw createTicketExpiredError();
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
        if (!activated) {
            throw createTicketExpiredError();
        }
        Account active = accounts.findById(accountId).orElseThrow(ActivationService::createTicketExpiredError);
        return authService.startSession(active);
    }

    static ApiException createTicketExpiredError() {
        return ApiException.createBadRequest(TICKET_EXPIRED, TICKET_EXPIRED_MESSAGE);
    }
}
