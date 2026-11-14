package edu.aiplatform.identity.service;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.model.EmailPolicy;
import edu.aiplatform.identity.model.ProfilePolicy;
import edu.aiplatform.identity.model.StudentCodePolicy;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.service.ImportTableReader.TableRow;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Nhập tài khoản từ Excel, tương thích CSV cũ (F10, luồng phụ UC 09). Bước kiểm không ghi; xác nhận gửi lại
 * cùng file, kiểm lại rồi tạo các dòng hợp lệ ở {@code PENDING} trong một transaction, không gửi mail (BR-U01-80–85).
 * Cột tùy chọn {@code student_code} bắt buộc có giá trị ở dòng STUDENT (BR-U01-108). Kết quả không lưu; audit ghi
 * checksum file và số dòng tạo/lỗi.
 */
@Service
public class AccountImportService {

    public static final int MAX_ROWS = 1000;
    public static final int MAX_BYTES = 1024 * 1024;
    static final List<String> REQUIRED_COLUMNS = List.of("email", "display_name", "role");
    static final String STUDENT_CODE_COLUMN = "student_code";
    /** Nhập hàng loạt không được tạo ADMIN (BR-U01-84). */
    static final Set<Role> IMPORTABLE_ROLES = Set.of(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER);

    private final AccountRepository accounts;
    private final IAuthorization authorization;
    private final IAudit audit;
    private final TransactionOperations transactions;
    private final Clock clock;

    public AccountImportService(AccountRepository accounts, IAuthorization authorization, IAudit audit,
                                TransactionOperations transactions, Clock clock) {
        this.accounts = accounts;
        this.authorization = authorization;
        this.audit = audit;
        this.transactions = transactions;
        this.clock = clock;
    }

    public enum Outcome {
        VALID, CREATED, REJECTED
    }

    public record RowResult(long rowNumber, String email, String displayName, String role, Outcome outcome,
                            String reason, String studentCode) {
    }

    public record ImportResult(String fileChecksum, List<RowResult> rows, int validCount, int rejectedCount,
                               boolean committed) {
    }

    /** Bước kiểm: không ghi database, không audit. */
    public ImportResult validate(Actor actor, byte[] file) {
        require(actor);
        return check(file, false);
    }

    /** Bước xác nhận: kiểm lại toàn bộ (dữ liệu có thể đã đổi) rồi chỉ tạo dòng hợp lệ (BR-U01-81, 83). */
    public ImportResult commit(Actor actor, byte[] file) {
        require(actor);
        Instant now = clock.instant();
        try {
            return commitChecked(actor, file, now);
        } catch (DuplicateKeyException e) {
            // Email hoặc MSSV vừa được dùng ở request khác giữa lúc kiểm và lúc ghi: cả tệp rollback.
            throw ApiException.conflict("IMPORT_CONFLICT", "Dữ liệu tài khoản vừa thay đổi, hãy kiểm lại tệp nhập");
        }
    }

