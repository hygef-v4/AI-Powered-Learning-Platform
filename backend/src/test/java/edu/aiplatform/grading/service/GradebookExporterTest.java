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
}
