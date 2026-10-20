package edu.aiplatform.identity.application;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.AccountSearch;
import edu.aiplatform.identity.domain.EmailPolicy;
import edu.aiplatform.identity.domain.ProfilePolicy;
import edu.aiplatform.identity.infrastructure.OtpChallengeStore;
import edu.aiplatform.identity.infrastructure.SessionStore;
import edu.aiplatform.identity.infrastructure.VerificationTicketStore;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Quản trị tài khoản của Admin (F8, F9, F11, F12, F14; UC 08–12). Mọi thao tác kiểm quyền qua {@link IAuthorization},
 * lần gọi trái phép bị từ chối và ghi audit (BR-U01-67). Sửa dùng {@code version}, lệch thì 409 (MSG17).
 */
@Service
public class AccountAdminService {

    public static final String VERSION_CONFLICT = "VERSION_CONFLICT";

    private final AccountRepository accounts;
    private final IAuthorization authorization;
    private final ISubjectScope subjectScope;
    private final IClassScope classScope;
    private final SessionStore sessions;
    private final OtpChallengeStore otpChallenges;
    private final VerificationTicketStore tickets;
    private final IAudit audit;
    private final TransactionOperations transactions;
    private final EmailPolicy emailPolicy;
    private final Clock clock;

    public AccountAdminService(AccountRepository accounts, IAuthorization authorization, ISubjectScope subjectScope,
                               IClassScope classScope, SessionStore sessions, OtpChallengeStore otpChallenges,
                               VerificationTicketStore tickets, IAudit audit, TransactionOperations transactions, IdentityProperties properties,
                               Clock clock) {
        this.accounts = accounts;
        this.authorization = authorization;
        this.subjectScope = subjectScope;
        this.classScope = classScope;
        this.sessions = sessions;
        this.otpChallenges = otpChallenges;
        this.tickets = tickets;
        this.audit = audit;
        this.transactions = transactions;
        this.emailPolicy = new EmailPolicy(properties.allowedEmailDomains());
        this.clock = clock;
    }

    /** F8: lọc role, trạng thái, tìm email/tên; phân trang. */
    public AccountViews.Page list(Actor actor, AccountSearch search) {
        require(actor, Actions.ACCOUNT_LIST, null);
        AccountSearch.Result result = accounts.search(search);
        return new AccountViews.Page(result.items().stream().map(AccountViews.Summary::of).toList(), result.total(),
                search.page(), search.size());
    }

    /** F14 bước 1 (UC 10). */
    public AccountViews.Detail detail(Actor actor, UUID id) {
        require(actor, Actions.ACCOUNT_VIEW, id);
        return AccountViews.Detail.of(load(id));
    }

    /** F9 (UC 09): tạo {@code PENDING}, không mật khẩu, không gửi mail (BR-U01-10); tạo ADMIN chỉ qua đây (BR-U01-84). */
    public AccountViews.Detail create(Actor actor, String rawEmail, String rawDisplayName, Role role) {
        require(actor, Actions.ACCOUNT_CREATE, null);
        String email = EmailPolicy.normalize(rawEmail);
        switch (emailPolicy.check(email)) {
            case INVALID_EMAIL -> throw ApiException.badRequest("INVALID_EMAIL", "Email không hợp lệ");
            case EMAIL_DOMAIN_NOT_ALLOWED -> throw ApiException.badRequest("EMAIL_DOMAIN_NOT_ALLOWED",
                    "Email phải thuộc tên miền của trường");
            case OK -> {
            }
        }
        String displayName = ProfilePolicy.normalizeDisplayName(rawDisplayName);
        ProfilePolicy.checkDisplayName(displayName).ifPresent(code -> {
            throw ApiException.badRequest(code, "Họ và tên cần từ 1 đến 150 ký tự");
        });
        if (role == null) {
            throw ApiException.badRequest("INVALID_ROLE", "Chọn vai trò");
        }
        Instant now = clock.instant();
        Account account = Account.newPending(UUID.randomUUID(), email, role, displayName, null, now);
        try {
            transactions.executeWithoutResult(status -> {
                if (accounts.findByEmail(email).isPresent()) {
                    throw emailExists();
                }
                accounts.insert(account);
                record(actor, "ACCOUNT_CREATED", account.id(), Map.of("role", role.name()), now);
            });
        } catch (DuplicateKeyException e) {
            throw emailExists();
        }
        return AccountViews.Detail.of(account);
    }

