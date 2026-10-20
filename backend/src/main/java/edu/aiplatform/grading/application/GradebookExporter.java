package edu.aiplatform.grading.application;

import edu.aiplatform.shared.error.ApiException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.*;
import org.springframework.stereotype.Component;

/**
 * Streaming CSV / inline-string XLSX. No formulas, shared-string table, files
 * or external dependencies.
 */
@Component
public class GradebookExporter {
    public void write(List<List<String>> rows, String format, OutputStream output) throws IOException {
        if ("csv".equals(format)) {
            var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
            writer.write('\uFEFF');
            for (var row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0)
                        writer.write(',');
                    writer.write(csv(row.get(i)));
                }
                writer.write("\r\n");
            }
            writer.flush();
        } else if ("xlsx".equals(format))
            xlsx(rows, output);
        else
            throw ApiException.badRequest("INVALID_EXPORT_FORMAT", "Choose csv or xlsx");
    }

    static String csv(String value) {
        value = value == null ? "" : value;
        String start = value.stripLeading().replaceAll("^[\\p{Cntrl}\\s]+", "");
        if (!start.isEmpty() && "=+-@".indexOf(start.charAt(0)) >= 0)
            value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String xml(String s) {
        if (s == null)
            return "";
        return s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void part(ZipOutputStream zip, String path, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void xlsx(List<List<String>> rows, OutputStream output) throws IOException {
        // finish(), not close(): ownership of the HTTP stream remains with Spring.
        var zip = new ZipOutputStream(output, StandardCharsets.UTF_8);
        part(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/pack"
                        + "age/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlforma"
                        + "ts-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Ov"
                        + "erride PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocumen"
                        + "t.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType="
                        + "\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
        part(zip, "_rels/.rels",
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relation"
                        + "ship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/off"
                        + "iceDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        part(zip, "xl/workbook.xml",
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://"
                        + "schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Gradebook"
                        + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        part(zip, "xl/_rels/workbook.xml.rels",
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relation"
                        + "ship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/wor"
                        + "ksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
        zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
        var writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
        writer.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (var row : rows) {
            writer.write("<row>");
            for (var cell : row) {
                writer.write("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">");
                writer.write(xml(cell));
                writer.write("</t></is></c>");
            }
            writer.write("</row>");
        }
        writer.write("</sheetData></worksheet>");
        writer.flush();
        zip.closeEntry();
        zip.finish();
        zip.flush();
    }
}
