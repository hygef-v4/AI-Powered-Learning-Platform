package edu.aiplatform.identity.service;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.model.ProfilePolicy;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Hồ sơ của chính mình (F7): xem (UC 06) và sửa tên hiển thị, số điện thoại (UC 07). Luôn là tài khoản của phiên,
 * không nhận ID (BR-U01-54); email, role, trạng thái không sửa được (BR-U01-51); không có ảnh đại diện (BR-U01-50).
 * Không có {@code version} cho hồ sơ cá nhân (Q18): request sau ghi đè.
 */
@Service
public class ProfileService {

    private final AccountRepository accounts;
    private final IAuthorization authorization;
    private final IAudit audit;
    private final TransactionOperations transactions;
    private final Clock clock;

    public ProfileService(AccountRepository accounts, IAuthorization authorization, IAudit audit,
                          TransactionOperations transactions, Clock clock) {
        this.accounts = accounts;
        this.authorization = authorization;
        this.audit = audit;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** Không có hash, OTP hay token (BR-U01-53: số điện thoại chỉ chủ tài khoản và Admin xem). */
    public record ProfileView(UUID accountId, String email, String displayName, String phoneNumber, Role role,
                              AccountStatus status, String homePath) {
    }

    public ProfileView get(Actor actor) {
        requireOwn(actor, Actions.PROFILE_VIEW);
        return view(load(actor));
    }

    public ProfileView update(Actor actor, String rawDisplayName, String rawPhoneNumber) {
        requireOwn(actor, Actions.PROFILE_UPDATE);
        String displayName = ProfilePolicy.normalizeDisplayName(rawDisplayName);
        String phoneNumber = ProfilePolicy.normalizePhone(rawPhoneNumber);
        ProfilePolicy.checkDisplayName(displayName).or(() -> ProfilePolicy.checkPhone(phoneNumber))
                .ifPresent(code -> {
                    throw ApiException.badRequest(code, code.equals("INVALID_PHONE_NUMBER")
                            ? "Số điện thoại gồm 8–15 chữ số, có thể có dấu + ở đầu"
                            : "Họ và tên cần từ 1 đến 150 ký tự");
                });
        Account before = load(actor);
        List<String> changed = new ArrayList<>();
        if (!before.displayName().equals(displayName)) {
            changed.add("displayName");
        }
        if (!Objects.equals(before.phoneNumber(), phoneNumber)) {
            changed.add("phoneNumber");
        }
        if (changed.isEmpty()) {
            return view(before);
        }
        Instant now = clock.instant();
        transactions.executeWithoutResult(status -> {
            accounts.updateProfile(before.id(), displayName, phoneNumber, null, now);
            // Chỉ ghi tên trường đã đổi, không ghi giá trị số điện thoại (BR-U01-53, 91).
            audit.record(new AuditEvent(UUID.randomUUID(), before.id(), "PROFILE_UPDATED", "ACCOUNT",
                    before.id().toString(), AuditResult.SUCCESS, null, Map.of("fields", changed), now));
        });
        return view(load(actor));
    }

    private void requireOwn(Actor actor, String action) {
        AuthorizationDecision decision = actor == null ? AuthorizationDecision.deny("NO_ACTOR")
                : authorization.authorize(actor, action, ResourceRef.account(actor.accountId()));
        AuthorizationService.throwIfUnavailable(decision);
        if (!decision.allowed()) {
            throw ApiException.forbidden("PROFILE_FORBIDDEN");
        }
    }

    private Account load(Actor actor) {
        return accounts.findById(actor.accountId()).orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND"));
    }

    private static ProfileView view(Account account) {
        return new ProfileView(account.id(), account.schoolEmail(), account.displayName(), account.phoneNumber(),
                account.role(), account.status(), HomePaths.of(account.role()));
    }
}
