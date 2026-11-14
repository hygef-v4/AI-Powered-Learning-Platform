package edu.aiplatform.identity.service;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.repository.AccountRepository;
import edu.aiplatform.identity.service.AccountImportService.Outcome;
import edu.aiplatform.identity.service.AccountImportService.RowResult;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.transaction.support.TransactionOperations;

class AccountImportServiceTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private final IAudit audit = audits::add;
    private final AccountImportService service = new AccountImportService(accounts, authorization, audit,
            TransactionOperations.withoutTransaction(),
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);

    private static final String CSV = """
            ﻿email,display_name,role,student_code
            sv01@fpt.edu.vn,"Nguyễn Văn A, K17",student,se000001
            sv02@,Sai định dạng,STUDENT
            sv01@fpt.edu.vn,Trùng trong tệp,STUDENT
            gv01@fpt.edu.vn,Đã có,TEACHER
            ad01@fpt.edu.vn,Admin,ADMIN
            sv03@fpt.edu.vn,,STUDENT
            sv04@fpt.edu.vn,Thiếu cột
            """;

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(accounts.existingEmails(anyCollection())).thenReturn(Set.of("gv01@fpt.edu.vn"));
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void validateReportsEveryRowWithoutWriting() {
        AccountImportService.ImportResult result = service.validate(admin, bytes(CSV));

        assertThat(result.rows()).extracting(RowResult::rowNumber, RowResult::outcome, RowResult::reason)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, Outcome.VALID, null),
                        org.assertj.core.groups.Tuple.tuple(3L, Outcome.REJECTED, "INVALID_EMAIL"),
                        org.assertj.core.groups.Tuple.tuple(4L, Outcome.REJECTED, "DUPLICATE_IN_FILE"),
                        org.assertj.core.groups.Tuple.tuple(5L, Outcome.REJECTED, "EMAIL_EXISTS"),
                        org.assertj.core.groups.Tuple.tuple(6L, Outcome.REJECTED, "INVALID_ROLE"),
                        org.assertj.core.groups.Tuple.tuple(7L, Outcome.REJECTED, "MISSING_FIELD"),
                        org.assertj.core.groups.Tuple.tuple(8L, Outcome.REJECTED, "MISSING_FIELD"));
        assertThat(result.rows().get(0).displayName()).isEqualTo("Nguyễn Văn A, K17");
        assertThat(result.validCount()).isEqualTo(1);
        verify(accounts, never()).insert(any());
        assertThat(audits).isEmpty();
    }

    /** BR-U01-03: không giới hạn tên miền, chỉ kiểm định dạng. */
    @Test
    void acceptsEmailFromAnyDomain() {
        String csv = """
                email,display_name,role,student_code
                sv@gmail.com,Gmail,STUDENT,SE000002
                gv@school.example.org,Trường khác,TEACHER
                """;

        AccountImportService.ImportResult result = service.validate(admin, bytes(csv));

        assertThat(result.rows()).extracting(RowResult::outcome).containsOnly(Outcome.VALID);
        assertThat(result.validCount()).isEqualTo(2);
    }

    /** Hai lần xác nhận cùng tệp chạy song song: bên ghi sau đụng unique key, trả 409 thay vì 500. */
    @Test
    void concurrentCommitOfSameEmailIsConflict() {
        org.mockito.Mockito.doThrow(new org.springframework.dao.DuplicateKeyException("accounts_school_email_key"))
                .when(accounts).insert(any());

        assertThatThrownBy(() -> service.commit(admin, bytes(CSV)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
                    assertThat(e.code()).isEqualTo("IMPORT_CONFLICT");
                });
    }

    @Test
    void commitCreatesOnlyValidRowsAsPendingAndAuditsChecksum() {
        AccountImportService.ImportResult result = service.commit(admin, bytes(CSV));

        verify(accounts, times(1)).insert(org.mockito.ArgumentMatchers.argThat(
                a -> a.schoolEmail().equals("sv01@fpt.edu.vn") && a.role() == Role.STUDENT && a.passwordHash() == null));
        assertThat(result.rows().get(0).outcome()).isEqualTo(Outcome.CREATED);
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("ACCOUNTS_IMPORTED");
            assertThat(e.details()).containsEntry("checksum", result.fileChecksum())
                    .containsEntry("created", 1).containsEntry("rejected", 6);
        });
    }

    @Test
    void fileProblemsAreRejectedAsAWhole() {
        assertThatThrownBy(() -> service.validate(admin, bytes("mail,name\nx,y\n")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("IMPORT_FILE_INVALID"));
        assertThatThrownBy(() -> service.validate(admin, new byte[0])).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.validate(admin, new byte[] {(byte) 0xC3, (byte) 0x28}))
                .isInstanceOf(ApiException.class);

        StringBuilder big = new StringBuilder("email,display_name,role\n");
        for (int i = 0; i <= AccountImportService.MAX_ROWS; i++) {
            big.append("sv").append(i).append("@fpt.edu.vn,SV ").append(i).append(",STUDENT\n");
        }
        assertThatThrownBy(() -> service.validate(admin, bytes(big.toString())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getMessage()).contains("1000"));
    }

    @Test
    void thousandRowsAreAccepted() {
        StringBuilder csv = new StringBuilder("email,display_name,role,student_code\n");
        for (int i = 0; i < AccountImportService.MAX_ROWS; i++) {
            csv.append("sv").append(i).append("@fpt.edu.vn,SV ").append(i).append(",STUDENT,")
                    .append(String.format("SE%06d", i)).append('\n');
        }
        assertThat(service.validate(admin, bytes(csv.toString())).validCount()).isEqualTo(1000);
    }

    private static byte[] workbook(String[]... rows) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Tài khoản");
            for (int i = 0; i < rows.length; i++) {
                var row = sheet.createRow(i);
                for (int j = 0; j < rows[i].length; j++) {
                    row.createCell(j).setCellValue(rows[i][j]);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    @Test
    void validatesExcelUsingTheSameRulesAndCommitsOnlyValidRows() throws IOException {
        byte[] file = workbook(new String[] {"role", "display_name", "email", "student_code"},
                new String[] {"student", "Nguyễn Văn A", "sv01@fpt.edu.vn", "se000001"},
                new String[] {"STUDENT", "Trùng", "sv01@fpt.edu.vn", "SE000002"},
                new String[] {"ADMIN", "Admin", "admin@fpt.edu.vn", ""},
                new String[] {"TEACHER", "Đã có", "gv01@fpt.edu.vn", ""},
                new String[] {"STUDENT", "Sai", "bad@", "SE000003"});
        var preview = service.validate(admin, file);
        assertThat(preview.validCount()).isEqualTo(1);
        assertThat(preview.rows()).extracting(RowResult::reason)
                .containsExactly(null, "DUPLICATE_IN_FILE", "INVALID_ROLE", "EMAIL_EXISTS", "INVALID_EMAIL");
        assertThat(preview.rows().get(0).rowNumber()).isEqualTo(2);
        assertThat(preview.rows().get(0).displayName()).isEqualTo("Nguyễn Văn A");
        verify(accounts, never()).insert(any());
        assertThat(audits).isEmpty();

        assertThat(service.commit(admin, file).validCount()).isEqualTo(1);
        verify(accounts).insert(org.mockito.ArgumentMatchers.argThat(
                account -> account.schoolEmail().equals("sv01@fpt.edu.vn")
                        && account.role() == Role.STUDENT && account.passwordHash() == null));
        assertThat(audits).singleElement().satisfies(event ->
                assertThat(event.details()).containsEntry("checksum", preview.fileChecksum()));
    }

    @Test
    void rejectsExcelWithMissingHeadersAndTooManyRows() throws IOException {
        byte[] missing = workbook(new String[] {"email", "display_name"}, new String[] {"sv@fpt.edu.vn", "SV"});
        assertThatThrownBy(() -> service.validate(admin, missing)).isInstanceOf(ApiException.class);
        String[][] rows = new String[AccountImportService.MAX_ROWS + 2][];
        rows[0] = new String[] {"email", "display_name", "role", "student_code"};
        for (int i = 1; i < rows.length; i++) {
            rows[i] = new String[] {"sv" + i + "@fpt.edu.vn", "SV " + i, "STUDENT", String.format("SE%06d", i)};
        }
        byte[] tooMany = workbook(rows);
        assertThatThrownBy(() -> service.validate(admin, tooMany))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getMessage()).contains("1000"));
        assertThat(service.validate(admin, workbook(java.util.Arrays.copyOf(rows, rows.length - 1))).validCount())
                .isEqualTo(1000);
    }

    @Test
    void deniesUnauthorizedExcelImportBeforeReadingTheWorkbook() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("ROLE"));
        assertThatThrownBy(() -> service.validate(new Actor(UUID.randomUUID(), Role.STUDENT), new byte[] {1}))
                .isInstanceOfSatisfying(ApiException.class, error ->
                        assertThat(error.status()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN));
        verify(accounts, never()).existingEmails(anyCollection());
    }

    @Test
    void rejectsCorruptOversizedAndFormulaWorkbooks() throws IOException {
        assertThatThrownBy(() -> service.validate(admin, new byte[] {'P', 'K', 3, 4, 0}))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("IMPORT_FILE_INVALID"));
        assertThatThrownBy(() -> service.validate(admin, new byte[AccountImportService.MAX_BYTES + 1]))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getMessage()).contains("1 MB"));
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Tài khoản");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("email");
            header.createCell(1).setCellValue("display_name");
            header.createCell(2).setCellValue("role");
            var row = sheet.createRow(1);
            row.createCell(0).setCellFormula("\"sv@fpt.edu.vn\"");
            row.createCell(1).setCellValue("SV");
            row.createCell(2).setCellValue("STUDENT");
            workbook.write(output);
            assertThatThrownBy(() -> service.validate(admin, output.toByteArray()))
                    .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getMessage()).contains("công thức"));
        }
        verify(accounts, never()).insert(any());
    }
    /** BR-U01-108: MSSV bắt buộc với STUDENT, cấm với role khác, kiểm định dạng và trùng sau các lỗi hiện có. */
    @Test
    void studentCodeColumnIsRequiredForStudentsAndCheckedForDuplicates() {
        when(accounts.existingStudentCodes(anyCollection())).thenReturn(Set.of("SE999999"));
        String csv = """
                email,display_name,role,student_code
                a@fpt.edu.vn,A,STUDENT,se000001
                b@fpt.edu.vn,B,STUDENT,
                c@fpt.edu.vn,C,TEACHER,SE000003
                d@fpt.edu.vn,D,STUDENT,SE-00004
                e@fpt.edu.vn,E,STUDENT,SE000001
                f@fpt.edu.vn,F,STUDENT,SE999999
                g@fpt.edu.vn,G,TEACHER,
                bad@,H,STUDENT,
                """;

        var result = service.validate(admin, bytes(csv));

        assertThat(result.rows()).extracting(RowResult::reason).containsExactly(null, "MISSING_STUDENT_CODE",
                "STUDENT_CODE_NOT_ALLOWED", "INVALID_STUDENT_CODE", "DUPLICATE_STUDENT_CODE_IN_FILE",
                "STUDENT_CODE_EXISTS", null, "INVALID_EMAIL");
        assertThat(result.rows().get(0).studentCode()).isEqualTo("SE000001");
    }

    @Test
    void fileWithoutStudentCodeColumnRejectsOnlyStudentRows() {
        var result = service.validate(admin, bytes("email,display_name,role\na@fpt.edu.vn,A,STUDENT\n"
                + "g@fpt.edu.vn,G,TEACHER\n"));

        assertThat(result.rows()).extracting(RowResult::reason).containsExactly("MISSING_STUDENT_CODE", null);
    }

    @Test
    void commitStoresNormalizedStudentCode() {
        service.commit(admin, bytes("email,display_name,role,student_code\na@fpt.edu.vn,A,STUDENT, se000001 \n"));

        verify(accounts).insert(org.mockito.ArgumentMatchers.argThat(
                account -> "SE000001".equals(account.studentCode()) && account.role() == Role.STUDENT));
    }
}
