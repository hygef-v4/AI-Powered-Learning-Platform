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
    void csvQuotesTextAndNeutralizesFormulas() throws Exception {
        var out = new ByteArrayOutputStream();
        new GradebookExporter().write(List.of(List.of("Name", "Feedback"), List.of("=SUM(1,2)", " \t@evil\n\"quote\"")),
                "csv", out);
        var value = out.toString(StandardCharsets.UTF_8);
        assertThat(value).contains("'=SUM(1,2)").contains("' \t@evil").contains("\"\"quote\"\"");
    }

    @Test
    void xlsxIsWellFormedAndUsesTextCellsInsteadOfFormula() throws Exception {
        var out = new ByteArrayOutputStream();
        new GradebookExporter().write(List.of(List.of("=1+1", "Tiếng Việt & < >")), "xlsx", out);
        var entries = new HashMap<String, String>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                var bytes = zip.readAllBytes();
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
                entries.put(e.getName(), new String(bytes, StandardCharsets.UTF_8));
            }
        }
        assertThat(entries).containsKeys("[Content_Types].xml", "xl/workbook.xml", "xl/worksheets/sheet1.xml");
        assertThat(entries.get("xl/worksheets/sheet1.xml")).contains("inlineStr", "=1+1", "&amp;")
                .doesNotContain("<f>");
    }
}