    /** F14 bước 2 (UC 11): chỉ tên, số điện thoại; email không đổi được (BR-U01-04). */
    public AccountViews.Detail update(Actor actor, UUID id, String rawDisplayName, String rawPhoneNumber,
                                     long version) {
        require(actor, Actions.ACCOUNT_UPDATE, id);
        String displayName = ProfilePolicy.normalizeDisplayName(rawDisplayName);
        String phoneNumber = ProfilePolicy.normalizePhone(rawPhoneNumber);
        ProfilePolicy.checkDisplayName(displayName).or(() -> ProfilePolicy.checkPhone(phoneNumber))
                .ifPresent(code -> {
                    throw ApiException.badRequest(code, "INVALID_PHONE_NUMBER".equals(code)
                            ? "Số điện thoại gồm 8–15 chữ số, có thể có dấu + ở đầu"
                            : "Họ và tên cần từ 1 đến 150 ký tự");
                });
        Account before = load(id);
        List<String> changed = new ArrayList<>();
        if (!before.displayName().equals(displayName)) {
            changed.add("displayName");
        }
        if (!Objects.equals(before.phoneNumber(), phoneNumber)) {
            changed.add("phoneNumber");
        }
        Instant now = clock.instant();
        transactions.executeWithoutResult(status -> {
            if (accounts.updateProfile(id, displayName, phoneNumber, version, now) == 0) {
                throw versionConflict();
            }
            // Ghi tên trường đã đổi, không ghi giá trị số điện thoại (BR-U01-91).
            record(actor, "ACCOUNT_UPDATED", id, Map.of("fields", changed), now);
        });
        return AccountViews.Detail.of(load(id));
    }

    /** F11 (UC 11): đổi role, xóa mọi phiên của người bị đổi (BR-U01-63). */
    public AccountViews.Detail changeRole(Actor actor, UUID id, Role newRole, long version) {
        require(actor, Actions.ACCOUNT_ROLE_CHANGE, id);
        if (newRole == null) {
            throw ApiException.badRequest("INVALID_ROLE", "Chọn vai trò");
        }
        Account target = load(id);
        if (target.role() == newRole) {
            return AccountViews.Detail.of(target);
        }
        if (id.equals(actor.accountId())) {
            // Người đổi role luôn là ADMIN nên mọi thay đổi role của chính mình đều là tự hạ quyền (BR-U01-65).
            throw ApiException.conflict("SELF_CHANGE_BLOCKED", "Không thể tự đổi vai trò của chính mình");
        }
        checkAssignmentsKept(id, newRole);
        Instant now = clock.instant();
        transactions.executeWithoutResult(status -> {
            protectLastAdmin(target, newRole != Role.ADMIN);
            if (accounts.updateRole(id, newRole, version, now) == 0) {
                throw versionConflict();
            }
            record(actor, "ROLE_CHANGED", id, Map.of("from", target.role().name(), "to", newRole.name()), now);
            sessions.deleteAll(id);
        });
        sessions.deleteAll(id);
        return AccountViews.Detail.of(load(id));
    }

