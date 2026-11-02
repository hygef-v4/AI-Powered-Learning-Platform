package edu.aiplatform.quizzes.application;

import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.quizzes.domain.IQuestionRepository;
import edu.aiplatform.quizzes.domain.Question;
import edu.aiplatform.quizzes.domain.QuestionContent;
import edu.aiplatform.quizzes.domain.QuestionOption;
import edu.aiplatform.quizzes.domain.QuestionRules;
import edu.aiplatform.quizzes.domain.Violation;
import edu.aiplatform.shared.error.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhập câu ngân hàng từ tệp (UC 47, F3, P5, BR-U10-07, NFR-U10-30). Loại tệp nhận theo nội dung: ZIP là
 * {@code .xlsx}, văn bản UTF-8 là {@code .csv}. Đọc trong bộ nhớ, không tính công thức: ô bắt đầu bằng {@code =},
 * {@code +}, {@code -}, {@code @} giữ nguyên là văn bản. Mỗi dòng kiểm như tạo tay; dòng hợp lệ thành câu
 * {@code DRAFT}, ghi trong một transaction; {@code dryRun} chỉ kiểm (D3).
 */
@Service
public class QuestionImporter {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final int MAX_ROWS = 500;
    static final List<String> REQUIRED_COLUMNS = List.of("noi_dung", "loai", "lua_chon_1", "lua_chon_2", "dap_an_dung");
    private static final int OPTION_COLUMNS = 6;
    private static final int STEM_PREVIEW = 120;
    private static final BigDecimal DEFAULT_POINTS = BigDecimal.ONE;
    /** Khóa nội bộ giữ số dòng trong tệp (dòng tiêu đề là 1) để trả kết quả từng dòng. */
    private static final String ROW_NUMBER = "__row";

    private final IQuestionRepository questions;
    private final ActiveSubjects activeSubjects;
    private final QuizAccess access;
    private final QuizAudit audit;
    private final Clock clock;

    public QuestionImporter(IQuestionRepository questions, ActiveSubjects activeSubjects, QuizAccess access, QuizAudit audit,
                            Clock clock) {
        this.questions = questions;
        this.activeSubjects = activeSubjects;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    public record RowResult(int row, String stem, int optionCount, boolean valid, List<Violation> reasons) {
    }

    public record ImportResult(boolean dryRun, int total, int valid, int invalid, int created, List<RowResult> rows) {
    }

    @Transactional
    public ImportResult importFile(Actor actor, UUID subjectId, byte[] bytes, boolean dryRun) {
        access.requireSubject(actor, Actions.QUESTION_BANK_MANAGE, subjectId, "SUBJECT", subjectId, "SUBJECT_NOT_FOUND");
        activeSubjects.require(subjectId);
        if (bytes == null || bytes.length == 0) {
            throw ApiException.badRequest("IMPORT_FILE_EMPTY", "Tệp rỗng");
        }
        if (bytes.length > MAX_BYTES) {
            throw ApiException.payloadTooLarge("IMPORT_FILE_TOO_LARGE", "Tệp tối đa 5 MB");
        }
        List<Map<String, String>> rows = isZip(bytes) ? readXlsx(bytes) : readCsv(bytes);
        if (rows.isEmpty()) {
            throw ApiException.badRequest("IMPORT_FILE_EMPTY", "Tệp không có dòng câu hỏi nào");
        }
        if (rows.size() > MAX_ROWS) {
            throw ApiException.badRequest("IMPORT_TOO_MANY_ROWS", "Tối đa 500 dòng mỗi lần nhập");
        }

        Instant now = clock.instant();
        List<RowResult> results = new ArrayList<>();
        List<Question> valid = new ArrayList<>();
        for (Map<String, String> row : rows) {
            int rowNumber = Integer.parseInt(row.get(ROW_NUMBER));
            List<Violation> reasons = new ArrayList<>();
            QuestionContent content = toContent(row, reasons);
            reasons.addAll(QuestionRules.validate(content));
            boolean ok = reasons.isEmpty();
            results.add(new RowResult(rowNumber, preview(content.stem()), content.options().size(), ok,
                    List.copyOf(reasons)));
            if (ok) {
                valid.add(Question.newBankDraft(subjectId, content, actor.accountId(), now));
            }
        }
        if (!dryRun) {
            valid.forEach(questions::insert);
            audit.success(actor, "QUESTIONS_IMPORTED", "SUBJECT", subjectId, Map.of(
                    "total", rows.size(), "created", valid.size(), "failed", rows.size() - valid.size()));
        }
        return new ImportResult(dryRun, rows.size(), valid.size(), rows.size() - valid.size(),
                dryRun ? 0 : valid.size(), results);
    }


    static QuestionContent toContent(Map<String, String> row, List<Violation> reasons) {
        QuestionType type = type(row.get("loai"), reasons);
        List<QuestionOption> options = new ArrayList<>();
        for (int i = 1; i <= OPTION_COLUMNS; i++) {
            String text = row.get("lua_chon_" + i);
            if (text != null && !text.isBlank()) {
                options.add(new QuestionOption(String.valueOf(i), text));
            }
        }
        String answers = row.getOrDefault("dap_an_dung", "");
        List<String> correct = answers.isBlank() ? List.of() : List.of(answers.strip().split("\\s*[,;\\s]\\s*"));
        return QuestionContent.of(type, row.get("noi_dung"), options, correct, row.get("giai_thich"),
                points(row.get("diem"), reasons), difficulty(row.get("do_kho"), reasons), tags(row.get("tag")));
    }

    private static QuestionType type(String value, List<Violation> reasons) {
        String key = key(value);
        return switch (key) {
            case "MCQ_SINGLE", "SINGLE", "MOT_DAP_AN" -> QuestionType.MCQ_SINGLE;
            case "MCQ_MULTI", "MULTI", "NHIEU_DAP_AN" -> QuestionType.MCQ_MULTI;
            default -> {
                reasons.add(new Violation("type", "TYPE_UNSUPPORTED",
                        key.isEmpty() ? "Thiếu loại câu" : "Loại câu không hỗ trợ (chỉ MCQ_SINGLE, MCQ_MULTI)"));
                yield null;
            }
        };
    }

    private static Difficulty difficulty(String value, List<Violation> reasons) {
        String key = key(value);
        return switch (key) {
            case "" -> Difficulty.MEDIUM;
            case "EASY", "DE" -> Difficulty.EASY;
            case "MEDIUM", "TRUNG_BINH" -> Difficulty.MEDIUM;
            case "HARD", "KHO" -> Difficulty.HARD;
            default -> {
                reasons.add(new Violation("difficulty", "DIFFICULTY_INVALID", "Độ khó chỉ nhận EASY, MEDIUM, HARD"));
                yield Difficulty.MEDIUM;
            }
        };
    }

    private static BigDecimal points(String value, List<Violation> reasons) {
        if (value == null || value.isBlank()) {
            return DEFAULT_POINTS;
        }
        try {
            return new BigDecimal(value.strip().replace(',', '.'));
        } catch (NumberFormatException invalid) {
            reasons.add(new Violation("defaultPoints", "POINTS_INVALID", "Điểm phải là số lớn hơn 0"));
            return DEFAULT_POINTS;
        }
    }

    private static List<String> tags(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.split("[;|]"));
    }

