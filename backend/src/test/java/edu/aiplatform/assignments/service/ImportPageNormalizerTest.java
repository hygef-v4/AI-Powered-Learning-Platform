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
    void imagesKeepTheChosenOrderAndBecomeJpegPagesNoLongerThanTwoThousandPixels() throws Exception {
        NormalizedPages result = normalizer.normalizePages(images(ImportTestImages.png(3000, 1500),
                ImportTestImages.png(1000, 1400)));

        assertThat(result.pages()).extracting(ImportPage::pageNo).containsExactly(1, 2);
        ImportPage first = result.pages().get(0);
        assertThat(first.mediaType()).isEqualTo("image/jpeg");
        assertThat(first.width()).isEqualTo(2000);
        assertThat(first.height()).isEqualTo(1000);
        assertThat(read(first).getWidth()).isEqualTo(2000);
        assertThat(result.pages().get(1).width()).as("không phóng to").isEqualTo(1000);
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void smallImagesGetALowResolutionWarning() {
        NormalizedPages result = normalizer.normalizePages(images(ImportTestImages.png(400, 300)));

        assertThat(result.warnings()).singleElement().satisfies(w -> {
            assertThat(w.kind()).isEqualTo("LOW_RESOLUTION");
            assertThat(w.page()).isEqualTo(1);
        });
    }

    @Test
    void jpegIsRotatedUprightFromItsExifOrientation() {
        byte[] sideways = ImportTestImages.jpegWithOrientation(1200, 900, 6);

        assertThat(ImportPageNormalizer.JpegOrientation.read(sideways)).isEqualTo(6);
        ImportPage page = normalizer.normalizePages(images(sideways)).pages().get(0);
        assertThat(page.width()).isEqualTo(900);
        assertThat(page.height()).isEqualTo(1200);
        assertThat(ImportPageNormalizer.JpegOrientation.read(page.bytes())).as("bỏ metadata").isEqualTo(1);
    }

    @Test
    void orientationTransformsMapEveryExifValue() {
        BufferedImage source = ImportTestImages.drawn(40, 20);
        for (int orientation = 1; orientation <= 8; orientation++) {
            BufferedImage out = ImportPageNormalizer.orient(source, orientation);
            boolean swapped = orientation >= 5;
            assertThat(out.getWidth()).isEqualTo(swapped ? 20 : 40);
            assertThat(out.getHeight()).isEqualTo(swapped ? 40 : 20);
        }
        // Góc đỏ trên trái của ảnh gốc: xoay 90° theo chiều kim đồng hồ thì sang góc trên phải.
        BufferedImage rotated = ImportPageNormalizer.orient(source, 6);
        assertThat(rotated.getRGB(19, 0) & 0xFFFFFF).isEqualTo(0xFF0000);
        assertThat(rotated.getRGB(0, 39) & 0xFFFFFF).isEqualTo(0xFFFFFF);
    }

    @Test
    void imageOverFortyMegapixelsIsRejectedBeforeDecoding() {
        assertThatThrownBy(() -> normalizer.normalizePages(images(ImportTestImages.pngHeaderOnly(8000, 6000))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ASSIGNMENT_IMPORT_IMAGE_TOO_LARGE"));
    }

    @Test
    void brokenImageIsUnreadable() {
        assertThatThrownBy(() -> normalizer.normalizePages(images(new byte[]{1, 2, 3, 4})))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ASSIGNMENT_IMPORT_UNREADABLE"));
    }

    @Test
    void pdfPagesAreRenderedInOrderWithTheirTextLayerAsHint() {
        NormalizedPages result = normalizer.normalizePages(pdf(ImportTestImages.pdf(3, "Question")));

        assertThat(result.pages()).hasSize(3);
        assertThat(result.pages().get(0).width()).isEqualTo(1275);
        assertThat(result.pages().get(0).textHint()).isEqualTo("Question 1");
        assertThat(result.pages().get(2).textHint()).isEqualTo("Question 3");
    }

    @Test
    void scannedPdfWithoutTextHasNoHint() {
        assertThat(normalizer.normalizePages(pdf(ImportTestImages.pdf(1, null))).pages().get(0).textHint()).isNull();
    }

    @Test
    void pdfOverTenPagesOrWithPasswordIsRejected() {
        assertThatThrownBy(() -> normalizer.normalizePages(pdf(ImportTestImages.pdf(11, null))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ASSIGNMENT_IMPORT_PDF_PAGES"));
        assertThatThrownBy(() -> normalizer.normalizePages(pdf(ImportTestImages.encryptedPdf())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ASSIGNMENT_IMPORT_PDF_ENCRYPTED"));
        assertThatThrownBy(() -> normalizer.normalizePages(pdf("%PDF-1.7 broken".getBytes())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ASSIGNMENT_IMPORT_UNREADABLE"));
    }
}
