package edu.aiplatform.identity.application;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.EmailPolicy;
import edu.aiplatform.identity.domain.ProfilePolicy;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Nhập tài khoản từ CSV (F10, luồng phụ UC 09). Bước kiểm trả kết quả từng dòng, không ghi gì; bước xác nhận gửi lại
 * cùng file, kiểm lại rồi tạo các dòng hợp lệ ở {@code PENDING} trong một transaction, không gửi mail (BR-U01-80–85).
 * Kết quả không lưu; audit ghi checksum file và số dòng tạo/lỗi.
 */
@Service
public class AccountImportService {

    public static final int MAX_ROWS = 1000;
    public static final int MAX_BYTES = 1024 * 1024;
    static final List<String> REQUIRED_COLUMNS = List.of("email", "display_name", "role");
    /** Nhập hàng loạt không được tạo ADMIN (BR-U01-84). */
    static final Set<Role> IMPORTABLE_ROLES = Set.of(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER);

    private final AccountRepository accounts;
    private final IAuthorization authorization;
    private final IAudit audit;
    private final TransactionOperations transactions;
    private final EmailPolicy emailPolicy;
    private final Clock clock;

    public AccountImportService(AccountRepository accounts, IAuthorization authorization, IAudit audit,
                                TransactionOperations transactions, IdentityProperties properties, Clock clock) {
        this.accounts = accounts;
        this.authorization = authorization;
        this.audit = audit;
        this.transactions = transactions;
        this.emailPolicy = new EmailPolicy(properties.allowedEmailDomains());
        this.clock = clock;
    }

    public enum Outcome {
        VALID, CREATED, REJECTED
    }

    public record RowResult(long rowNumber, String email, String displayName, String role, Outcome outcome,
                            String reason) {
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
            // Email vừa được tạo ở request khác giữa lúc kiểm và lúc ghi (hai lần xác nhận cùng tệp): cả tệp rollback.
            throw ApiException.conflict("IMPORT_CONFLICT", "Dữ liệu tài khoản vừa thay đổi, hãy kiểm lại tệp CSV");
        }
    }

    private ImportResult commitChecked(Actor actor, byte[] file, Instant now) {
        return transactions.execute(status -> {
            ImportResult checked = check(file, true);
            for (RowResult row : checked.rows()) {
                if (row.outcome() == Outcome.CREATED) {
                    accounts.insert(Account.newPending(UUID.randomUUID(), row.email(), Role.valueOf(row.role()),
                            row.displayName(), null, now));
                }
            }
            audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "ACCOUNTS_IMPORTED", "ACCOUNT", null,
                    AuditResult.SUCCESS, null, Map.of("checksum", checked.fileChecksum(),
                    "created", checked.validCount(), "rejected", checked.rejectedCount()), now));
            return checked;
        });
    }

    private ImportResult check(byte[] file, boolean committing) {
        if (file == null || file.length == 0) {
            throw invalidFile("Tệp CSV trống");
        }
        if (file.length > MAX_BYTES) {
            throw invalidFile("Tệp CSV vượt 1 MB");
        }
        List<CSVRecord> records = parse(decode(file));
        if (records.size() > MAX_ROWS) {
            throw invalidFile("Tệp CSV có quá " + MAX_ROWS + " dòng dữ liệu");
        }
        List<String> normalizedEmails = records.stream().map(r -> EmailPolicy.normalize(value(r, "email"))).toList();
        Set<String> existing = accounts.existingEmails(normalizedEmails.stream().filter(e -> !e.isEmpty()).toList());
        Set<String> seen = new HashSet<>();
        List<RowResult> rows = new ArrayList<>();
        int valid = 0;
        for (int i = 0; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            String email = normalizedEmails.get(i);
            String displayName = ProfilePolicy.normalizeDisplayName(value(record, "display_name"));
            String roleText = value(record, "role").strip().toUpperCase(Locale.ROOT);
            String reason = reason(email, displayName, roleText, existing, seen);
            if (!email.isEmpty()) {
                seen.add(email);
            }
            Outcome outcome = reason != null ? Outcome.REJECTED : committing ? Outcome.CREATED : Outcome.VALID;
            if (reason == null) {
                valid++;
            }
            // Số dòng trong tệp: dòng tiêu đề là dòng 1.
            rows.add(new RowResult(record.getRecordNumber() + 1, email, displayName, roleText, outcome, reason));
        }
        return new ImportResult(checksum(file), rows, valid, rows.size() - valid, committing);
    }

    private String reason(String email, String displayName, String role, Set<String> existing, Set<String> seen) {
        if (email.isEmpty() || displayName.isEmpty() || role.isEmpty()) {
            return "MISSING_FIELD";
        }
        EmailPolicy.EmailCheck emailCheck = emailPolicy.check(email);
        if (emailCheck != EmailPolicy.EmailCheck.OK) {
            return emailCheck.name();
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

    /** Dòng thiếu cột (ít dấu phẩy hơn tiêu đề) coi như ô trống thay vì lỗi cả tệp. */
    private static String value(CSVRecord record, String column) {
        return record.isMapped(column) && record.isSet(column) ? record.get(column) : "";
    }

    private static String decode(byte[] file) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file)).toString();
            // Excel thường lưu UTF-8 kèm BOM.
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            throw invalidFile("Tệp CSV phải mã hóa UTF-8");
        }
    }

    private static List<CSVRecord> parse(String text) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setIgnoreHeaderCase(true)
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            List<String> header = parser.getHeaderNames().stream()
                    .map(name -> name.strip().toLowerCase(Locale.ROOT)).toList();
            if (!header.containsAll(REQUIRED_COLUMNS)) {
                throw invalidFile("Tệp CSV cần đủ cột email, display_name, role");
            }
            List<CSVRecord> records = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (records.size() > MAX_ROWS) {
                    break;
                }
                records.add(record);
            }
            return records;
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw invalidFile("Không đọc được tệp CSV");
        }
    }

    private static String checksum(byte[] file) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    private void require(Actor actor) {
        if (actor == null || !authorization.authorize(actor, Actions.ACCOUNT_IMPORT, ResourceRef.none()).allowed()) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                    "ACCOUNT", null, AuditResult.DENIED, Actions.ACCOUNT_IMPORT, Map.of(), clock.instant()));
            throw ApiException.forbidden("ACCOUNT_ADMIN_FORBIDDEN");
        }
    }

    private static ApiException invalidFile(String message) {
        return ApiException.badRequest("IMPORT_FILE_INVALID", message);
    }
}
