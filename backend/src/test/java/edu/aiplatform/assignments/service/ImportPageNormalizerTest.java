package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.assignments.model.ImportPage;
import edu.aiplatform.assignments.service.ImportFileReader.CheckedFile;
import edu.aiplatform.assignments.service.ImportFileReader.CheckedFiles;
import edu.aiplatform.assignments.service.ImportFileReader.FileKind;
import edu.aiplatform.assignments.service.ImportPageNormalizer.NormalizedPages;
import edu.aiplatform.shared.error.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImportPageNormalizerTest {

    private final ImportPageNormalizer normalizer = new ImportPageNormalizer();

    private static CheckedFiles images(byte[]... contents) {
        List<CheckedFile> files = java.util.Arrays.stream(contents)
                .map(c -> new CheckedFile("anh", "image/png", c)).toList();
        return new CheckedFiles(FileKind.IMAGE, files, 0);
    }

    private static CheckedFiles pdf(byte[] content) {
        return new CheckedFiles(FileKind.PDF, List.of(new CheckedFile("de.pdf", "application/pdf", content)),
                content.length);
    }

    private static BufferedImage read(ImportPage page) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(page.bytes()));
    }

    @Test
    void imageOverFortyMegapixelsIsRejectedBeforeDecoding() {
        assertThatThrownBy(() -> normalizer.normalizePages(images(ImportTestImages.pngHeaderOnly(8000, 6000))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("ASSIGNMENT_IMPORT_IMAGE_TOO_LARGE"));
    }

    @Test
    void pdfOverTenPagesOrWithPasswordIsRejected() {
        assertThatThrownBy(() -> normalizer.normalizePages(pdf(ImportTestImages.pdf(11, null))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("ASSIGNMENT_IMPORT_PDF_PAGES"));
        assertThatThrownBy(() -> normalizer.normalizePages(pdf(ImportTestImages.encryptedPdf())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("ASSIGNMENT_IMPORT_PDF_ENCRYPTED"));
        assertThatThrownBy(() -> normalizer.normalizePages(pdf("%PDF-1.7 broken".getBytes())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("ASSIGNMENT_IMPORT_UNREADABLE"));
    }
}