    /**
     * F12 (UC 12): {@code DISABLED} vô hiệu hóa (gỡ khỏi Chủ nhiệm môn trong cùng transaction, xóa mọi phiên và OTP,
     * BR-U01-71); {@code ACTIVE} là mở lại — về
     * {@code ACTIVE} nếu đã có mật khẩu, về {@code PENDING} nếu chưa (BR-U01-72). Không có xóa tài khoản (BR-U01-73).
     */
    public AccountViews.Detail changeStatus(Actor actor, UUID id, AccountStatus requested, long version) {
        require(actor, Actions.ACCOUNT_STATUS_CHANGE, id);
        if (requested != AccountStatus.ACTIVE && requested != AccountStatus.DISABLED) {
            throw ApiException.badRequest("INVALID_STATUS", "Chỉ vô hiệu hóa hoặc mở lại");
        }
        Account target = load(id);
        boolean disable = requested == AccountStatus.DISABLED;
        if (disable == (target.status() == AccountStatus.DISABLED)) {
            return AccountViews.Detail.of(target);
        }
        if (disable && id.equals(actor.accountId())) {
            throw ApiException.conflict("SELF_CHANGE_BLOCKED", "Không thể tự vô hiệu hóa chính mình");
        }
        AccountStatus next = disable ? AccountStatus.DISABLED : target.statusAfterReopen();
        Instant now = clock.instant();
        transactions.executeWithoutResult(status -> {
            if (disable) {
                protectLastAdmin(target, true);
            }
            if (accounts.updateStatus(id, next, version, now) == 0) {
                throw versionConflict();
            }
            if (disable) {
                subjectScope.releaseManagerAssignments(id, actor);
            }
            record(actor, disable ? "ACCOUNT_DISABLED" : "ACCOUNT_ENABLED", id, Map.of("status", next.name()), now);
            if (disable) {
                sessions.deleteAll(id);
            }
        });
        if (disable) {
            sessions.deleteAll(id);
            otpChallenges.deleteAll(id);
            tickets.revokeAll(id);
        }
        return AccountViews.Detail.of(load(id));
    }

    /** BR-U01-64: Chủ nhiệm môn cần SUBJECT_MANAGER; giảng viên lớp cần TEACHER hoặc SUBJECT_MANAGER. */
    private void checkAssignmentsKept(UUID id, Role newRole) {
        List<UUID> subjects = newRole == Role.SUBJECT_MANAGER ? List.of() : subjectScope.listManagedSubjects(id);
        List<UUID> classes = newRole == Role.TEACHER || newRole == Role.SUBJECT_MANAGER
                ? List.of() : classScope.listTaughtClasses(id);
        if (!subjects.isEmpty() || !classes.isEmpty()) {
            throw new RoleChangeBlockedException(subjects, classes);
        }
    }

    /** BR-U01-66: khóa các dòng Admin đang hoạt động rồi mới kiểm, để hai Admin không cùng lúc hạ quyền nhau. */
    private void protectLastAdmin(Account target, boolean removesAdmin) {
        if (!removesAdmin || target.role() != Role.ADMIN) {
            return;
        }
        List<UUID> activeAdmins = accounts.lockActiveAdmins();
        if (activeAdmins.contains(target.id()) && activeAdmins.size() <= 1) {
            throw ApiException.conflict("LAST_ADMIN", "Phải còn ít nhất một Admin đang hoạt động");
        }
    }

    private void require(Actor actor, String action, UUID target) {
        AuthorizationDecision decision = actor == null ? AuthorizationDecision.deny("NO_ACTOR")
                : authorization.authorize(actor, action,
                        target == null ? ResourceRef.none() : ResourceRef.account(target));
        AuthorizationService.throwIfUnavailable(decision);
        if (!decision.allowed()) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                    "ACCOUNT", target == null ? null : target.toString(), AuditResult.DENIED, action, Map.of(),
                    clock.instant()));
            throw ApiException.forbidden("ACCOUNT_ADMIN_FORBIDDEN");
        }
    }

    private Account load(UUID id) {
        return accounts.findById(id).orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND"));
    }

    private void record(Actor actor, String action, UUID accountId, Map<String, Object> details, Instant now) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "ACCOUNT", accountId.toString(),
                AuditResult.SUCCESS, null, details, now));
    }

    private static ApiException emailExists() {
        return ApiException.conflict("EMAIL_EXISTS", "Email đã có tài khoản");
    }

    private static ApiException versionConflict() {
        return ApiException.conflict(VERSION_CONFLICT, "Dữ liệu đã được thay đổi ở phiên khác. Hãy tải lại trước khi sửa.");
    }
}
