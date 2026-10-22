package edu.aiplatform.materials.infrastructure;

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
    IArtifact files;MaterialTextExtractor extractor;
    @BeforeEach void setup() {files=mock(IArtifact.class);extractor=new MaterialTextExtractor(files);}
    @AfterEach void close() {extractor.close();}
    @Test void deadlineCancelsAndClosesInputStream() throws Exception {
        extractor.close();extractor=new MaterialTextExtractor(files,java.time.Duration.ofMillis(250));
        var closed=new java.util.concurrent.CountDownLatch(1);
        InputStream blocking=new InputStream() {
            @Override public int read() throws IOException {try {Thread.sleep(30_000);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException();}return -1;}
            @Override public void close(){closed.countDown();}
        };
        when(files.open("slow")).thenReturn(blocking);
        assertThatThrownBy(()->extractor.extract("slow")).isInstanceOf(IllegalStateException.class);
        assertThat(closed.await(2,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
    @Test void extractsActualDocxInBoundedChildJvm() throws Exception {
        byte[] bytes;try(var document=new XWPFDocument();var output=new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Reference material sentence ".repeat(10));document.write(output);bytes=output.toByteArray();
        }
        when(files.open("file")).thenReturn(new ByteArrayInputStream(bytes));
        assertThat(extractor.extract("file").text()).contains("Reference material sentence");
    }
    @Test void extractsActualPptx() throws Exception {
        byte[] bytes;try(var document=new XMLSlideShow();var output=new ByteArrayOutputStream()) {
            document.createSlide().createTextBox().setText("Presentation reference content ".repeat(10));document.write(output);bytes=output.toByteArray();
        }
        when(files.open("file")).thenReturn(new ByteArrayInputStream(bytes));
        assertThat(extractor.extract("file").text()).contains("Presentation reference content");
    }
    @Test void extractsActualPdfAndCountsPages() throws Exception {
        byte[] bytes;try(var document=new PDDocument();var output=new ByteArrayOutputStream()) {
            var page=new PDPage();document.addPage(page);
            try(var stream=new PDPageContentStream(document,page)) {
                stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                stream.newLineAtOffset(20,700);stream.showText("Reference content with enough readable text for material extraction verification.");stream.endText();
            }
            document.save(output);bytes=output.toByteArray();
        }
        when(files.open("file")).thenReturn(new ByteArrayInputStream(bytes));var result=extractor.extract("file");
        assertThat(result.text()).contains("Reference content");assertThat(result.pages()).isEqualTo(1);
    }
}
