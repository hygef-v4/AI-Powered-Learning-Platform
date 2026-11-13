package edu.aiplatform.materials.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.files.contract.IArtifact;
import java.io.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.*;

class MaterialTextExtractorTest {
    IArtifact files;
    MaterialTextExtractor extractor;

    @BeforeEach
    void setup() {
        files = mock(IArtifact.class);
        extractor = new MaterialTextExtractor(files);
    }

    @AfterEach
    void close() {
        extractor.close();
    }

    @Test
    void deadlineCancelsAndClosesInputStream() throws Exception {
        extractor.close();
        extractor = new MaterialTextExtractor(files, java.time.Duration.ofMillis(250));
        var closed = new java.util.concurrent.CountDownLatch(1);
        InputStream blocking =
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        try {
                            Thread.sleep(30_000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException();
                        }
                        return -1;
                    }

                    @Override
                    public void close() {
                        closed.countDown();
                    }
                };
        when(files.open("slow")).thenReturn(blocking);
        assertThatThrownBy(() -> extractor.extract("slow"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(closed.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void extractsActualDocxInBoundedChildJvm() throws Exception {
        byte[] bytes;
        try (var document = new XWPFDocument();
                var output = new ByteArrayOutputStream()) {
            document.createParagraph()
                    .createRun()
                    .setText("Reference material sentence ".repeat(10));
            document.write(output);
            bytes = output.toByteArray();
        }
        when(files.open("file")).thenReturn(new ByteArrayInputStream(bytes));
        assertThat(extractor.extract("file").text()).contains("Reference material sentence");
    }

    @Test
    void extractsActualPptx() throws Exception {
        byte[] bytes;
        try (var document = new XMLSlideShow();
                var output = new ByteArrayOutputStream()) {
            document.createSlide()
                    .createTextBox()
                    .setText("Presentation reference content ".repeat(10));
            document.write(output);
            bytes = output.toByteArray();
        }
        when(files.open("file")).thenReturn(new ByteArrayInputStream(bytes));
        assertThat(extractor.extract("file").text()).contains("Presentation reference content");
    }

    @Test
    void extractsActualPdfAndCountsPages() throws Exception {
        byte[] bytes;
        try (var document = new PDDocument();
                var output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            try (var stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(20, 700);
                stream.showText(
                        "Reference content with enough readable text for material extraction verification.");
                stream.endText();
            }
            document.save(output);
            bytes = output.toByteArray();
        }
        when(files.open("file")).thenReturn(new ByteArrayInputStream(bytes));
        var result = extractor.extract("file");
        assertThat(result.text()).contains("Reference content");
        assertThat(result.pages()).isEqualTo(1);
    }

    @Test
    void extractsActualXlsxAcrossSheetsUsingCachedFormulaValues() throws Exception {
        byte[] bytes;
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                var output = new ByteArrayOutputStream()) {
            workbook.createSheet("Overview").createRow(0).createCell(0)
                    .setCellValue("Workbook overview reference sentence ".repeat(10));
            workbook.createSheet("Details").createRow(0).createCell(0)
                    .setCellValue("Second sheet reference evidence ".repeat(10));
            var formula = workbook.getSheetAt(1).getRow(0).createCell(1);
            formula.setCellFormula("7*8");
            formula.getCTCell().setV("137");
            workbook.write(output);
            bytes = output.toByteArray();
        }
        when(files.open("xlsx-file")).thenReturn(new ByteArrayInputStream(bytes));
        var result = extractor.extract("xlsx-file");
        assertThat(result.text()).contains("Workbook overview reference sentence",
                "Second sheet reference evidence", "137").doesNotContain("56");
        assertThat(result.text().strip().length() / Math.max(1, result.pages()))
                .isGreaterThanOrEqualTo(50);
    }

    @Test
    void extractsFiftyMegabyteImageAndTextPdfWithinTwoMinutes() throws Exception {
        byte[] bytes;
        var random = new java.util.Random(20261108L);
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            for (int index = 0; index < 65; index++) {
                var image = new java.awt.image.BufferedImage(512, 512,
                        java.awt.image.BufferedImage.TYPE_3BYTE_BGR);
                random.nextBytes(((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData());
                var page = new PDPage();
                document.addPage(page);
                try (var content = new PDPageContentStream(document, page)) {
                    content.drawImage(org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, image),
                            20, 100, 512, 512);
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    content.newLineAtOffset(20, 700);
                    for (int line = 0; line < 4; line++) {
                        content.showText("Bounded extraction reference content");
                        content.newLineAtOffset(0, -14);
                    }
                    content.endText();
                }
            }
            document.save(output);
            bytes = output.toByteArray();
        }
        assertThat(bytes.length).isBetween(50_000_000, 52_428_800);
        when(files.open("large-pdf")).thenReturn(new ByteArrayInputStream(bytes));
        long started = System.nanoTime();
        var result = extractor.extract("large-pdf");
        long elapsed = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertThat(result.text()).contains("Bounded extraction reference content");
        assertThat(result.pages()).isEqualTo(65);
        assertThat(elapsed).isLessThanOrEqualTo(120_000);
        System.out.println("LOCAL_NFR_U13_02 bytes=" + bytes.length + " pages=65 elapsedMs=" + elapsed + " targetMs=120000");
    }

    @Test
    void imageOnlyPdfDoesNotInventTextThroughOcr() throws Exception {
        byte[] bytes;
        var image = new java.awt.image.BufferedImage(600, 80, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(java.awt.Color.BLACK);
            graphics.drawString("OCR_ONLY_EVIDENCE_SENTINEL", 10, 40);
        } finally {
            graphics.dispose();
        }
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            try (var stream = new PDPageContentStream(document, page)) {
                stream.drawImage(org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, image),
                        10, 500);
            }
            document.save(output);
            bytes = output.toByteArray();
        }
        when(files.open("image-only")).thenReturn(new ByteArrayInputStream(bytes));
        var result = extractor.extract("image-only");
        assertThat(result.text()).isBlank();
        assertThat(result.pages()).isEqualTo(1);
    }

    @Test
    void decodesUtf8AndRemovesBom() {
        byte[] bytes = "\uFEFFĐề SWR".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(MaterialTextExtractor.decodeText(bytes)).isEqualTo("Đề SWR");
    }

    @Test
    void rejectsMalformedUtf8InsteadOfSilentlyCorruptingSource() {
        assertThatThrownBy(() -> MaterialTextExtractor.decodeText(new byte[] {(byte) 0xC3, 0x28}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
