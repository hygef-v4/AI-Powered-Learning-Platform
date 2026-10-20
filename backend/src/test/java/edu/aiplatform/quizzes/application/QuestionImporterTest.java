package edu.aiplatform.quizzes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.quizzes.domain.IQuestionRepository;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuestionStatus;
import edu.aiplatform.quizzes.domain.Violation;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/** F3 của U10: nhập tệp (BR-U10-07, P5, NFR-U10-30). */
class QuestionImporterTest {

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");
    private static final String HEADER =
            "noi_dung,loai,lua_chon_1,lua_chon_2,lua_chon_3,lua_chon_4,lua_chon_5,lua_chon_6,dap_an_dung,giai_thich,diem,do_kho,tag";
    private final UUID subject = UUID.randomUUID();
    private final Actor manager = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    private final IQuestionRepository repo = mock(IQuestionRepository.class);
    private final ISubjectQuery subjects = mock(ISubjectQuery.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final List<AuditEvent> audits = new ArrayList<>();
    private QuestionImporter importer;

    @BeforeEach
    void setup() {
        QuizAudit audit = new QuizAudit(audits::add, Clock.fixed(NOW, ZoneOffset.UTC));
        importer = new QuestionImporter(repo, new ActiveSubjects(subjects), new QuizAccess(authorization, audit), audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(subjects.isActive(subject)).thenReturn(true);
    }

    private static byte[] csv(String... lines) {
        return String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
    }

    private static ApiException api(Runnable call) {
        try {
            call.run();
        } catch (ApiException failure) {
            return failure;
        }
        throw new AssertionError("Không ném ApiException");
    }

    @Test
    void validRowsBecomeDraftsAndInvalidRowsReportReasons() {
        byte[] file = csv(HEADER,
                "Thủ đô Việt Nam?,MCQ_SINGLE,Hà Nội,Huế,,,,,1,Giải thích,\"1,5\",EASY,địa lý;thủ đô",
                "Số chẵn?,Nhiều đáp án,2,3,4,,,,\"1, 3\",,,Khó,",
                "Thiếu lựa chọn,MCQ_SINGLE,Chỉ một,,,,,,1,,,,",
                "Tự luận?,ESSAY,A,B,,,,,1,,,,");

        var result = importer.importFile(manager, subject, file, false);

        assertThat(result.total()).isEqualTo(4);
        assertThat(result.valid()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(2);
        assertThat(result.rows()).extracting(QuestionImporter.RowResult::row).containsExactly(2, 3, 4, 5);
        assertThat(result.rows().get(2).reasons()).extracting(Violation::code).contains("OPTION_COUNT");
        assertThat(result.rows().get(3).reasons()).extracting(Violation::code).contains("TYPE_UNSUPPORTED");

        ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
        verify(repo, times(2)).insert(saved.capture());
        Question first = saved.getAllValues().get(0);
        assertThat(first.status()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(first.content().defaultPoints()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(first.content().tags()).containsExactly("địa lý", "thủ đô");
        Question second = saved.getAllValues().get(1);
        assertThat(second.content().type()).isEqualTo(QuestionType.MCQ_MULTI);
        assertThat(second.content().correctOptionIds()).containsExactly("1", "3");
        assertThat(second.content().difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(audits).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("QUESTIONS_IMPORTED");
            assertThat(e.details()).containsEntry("created", 2).containsEntry("failed", 2);
        });
    }

    @Test
    void dryRunOnlyChecksWithoutWritingOrAuditing() {
        var result = importer.importFile(manager, subject, csv(HEADER, "Câu?,MCQ_SINGLE,A,B,,,,,1,,,,"), true);
        assertThat(result.dryRun()).isTrue();
        assertThat(result.valid()).isEqualTo(1);
        assertThat(result.created()).isZero();
        verify(repo, never()).insert(any());
        assertThat(audits).isEmpty();
    }

    @Test
    void formulaLikeCellsStayPlainText() {
        importer.importFile(manager, subject, csv(HEADER, "=1+1,MCQ_SINGLE,@SUM(A1),-5,,,,,2,,,,"), false);
        ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
        verify(repo).insert(saved.capture());
        assertThat(saved.getValue().content().stem()).isEqualTo("=1+1");
        assertThat(saved.getValue().content().options()).extracting(o -> o.text()).containsExactly("@SUM(A1)", "-5");
    }

    @Test
    void semicolonSeparatedCsvWithBomIsAccepted() {
        byte[] file = ("﻿" + HEADER.replace(',', ';') + "\nCâu?;MCQ_SINGLE;A;B;;;;;1;;;;")
                .getBytes(StandardCharsets.UTF_8);
        assertThat(importer.importFile(manager, subject, file, true).valid()).isEqualTo(1);
    }

    @Test
    void rejectsMissingColumnsTooManyRowsAndNonUtf8Files() {
        assertThat(api(() -> importer.importFile(manager, subject, csv("noi_dung,loai", "Câu,MCQ_SINGLE"), true)).code())
                .isEqualTo("IMPORT_HEADER_INVALID");

        List<String> lines = new ArrayList<>(List.of(HEADER));
        for (int i = 0; i < 501; i++) {
            lines.add("Câu " + i + ",MCQ_SINGLE,A,B,,,,,1,,,,");
        }
        assertThat(api(() -> importer.importFile(manager, subject, csv(lines.toArray(String[]::new)), true)).code())
                .isEqualTo("IMPORT_TOO_MANY_ROWS");

        ApiException binary = api(() -> importer.importFile(manager, subject, new byte[] {(byte) 0xC3, 0x28, 0x00}, true));
        assertThat(binary.status()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        assertThat(api(() -> importer.importFile(manager, subject, new byte[0], true)).code()).isEqualTo("IMPORT_FILE_EMPTY");
        assertThat(api(() -> importer.importFile(manager, subject, csv(HEADER), true)).code()).isEqualTo("IMPORT_FILE_EMPTY");
        assertThat(api(() -> importer.importFile(manager, subject,
                new byte[(int) QuestionImporter.MAX_BYTES + 1], true)).status()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void readsFirstSheetOfXlsxWithoutEvaluatingFormulas() throws IOException {
        byte[] file;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Câu hỏi");
            String[] header = HEADER.split(",");
            Row head = sheet.createRow(0);
            for (int i = 0; i < header.length; i++) {
                head.createCell(i).setCellValue(header[i]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Kết quả của phép tính?");
            row.createCell(1).setCellValue("MCQ_SINGLE");
            row.createCell(2).setCellFormula("1+1");
            row.createCell(3).setCellValue(3);
            row.createCell(8).setCellValue(2);
            workbook.write(out);
            file = out.toByteArray();
        }

        var result = importer.importFile(manager, subject, file, false);

        assertThat(result.valid()).isEqualTo(1);
        ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
        verify(repo).insert(saved.capture());
        assertThat(saved.getValue().content().options()).extracting(o -> o.text()).containsExactly("1+1", "3");
        assertThat(saved.getValue().content().correctOptionIds()).containsExactly("2");
    }

    @Test
    void archivedSubjectCannotImport() {
        when(subjects.isActive(subject)).thenReturn(false);
        assertThat(api(() -> importer.importFile(manager, subject, csv(HEADER, "Câu?,MCQ_SINGLE,A,B,,,,,1,,,,"), true))
                .code()).isEqualTo("SUBJECT_NOT_ACTIVE");
    }
}
