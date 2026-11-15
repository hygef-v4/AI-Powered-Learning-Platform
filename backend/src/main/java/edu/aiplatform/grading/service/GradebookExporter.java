package edu.aiplatform.grading.service;

import edu.aiplatform.shared.error.ApiException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.*;
import org.springframework.stereotype.Component;

/**
 * Streaming CSV / XLSX. No formulas, shared-string table, files
 * or external dependencies.
 */
@Component
public class GradebookExporter {
    public String buildFilename(String title, String fallback, String format) {
        String name = title == null ? fallback : title.replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_").strip();
        // Leave room for the extension within common 255-byte filesystem filename limits.
        name = name.codePoints().limit(60).collect(StringBuilder::new,
                StringBuilder::appendCodePoint, StringBuilder::append).toString()
                .replaceAll("[.\\s]+$", "");
        if (name.isBlank() || name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?"))
            name = fallback;
        return name + "." + format;
    }

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
                        + "\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                        + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
        writePart(zip, "_rels/.rels",
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relation"
                        + "ship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/off"
                        + "iceDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        writePart(zip, "xl/workbook.xml",
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://"
                        + "schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Bảng điểm"
                        + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        writePart(zip, "xl/_rels/workbook.xml.rels",
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relation"
                        + "ship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/wor"
                        + "ksheet\" Target=\"worksheets/sheet1.xml\"/>"
                        + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
        writePart(zip, "xl/styles.xml", """
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
                  <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
                  <borders count="1"><border/></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment wrapText="1" vertical="center"/></xf></cellXfs>
                  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                </styleSheet>
                """);
        zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
        var writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
        boolean matrix = !rows.isEmpty() && !rows.get(0).isEmpty()
                && "STT".equals(rows.get(0).get(0));
        int columnCount = rows.isEmpty() ? 0 : rows.get(0).size();
        writer.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        if (matrix) {
            writer.write("<sheetViews><sheetView workbookViewId=\"0\"><pane xSplit=\"4\" ySplit=\"1\" topLeftCell=\"E2\" activePane=\"bottomRight\" state=\"frozen\"/></sheetView></sheetViews>");
            writer.write("<cols><col min=\"1\" max=\"1\" width=\"6\" customWidth=\"1\"/><col min=\"2\" max=\"2\" width=\"12\" customWidth=\"1\"/><col min=\"3\" max=\"3\" width=\"28\" customWidth=\"1\"/><col min=\"4\" max=\"4\" width=\"32\" customWidth=\"1\"/>");
            if (columnCount > 4)
                writer.write("<col min=\"5\" max=\"" + columnCount + "\" width=\"24\" customWidth=\"1\"/>");
            writer.write("</cols>");
        }
        writer.write("<sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            var row = rows.get(rowIndex);
            writer.write("<row r=\"" + (rowIndex + 1) + "\""
                    + (rowIndex == 0 ? " ht=\"60\" customHeight=\"1\"" : "") + ">");
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String cell = row.get(columnIndex);
                String reference = buildColumnName(columnIndex) + (rowIndex + 1);
                if (matrix && rowIndex > 0 && (columnIndex == 0 || columnIndex >= 4)
                        && cell != null && cell.matches("-?\\d+(\\.\\d+)?")) {
                    writer.write("<c r=\"" + reference + "\"><v>" + cell + "</v></c>");
                } else {
                    writer.write("<c r=\"" + reference + "\" t=\"inlineStr\""
                            + (rowIndex == 0 ? " s=\"1\"" : "")
                            + "><is><t xml:space=\"preserve\">");
                    writer.write(escapeXml(cell));
                    writer.write("</t></is></c>");
                }
            }
            writer.write("</row>");
        }
        writer.write("</sheetData>");
        if (matrix && columnCount > 0)
            writer.write("<autoFilter ref=\"A1:" + buildColumnName(columnCount - 1) + rows.size() + "\"/>");
        writer.write("</worksheet>");
        writer.flush();
        zip.closeEntry();
        zip.finish();
        zip.flush();
    }

    private static String buildColumnName(int index) {
        var name = new StringBuilder();
        for (int column = index + 1; column > 0; column = (column - 1) / 26)
            name.append((char) ('A' + (column - 1) % 26));
        return name.reverse().toString();
    }
}