    private ImportResult commitChecked(Actor actor, byte[] file, Instant now) {
        return transactions.execute(status -> {
            ImportResult checked = check(file, true);
            for (RowResult row : checked.rows()) {
                if (row.outcome() == Outcome.CREATED) {
                    accounts.insert(Account.newPending(UUID.randomUUID(), row.email(), Role.valueOf(row.role()),
                            row.displayName(), null, row.studentCode(), now));
                }
            }
            audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "ACCOUNTS_IMPORTED", "ACCOUNT", null,
                    AuditResult.SUCCESS, null, Map.of("checksum", checked.fileChecksum(),
                    "created", checked.validCount(), "rejected", checked.rejectedCount()), now));
            return checked;
        });
    }

    private ImportResult check(byte[] file, boolean committing) {
        List<TableRow> records = ImportTableReader.readRows(file, REQUIRED_COLUMNS, List.of(STUDENT_CODE_COLUMN),
                MAX_BYTES, MAX_ROWS);
        List<String> normalizedEmails = records.stream().map(r -> EmailPolicy.normalize(r.value("email"))).toList();
        List<String> normalizedCodes = records.stream()
                .map(r -> StudentCodePolicy.normalize(r.value(STUDENT_CODE_COLUMN))).toList();
        Set<String> existing = accounts.existingEmails(normalizedEmails.stream().filter(e -> !e.isEmpty()).toList());
        Set<String> existingCodes = accounts.existingStudentCodes(normalizedCodes.stream()
                .filter(StudentCodePolicy::isValid).distinct().toList());
        Set<String> seen = new HashSet<>();
        Set<String> seenCodes = new HashSet<>();
        List<RowResult> rows = new ArrayList<>();
        int valid = 0;
        for (int i = 0; i < records.size(); i++) {
            TableRow record = records.get(i);
            String email = normalizedEmails.get(i);
            String studentCode = normalizedCodes.get(i);
            String displayName = ProfilePolicy.normalizeDisplayName(record.value("display_name"));
            String roleText = record.value("role").strip().toUpperCase(Locale.ROOT);
            String reason = reason(email, displayName, roleText, existing, seen);
            if (reason == null) {
                reason = studentCodeReason(roleText, studentCode, existingCodes, seenCodes);
            }
            if (!email.isEmpty()) {
                seen.add(email);
            }
            if (StudentCodePolicy.isValid(studentCode)) {
                seenCodes.add(studentCode);
            }
            Outcome outcome = reason != null ? Outcome.REJECTED : committing ? Outcome.CREATED : Outcome.VALID;
            if (reason == null) {
                valid++;
            }
            // Số dòng trong tệp: dòng tiêu đề là dòng 1.
            rows.add(new RowResult(record.rowNumber(), email, displayName, roleText, outcome, reason, studentCode));
        }
        return new ImportResult(ImportTableReader.checksum(file), rows, valid, rows.size() - valid, committing);
    }

    private String reason(String email, String displayName, String role, Set<String> existing, Set<String> seen) {
        if (email.isEmpty() || displayName.isEmpty() || role.isEmpty()) {
            return "MISSING_FIELD";
        }
        if (!EmailPolicy.isValid(email)) {
            return "INVALID_EMAIL";
        }
        if (seen.contains(email)) {
            return "DUPLICATE_IN_FILE";
        }
        if (existing.contains(email)) {
            return "EMAIL_EXISTS";
        }
        if (ProfilePolicy.checkDisplayName(displayName).isPresent()) {
            return "INVALID_DISPLAY_NAME";
        }
        try {
            return IMPORTABLE_ROLES.contains(Role.valueOf(role)) ? null : "INVALID_ROLE";
        } catch (IllegalArgumentException e) {
            return "INVALID_ROLE";
        }
    }

    /** BR-U01-108: xét sau các lỗi email, tên, role; {@code role} đã hợp lệ. */
    private static String studentCodeReason(String role, String studentCode, Set<String> existingCodes,
                                            Set<String> seenCodes) {
        boolean student = Objects.equals(role, Role.STUDENT.name());
        if (student && studentCode == null) {
            return "MISSING_STUDENT_CODE";
        }
        if (!student) {
            return studentCode == null ? null : "STUDENT_CODE_NOT_ALLOWED";
        }
        if (!StudentCodePolicy.isValid(studentCode)) {
            return "INVALID_STUDENT_CODE";
        }
        if (seenCodes.contains(studentCode)) {
            return "DUPLICATE_STUDENT_CODE_IN_FILE";
        }
        return existingCodes.contains(studentCode) ? "STUDENT_CODE_EXISTS" : null;
    }

    private void require(Actor actor) {
        AuthorizationDecision decision = actor == null ? AuthorizationDecision.deny("NO_ACTOR")
                : authorization.authorize(actor, Actions.ACCOUNT_IMPORT, ResourceRef.none());
        AuthorizationService.throwIfUnavailable(decision);
        if (!decision.allowed()) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                    "ACCOUNT", null, AuditResult.DENIED, Actions.ACCOUNT_IMPORT, Map.of(), clock.instant()));
            throw ApiException.forbidden("ACCOUNT_ADMIN_FORBIDDEN");
        }
    }
}
