package edu.aiplatform.identity.service;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.IClassScope;
import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.contract.ScopeKind;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.shared.error.ApiException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Cài {@link IAuthorization} (F13), mặc định từ chối. Đọc tài khoản hiện hành (không tin role do người gọi truyền),
 * tra {@link AccessPolicy}, rồi hỏi U04/U05 về phạm vi môn/lớp. Phụ thuộc lỗi thì từ chối (BR-U01-93).
 * Không tự ghi audit: unit gọi ghi {@code ACCESS_DENIED} cho thao tác của mình (như U03 đang làm), tránh ghi hai lần.
 */
@Service
public class AuthorizationService implements IAuthorization {

    /** Lý do từ chối khi database hoặc U04/U05 lỗi (BR-U01-93). */
    public static final String DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE";

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    private final AccountRepository accounts;
    private final ISubjectScope subjectScope;
    private final IClassScope classScope;

    public AuthorizationService(AccountRepository accounts, ISubjectScope subjectScope, IClassScope classScope) {
        this.accounts = accounts;
        this.subjectScope = subjectScope;
        this.classScope = classScope;
    }

    @Override
    public AuthorizationDecision authorize(Actor actor, String action, ResourceRef resourceRef) {
        try {
            return decide(actor, action, resourceRef == null ? ResourceRef.none() : resourceRef);
        } catch (RuntimeException e) {
            log.warn("Không kiểm được quyền {}: {}", action, e.getClass().getSimpleName());
            return AuthorizationDecision.deny(DEPENDENCY_UNAVAILABLE);
        }
    }

    /**
     * Thao tác của U01 bị từ chối vì phụ thuộc lỗi: vẫn không cho làm (BR-U01-93) nhưng trả 503 "hệ thống tạm bận" thay vì
     * 403 "không có quyền", và không ghi {@code ACCESS_DENIED} oan cho người dùng (BR-U01-95).
     */
    static void throwIfUnavailable(AuthorizationDecision decision) {
        if (!decision.allowed() && DEPENDENCY_UNAVAILABLE.equals(decision.reason())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SYSTEM_BUSY",
                    "Hệ thống tạm bận, vui lòng thử lại sau");
        }
    }

    private AuthorizationDecision decide(Actor actor, String action, ResourceRef ref) {
        if (actor == null || actor.accountId() == null) {
            return AuthorizationDecision.deny("NO_ACTOR");
        }
        Optional<AccessPolicy.Rule> rule = AccessPolicy.ruleFor(action);
        if (rule.isEmpty()) {
            return AuthorizationDecision.deny("UNKNOWN_ACTION");
        }
        Optional<Account> account = accounts.findById(actor.accountId());
        if (account.isEmpty() || account.get().status() != AccountStatus.ACTIVE) {
            return AuthorizationDecision.deny("ACCOUNT_NOT_ACTIVE");
        }
        Role role = account.get().role();
        if (!rule.get().roles().contains(role)) {
            return AuthorizationDecision.deny("ROLE_NOT_ALLOWED");
        }
        return checkScope(rule.get().scope(), actor, role, ref);
    }

    private AuthorizationDecision checkScope(ScopeKind scope, Actor actor, Role role, ResourceRef ref) {
        return switch (scope) {
            case NONE -> AuthorizationDecision.allow();
            case ACCOUNT -> ref.scope() == ScopeKind.ACCOUNT && actor.accountId().equals(ref.id())
                    ? AuthorizationDecision.allow() : AuthorizationDecision.deny("NOT_OWNER");
            case SUBJECT -> role != Role.ADMIN && ref.scope() == ScopeKind.SUBJECT && ref.id() != null
                    && subjectScope.isSubjectManager(actor.accountId(), ref.id())
                    ? AuthorizationDecision.allow() : AuthorizationDecision.deny("OUT_OF_SUBJECT_SCOPE");
            case CLASS -> role != Role.ADMIN && ref.scope() == ScopeKind.CLASS && ref.id() != null
                    && inClass(actor, role, ref)
                    ? AuthorizationDecision.allow() : AuthorizationDecision.deny("OUT_OF_CLASS_SCOPE");
        };
    }

    /** Student cần đang ghi danh (R5); Teacher, Subject Manager cần được giao dạy lớp (R3/R4). */
    private boolean inClass(Actor actor, Role role, ResourceRef ref) {
        return role == Role.STUDENT
                ? classScope.isEnrolledStudent(actor.accountId(), ref.id())
                : classScope.isTeacherOf(actor.accountId(), ref.id());
    }
}
