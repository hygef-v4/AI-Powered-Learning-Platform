package edu.aiplatform.identity.service;

import edu.aiplatform.academics.contract.IClassAccess;
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
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.model.AccountSearch;
import edu.aiplatform.identity.model.EmailPolicy;
import edu.aiplatform.identity.model.ProfilePolicy;
import edu.aiplatform.identity.model.StudentCodePolicy;
import edu.aiplatform.identity.integration.OtpChallengeStore;
import edu.aiplatform.identity.integration.SessionStore;
import edu.aiplatform.identity.integration.VerificationTicketStore;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<ISubjectQuery> subjectQuery;
    private final ObjectProvider<IClassAccess> classAccess;
    private final SessionStore sessions;
    private final OtpChallengeStore otpChallenges;
    private final VerificationTicketStore tickets;
    private final IAudit audit;
    private final TransactionOperations transactions;
    private final Clock clock;

    public AccountAdminService(AccountRepository accounts, IAuthorization authorization, ISubjectScope subjectScope,
                               IClassScope classScope, ObjectProvider<ISubjectQuery> subjectQuery,
                               ObjectProvider<IClassAccess> classAccess, SessionStore sessions, OtpChallengeStore otpChallenges,
                               VerificationTicketStore tickets, IAudit audit, TransactionOperations transactions, Clock clock) {
        this.accounts = accounts;
        this.authorization = authorization;
        this.subjectScope = subjectScope;
        this.classScope = classScope;
        this.subjectQuery = subjectQuery;
        this.classAccess = classAccess;
        this.sessions = sessions;
        this.otpChallenges = otpChallenges;
        this.tickets = tickets;
        this.audit = audit;
        this.transactions = transactions;
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
        return create(actor, rawEmail, rawDisplayName, role, null);
    }

    /** F9 kèm MSSV: bắt buộc với {@code STUDENT}, không được có với role khác (BR-U01-102, 103). */
    public AccountViews.Detail create(Actor actor, String rawEmail, String rawDisplayName, Role role,
                                      String rawStudentCode) {
        require(actor, Actions.ACCOUNT_CREATE, null);
        String email = EmailPolicy.normalize(rawEmail);
        if (!EmailPolicy.isValid(email)) {
            throw ApiException.badRequest("INVALID_EMAIL", "Email không hợp lệ");
        }
        String displayName = ProfilePolicy.normalizeDisplayName(rawDisplayName);
        ProfilePolicy.checkDisplayName(displayName).ifPresent(code -> {
            throw ApiException.badRequest(code, "Họ và tên cần từ 1 đến 150 ký tự");
        });
        if (role == null) {
            throw ApiException.badRequest("INVALID_ROLE", "Chọn vai trò");
        }
        String studentCode = StudentCodePolicy.normalize(rawStudentCode);
        if (role != Role.STUDENT && studentCode != null) {
            throw studentCodeNotAllowed();
        }
        if (role == Role.STUDENT) {
            requireValidStudentCode(studentCode);
        }
        Instant now = clock.instant();
        Account account = Account.newPending(UUID.randomUUID(), email, role, displayName, null, studentCode, now);
        try {
            transactions.executeWithoutResult(status -> {
                if (accounts.findByEmail(email).isPresent()) {
                    throw emailExists();
                }
                requireStudentCodeFree(studentCode);
                accounts.insert(account);
                record(actor, "ACCOUNT_CREATED", account.id(),
                        Map.of("role", role.name(), "studentCodeSet", studentCode != null), now);
            });
        } catch (DuplicateKeyException e) {
            throw AccountRepository.isStudentCodeConflict(e) ? studentCodeExists() : emailExists();
        }
        return AccountViews.Detail.of(account);
    }

    /** F14 bước 2 (UC 11): chỉ tên, số điện thoại; email không đổi được (BR-U01-04). */
    public AccountViews.Detail update(Actor actor, UUID id, String rawDisplayName, String rawPhoneNumber,
                                     long version) {
        return update(actor, id, rawDisplayName, rawPhoneNumber, null, version);
    }

    /**
     * F14 kèm MSSV (BR-U01-105): {@code rawStudentCode} rỗng ({@code null}) là không đổi mã; chỉ sửa được khi tài
     * khoản còn {@code PENDING}, còn lại mã đã khóa.
     */
    public AccountViews.Detail update(Actor actor, UUID id, String rawDisplayName, String rawPhoneNumber,
                                     String rawStudentCode, long version) {
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
        String studentCode = resolveStudentCode(before, rawStudentCode);
        boolean studentCodeChanged = !Objects.equals(before.studentCode(), studentCode);
        if (studentCodeChanged) {
            changed.add("studentCode");
        }
        Instant now = clock.instant();
        try {
            transactions.executeWithoutResult(status -> {
                if (studentCodeChanged) {
                    requireStudentCodeFree(studentCode);
                }
                if (accounts.updateAccountDetails(id, displayName, phoneNumber, studentCode, version, now) == 0) {
                    throw versionConflict();
                }
                // Ghi tên trường đã đổi, không ghi giá trị số điện thoại hay MSSV (BR-U01-91, 115).
                record(actor, "ACCOUNT_UPDATED", id, Map.of("fields", changed), now);
            });
        } catch (DuplicateKeyException e) {
            throw studentCodeExists();
        }
        return AccountViews.Detail.of(load(id));
    }

    /** BR-U01-105: MSSV cuối cùng sau khi xét yêu cầu; trả mã hiện tại khi không đổi. */
    private static String resolveStudentCode(Account before, String rawStudentCode) {
        if (rawStudentCode == null) {
            return before.studentCode();
        }
        if (before.role() != Role.STUDENT) {
            throw studentCodeNotAllowed();
        }
        String code = StudentCodePolicy.normalize(rawStudentCode);
        if (code == null) {
            if (!before.hasStudentCode()) {
                return null;
            }
            throw invalidStudentCode();
        }
        if (!StudentCodePolicy.isValid(code)) {
            throw invalidStudentCode();
        }
        if (code.equals(before.studentCode())) {
            return code;
        }
        // Chỉ sửa lỗi gõ khi sinh viên chưa kích hoạt; không có chức năng gán mã cho tài khoản cũ (người dùng chốt
        // 2026-11-14).
        if (before.status() != AccountStatus.PENDING) {
            throw ApiException.conflict("STUDENT_CODE_LOCKED",
                    "MSSV đã khóa vì sinh viên đã kích hoạt tài khoản");
        }
        return code;
    }

    private static void requireValidStudentCode(String studentCode) {
        if (studentCode == null) {
            throw ApiException.badRequest("MISSING_STUDENT_CODE", "Cần nhập MSSV cho tài khoản sinh viên");
        }
        if (!StudentCodePolicy.isValid(studentCode)) {
            throw invalidStudentCode();
        }
    }

    /** Kiểm trước để báo lỗi rõ; unique index vẫn chặn khi hai request ghi cùng lúc (BR-U01-101). */
    private void requireStudentCodeFree(String studentCode) {
        if (studentCode != null && !accounts.existingStudentCodes(List.of(studentCode)).isEmpty()) {
            throw studentCodeExists();
        }
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
        if (target.role() == Role.STUDENT || newRole == Role.STUDENT) {
            // BR-U01-107: không đổi role vào hoặc ra khỏi STUDENT; xét trước BR-U01-64…66.
            audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "ROLE_CHANGE_REJECTED", "ACCOUNT",
                    id.toString(), AuditResult.DENIED, "STUDENT_ROLE_CHANGE_NOT_ALLOWED",
                    Map.of("from", target.role().name(), "to", newRole.name()), clock.instant()));
            throw ApiException.conflict("STUDENT_ROLE_CHANGE_NOT_ALLOWED",
                    "Không đổi được vai trò vào hoặc ra khỏi Sinh viên");
        }
        if (id.equals(actor.accountId())) {
            // Người đổi role luôn là ADMIN nên mọi thay đổi role của chính mình đều là tự hạ quyền (BR-U01-65).
            throw ApiException.conflict("SELF_CHANGE_BLOCKED", "Không thể tự đổi vai trò của chính mình");
        }
        Instant now = clock.instant();
        transactions.executeWithoutResult(status -> {
            protectLastAdmin(target, newRole != Role.ADMIN);
            // Khóa tài khoản rồi mới kiểm: U04, U05 gán phân công qua IAccountLookup.lockContact nên không gán xen vào
            // giữa lúc kiểm và lúc đổi role.
            accounts.lockById(id);
            checkAssignmentsKept(id, newRole);
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
            record(actor, disable ? "ACCOUNT_DISABLED" : "ACCOUNT_ENABLED", id,
                    Map.of("status", next.name(), "before", target.status().name()), now);
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
            throw new RoleChangeBlockedException(
                    subjects.stream().map(subjectId -> held(subjectId, () -> subjectQuery.getObject().getSubject(subjectId)
                            .map(s -> new RoleChangeBlockedException.Held(subjectId, s.code(), s.name())))).toList(),
                    classes.stream().map(classId -> held(classId, () -> classAccess.getObject().getClassRef(classId)
                            .map(c -> new RoleChangeBlockedException.Held(classId, c.code(),
                                    c.subjectCode() == null ? null : "Môn " + c.subjectCode())))).toList());
        }
    }

    /** Mã, tên của môn/lớp để Admin nhận ra; unit sở hữu lỗi hay không thấy thì chỉ trả id, vẫn chặn đổi role. */
    private static RoleChangeBlockedException.Held held(UUID id, Supplier<Optional<RoleChangeBlockedException.Held>> lookup) {
        try {
            return lookup.get().orElseGet(() -> new RoleChangeBlockedException.Held(id, null, null));
        } catch (RuntimeException unavailable) {
            return new RoleChangeBlockedException.Held(id, null, null);
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

    private static ApiException studentCodeExists() {
        return ApiException.conflict("STUDENT_CODE_EXISTS", "MSSV này đã được dùng cho tài khoản khác");
    }

    private static ApiException studentCodeNotAllowed() {
        return ApiException.badRequest("STUDENT_CODE_NOT_ALLOWED", "Chỉ tài khoản sinh viên mới có MSSV");
    }

    private static ApiException invalidStudentCode() {
        return ApiException.badRequest("INVALID_STUDENT_CODE", "MSSV gồm 2 chữ cái và 6 chữ số, ví dụ SE123456");
    }

    private static ApiException versionConflict() {
        return ApiException.conflict(VERSION_CONFLICT, "Dữ liệu đã được thay đổi ở phiên khác. Hãy tải lại trước khi sửa.");
    }
}
