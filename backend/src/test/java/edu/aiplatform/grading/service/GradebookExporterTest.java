package edu.aiplatform.grading.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;

class GradebookExporterTest {
    @Test
    void filenamesPreserveUnicodeAndHandleUnsafeReservedEmptyAndLongTitles() {
        var exporter = new GradebookExporter();
        assertThat(exporter.buildFilename("Bài tập tuần 1", "gradebook", "xlsx"))
                .isEqualTo("Bài tập tuần 1.xlsx");
        assertThat(exporter.buildFilename("Bài: 1/2\\3?*<>|\"\r\n. ", "gradebook", "csv"))
                .isEqualTo("Bài_ 1_2_3________.csv");
        for (String title : List.of("", "... ", "CON", "nul.txt", "LPT9"))
            assertThat(exporter.buildFilename(title, "gradebook", "xlsx")).isEqualTo("gradebook.xlsx");
        assertThat(exporter.buildFilename(null, "gradebook-class-date", "xlsx"))
                .isEqualTo("gradebook-class-date.xlsx");
        assertThat(exporter.buildFilename("😀".repeat(150), "gradebook", "xlsx"))
                .isEqualTo("😀".repeat(60) + ".xlsx");
    }

    @Test
    void csvQuotesTextAndNeutralizesFormulas() throws Exception {
        var out = new ByteArrayOutputStream();
        new GradebookExporter().writeGradebook(List.of(List.of("Name", "Feedback"), List.of("=SUM(1,2)", " \t@evil\n\"quote\"")),
                "csv", out);
        var value = out.toString(StandardCharsets.UTF_8);
        assertThat(value).contains("'=SUM(1,2)").contains("' \t@evil").contains("\"\"quote\"\"");
    }

    @Test
    void xlsxIsWellFormedAndUsesTextCellsInsteadOfFormula() throws Exception {
        var out = new ByteArrayOutputStream();
        new GradebookExporter().writeGradebook(List.of(List.of("=1+1", "Tiếng Việt & < >")), "xlsx", out);
        var entries = new HashMap<String, String>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                var bytes = zip.readAllBytes();
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
                entries.put(e.getName(), new String(bytes, StandardCharsets.UTF_8));
            }
        }
        assertThat(entries).containsKeys("[Content_Types].xml", "xl/workbook.xml", "xl/worksheets/sheet1.xml");
        assertThat(entries.get("xl/workbook.xml")).contains("name=\"Bảng điểm\"");
        assertThat(entries.get("xl/worksheets/sheet1.xml")).contains("inlineStr", "=1+1", "&amp;")
                .doesNotContain("<f>");
    }

    @Test
    void matrixUsesNumericScoresAndFrozenStudentColumnsWithoutExecutingText() throws Exception {
        var out = new ByteArrayOutputStream();
        var header = new ArrayList<>(List.of("STT", "Họ và tên", "Email"));
        header.addAll(Collections.nCopies(25, "=Tên bài (/10)"));
        var student = new ArrayList<>(List.of("1", "=SUM(1,2)", "00123"));
        student.addAll(Collections.nCopies(25, ""));
        student.set(3, "0");
        student.set(27, "8.25");
        new GradebookExporter().writeGradebook(List.of(header, student), "xlsx", out);
        var entries = new HashMap<String, String>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                var bytes = zip.readAllBytes();
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new ByteArrayInputStream(bytes));
                entries.put(entry.getName(), new String(bytes, StandardCharsets.UTF_8));
            }
        }
        assertThat(entries.get("xl/worksheets/sheet1.xml"))
                .contains("xSplit=\"3\" ySplit=\"1\"", "topLeftCell=\"D2\"", "state=\"frozen\"",
                        "<c r=\"A2\"><v>1</v></c>", "<c r=\"D2\"><v>0</v></c>",
                        "<c r=\"AB2\"><v>8.25</v></c>", "<autoFilter ref=\"A1:AB2\"/>",
                        "r=\"B2\" t=\"inlineStr\"", "r=\"C2\" t=\"inlineStr\"", "=SUM(1,2)", "00123")
                .doesNotContain("<f>");
        assertThat(entries).containsKey("xl/styles.xml");
    }
}
