package edu.aiplatform.grading.service;

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
    /** Writes gradebook rows as CSV or XLSX to the supplied output stream. */
    public void writeGradebook(List<List<String>> rows, String format, OutputStream output)
            throws IOException {
        if ("csv".equals(format)) {
            var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
            writer.write('\uFEFF');
            for (var row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0)
                        writer.write(',');
                    writer.write(writeCsv(row.get(i)));
                }
                writer.write("\r\n");
            }
            writer.flush();
        } else if ("xlsx".equals(format))
            writeXlsx(rows, output);
        else
            throw ApiException.badRequest("INVALID_EXPORT_FORMAT", "Choose csv or xlsx");
    }

    /** Escapes CSV cell content and prefixes formula-like values to prevent spreadsheet execution. */
    static String writeCsv(String value) {
        value = value == null ? "" : value;
        String start = value.stripLeading().replaceAll("^[\\p{Cntrl}\\s]+", "");
        if (!start.isEmpty() && "=+-@".indexOf(start.charAt(0)) >= 0)
            value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /** Escapes text for inclusion in an XML document. */
    private static String escapeXml(String s) {
        if (s == null)
            return "";
        return s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Adds one UTF-8 XML file to the XLSX archive. */
    private static void writePart(ZipOutputStream zip, String path, String text)
            throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Builds a minimal XLSX workbook from the supplied gradebook rows. */
    private static void writeXlsx(List<List<String>> rows, OutputStream output) throws IOException {
        // finish(), not close(): ownership of the HTTP stream remains with Spring.
        var zip = new ZipOutputStream(output, StandardCharsets.UTF_8);
        writePart(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/pack"
                        + "age/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlforma"
                        + "ts-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Ov"
                        + "erride PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocumen"
                        + "t.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType="
                        + "\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
        writePart(zip, "_rels/.rels",
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relation"
                        + "ship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/off"
                        + "iceDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        writePart(zip, "xl/workbook.xml",
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://"
                        + "schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Gradebook"
                        + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        writePart(zip, "xl/_rels/workbook.xml.rels",
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relation"
                        + "ship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/wor"
                        + "ksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
        zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
        var writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
        writer.write(
                "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (var row : rows) {
            writer.write("<row>");
            for (var cell : row) {
                writer.write("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">");
                writer.write(escapeXml(cell));
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
