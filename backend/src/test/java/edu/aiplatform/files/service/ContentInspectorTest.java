package edu.aiplatform.files.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.files.model.FileType;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContentInspectorTest {

    @TempDir
    Path dir;

    final ContentInspector inspector = new ContentInspector();

    @Test
    void detectsRealXlsxFromPackageContentDespiteMisleadingExtension() throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                var output = new java.io.ByteArrayOutputStream()) {
            workbook.createSheet("Evidence").createRow(0).createCell(0).setCellValue("Workbook source");
            workbook.write(output);
            assertThat(inspector.detect(write("renamed.pdf", output.toByteArray()), "renamed.pdf"))
                    .contains(FileType.XLSX);
        }
    }

    @Test
    void macroEnabledWorkbookCannotEnterFixedMaterialWhitelistByRenamingToXlsx() throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                var output = new java.io.ByteArrayOutputStream()) {
            workbook.setWorkbookType(org.apache.poi.xssf.usermodel.XSSFWorkbookType.XLSM);
            workbook.createSheet("Evidence").createRow(0).createCell(0).setCellValue("Macro-enabled package");
            workbook.write(output);
            assertThat(inspector.detect(write("renamed.xlsx", output.toByteArray()), "renamed.xlsx"))
                    .isEmpty();
        }
    }

    @Test
    void detectsByContentNotByExtension() throws Exception {
        assertThat(inspector.detect(write("anh.pdf", TestFiles.PNG), "anh.pdf")).contains(FileType.PNG);
        assertThat(inspector.detect(write("de.pdf", TestFiles.PDF), "de.pdf")).contains(FileType.PDF);
        assertThat(inspector.detect(write("so-do.svg", TestFiles.SVG), "so-do.svg")).contains(FileType.SVG);
        assertThat(inspector.detect(write("bai.docx", TestFiles.docx()), "bai.docx")).contains(FileType.DOCX);
    }

    @Test
    void detectsWebpByItsRiffSignature() throws Exception {
        byte[] webp = {'R', 'I', 'F', 'F', 0x1A, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', 'L', 0x0D, 0, 0, 0, 0x2F,
                0, 0, 0, 0x10, 0x07, 0x10, 0x11, 0x11, (byte) 0x88, (byte) 0x88, (byte) 0xFE, 0x07, 0};

        assertThat(inspector.detect(write("de.png", webp), "de.png")).contains(FileType.WEBP);
    }

    @Test
    void textRenamedAsPdfIsRecognizedByItsActualContent() throws Exception {
        assertThat(inspector.detect(write("gia.pdf", TestFiles.TEXT), "gia.pdf")).contains(FileType.TXT);
    }

    @Test
    void detectsPlainTextForAssignmentSources() throws Exception {
        assertThat(inspector.detect(write("exam.txt", "Exam context and requirements".getBytes()), "exam.txt"))
                .contains(FileType.TXT);
    }

    private Path write(String name, byte[] content) throws Exception {
        return Files.write(dir.resolve(name), content);
    }
}
