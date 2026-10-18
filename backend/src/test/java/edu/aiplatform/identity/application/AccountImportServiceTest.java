package edu.aiplatform.identity.application;

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
import edu.aiplatform.identity.IdentityTestData;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.application.AccountImportService.Outcome;
import edu.aiplatform.identity.application.AccountImportService.RowResult;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

class AccountImportServiceTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private final IAudit audit = audits::add;
    private final AccountImportService service = new AccountImportService(accounts, authorization, audit,
            TransactionOperations.withoutTransaction(), IdentityTestData.properties(),
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);

    private static final String CSV = """
            ﻿email,display_name,role
            sv01@fpt.edu.vn,"Nguyễn Văn A, K17",student
            sv02@gmail.com,Ngoài trường,STUDENT
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
                        org.assertj.core.groups.Tuple.tuple(3L, Outcome.REJECTED, "EMAIL_DOMAIN_NOT_ALLOWED"),
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
        StringBuilder csv = new StringBuilder("email,display_name,role\n");
        for (int i = 0; i < AccountImportService.MAX_ROWS; i++) {
            csv.append("sv").append(i).append("@fpt.edu.vn,SV ").append(i).append(",STUDENT\n");
        }
        assertThat(service.validate(admin, bytes(csv.toString())).validCount()).isEqualTo(1000);
    }
}