    /** Bỏ dấu, viết hoa, khoảng trắng thành "_" để nhận cả "Một đáp án", "trung bình". */
    private static String key(String value) {
        if (value == null) {
            return "";
        }
        String plain = Normalizer.normalize(value.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return plain.toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    private static List<Map<String, String>> readCsv(byte[] bytes) {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException notText) {
            throw ApiException.unsupportedMediaType("IMPORT_FILE_TYPE", "Chỉ nhận tệp .xlsx hoặc .csv UTF-8");
        }
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        int firstLineEnd = text.indexOf('\n');
        String header = firstLineEnd < 0 ? text : text.substring(0, firstLineEnd);
        char delimiter = header.contains(";") && !header.contains(",") ? ';' : ',';
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).setAllowMissingColumnNames(true).build();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            List<String> headers = parser.getHeaderNames().stream().map(QuestionImporter::header).toList();
            requireColumns(headers);
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.size() && i < record.size(); i++) {
                    row.put(headers.get(i), record.get(i));
                }
                if (!isBlank(row)) {
                    row.put(ROW_NUMBER, String.valueOf(record.getRecordNumber() + 1));
                    rows.add(row);
                }
                if (rows.size() > MAX_ROWS) {
                    break;
                }
            }
            return rows;
        } catch (IOException | IllegalArgumentException | IllegalStateException unreadable) {
            throw ApiException.badRequest("IMPORT_FILE_INVALID", "Không đọc được tệp CSV");
        }
    }

    private static List<Map<String, String>> readXlsx(byte[] bytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() == 0) {
                return List.of();
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }
            Map<Integer, String> headers = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                headers.put(cell.getColumnIndex(), header(formatter.formatCellValue(cell)));
            }
            requireColumns(List.copyOf(headers.values()));
            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum() && rows.size() <= MAX_ROWS; r++) {
                Row source = sheet.getRow(r);
                if (source == null) {
                    continue;
                }
                Map<String, String> row = new HashMap<>();
                // Không truyền FormulaEvaluator: ô công thức trả về chuỗi công thức, không bao giờ được tính.
                headers.forEach((column, name) -> row.put(name, formatter.formatCellValue(source.getCell(column))));
                if (!isBlank(row)) {
                    row.put(ROW_NUMBER, String.valueOf(r + 1));
                    rows.add(row);
                }
            }
            return rows;
        } catch (ApiException rejected) {
            throw rejected;
        } catch (IOException | RuntimeException unreadable) {
            throw ApiException.badRequest("IMPORT_FILE_INVALID", "Không đọc được tệp .xlsx");
        }
    }

    private static void requireColumns(List<String> headers) {
        List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !headers.contains(c)).toList();
        if (!missing.isEmpty()) {
            throw ApiException.badRequest("IMPORT_HEADER_INVALID",
                    "Tệp thiếu cột theo mẫu: " + String.join(", ", missing));
        }
    }

    private static String header(String name) {
        return name == null ? "" : name.replace("﻿", "").strip().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(Map<String, String> row) {
        return row.values().stream().allMatch(v -> v == null || v.isBlank());
    }

    private static boolean isZip(byte[] bytes) {
        return bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4;
    }


    private static String preview(String stem) {
        if (stem == null) {
            return "";
        }
        if (stem.codePointCount(0, stem.length()) <= STEM_PREVIEW) {
            return stem;
        }
        return stem.substring(0, stem.offsetByCodePoints(0, STEM_PREVIEW)) + "…";
    }
}
