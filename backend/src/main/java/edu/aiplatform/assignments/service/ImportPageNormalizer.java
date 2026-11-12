package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.model.ImportPage;
import edu.aiplatform.assignments.service.ImportFileReader.CheckedFile;
import edu.aiplatform.assignments.service.ImportFileReader.CheckedFiles;
import edu.aiplatform.assignments.service.ImportFileReader.FileKind;
import edu.aiplatform.shared.error.ApiException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Chuẩn hóa tệp đề thành ảnh trang (BR-U07-89, 90): ảnh xoay theo EXIF, PDF dựng ở 150 DPI; mỗi trang thu về cạnh dài
 * tối đa 2.000px, JPEG chất lượng 0,9, không metadata. PDF có lớp chữ thì giữ chữ từng trang làm gợi ý chính tả.
 */
@Component
public class ImportPageNormalizer {

    static final int MAX_PAGES = 10;
    static final int MAX_LONG_SIDE = 2_000;
    static final int LOW_RESOLUTION = 800;
    static final long MAX_PIXELS = 40_000_000L;
    static final float PDF_DPI = 150f;
    static final int MAX_HINT_PER_PAGE = 20_000;
    static final int MAX_HINT_TOTAL = 60_000;
    static final String JPEG = "image/jpeg";

    static {
        // Nạp plugin ImageIO trên classpath (đọc WebP) cả khi chạy trong fat jar.
        ImageIO.scanForPlugins();
    }

    /** Cảnh báo kèm trang (đánh số từ 1) để hiện ở màn xem trước. */
    public record PageWarning(String kind, int page, String message) {
    }

    public record NormalizedPages(List<ImportPage> pages, List<PageWarning> warnings) {
    }

    public NormalizedPages normalizePages(CheckedFiles files) {
        return files.kind() == FileKind.PDF ? normalizePdf(files.files().get(0)) : normalizeImages(files.files());
    }

    private NormalizedPages normalizeImages(List<CheckedFile> files) {
        List<ImportPage> pages = new ArrayList<>();
        List<PageWarning> warnings = new ArrayList<>();
        for (CheckedFile file : files) {
            int pageNo = pages.size() + 1;
            BufferedImage image = orient(decodeImage(file.content()), JpegOrientation.read(file.content()));
            pages.add(encodePage(pageNo, image, null, warnings));
        }
        return new NormalizedPages(List.copyOf(pages), List.copyOf(warnings));
    }

    private NormalizedPages normalizePdf(CheckedFile file) {
        try (PDDocument document = Loader.loadPDF(file.content())) {
            int count = document.getNumberOfPages();
            if (count == 0 || count > MAX_PAGES) {
                throw ApiException.badRequest("ASSIGNMENT_IMPORT_PDF_PAGES", "PDF phải có từ 1 đến 10 trang");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            PDFTextStripper stripper = new PDFTextStripper();
            List<ImportPage> pages = new ArrayList<>();
            List<PageWarning> warnings = new ArrayList<>();
            int hintChars = 0;
            for (int index = 0; index < count; index++) {
                BufferedImage image = renderer.renderImageWithDPI(index, dpiFor(document.getPage(index)), ImageType.RGB);
                String hint = null;
                if (hintChars < MAX_HINT_TOTAL) {
                    stripper.setStartPage(index + 1);
                    stripper.setEndPage(index + 1);
                    hint = limitHint(stripper.getText(document), MAX_HINT_TOTAL - hintChars);
                    hintChars += hint == null ? 0 : hint.length();
                }
                pages.add(encodePage(index + 1, image, hint, warnings));
            }
            return new NormalizedPages(List.copyOf(pages), List.copyOf(warnings));
        } catch (InvalidPasswordException e) {
            throw ApiException.badRequest("ASSIGNMENT_IMPORT_PDF_ENCRYPTED", "PDF có mật khẩu, hãy bỏ mật khẩu rồi thử lại");
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException api) {
                throw api;
            }
            throw unreadable();
        }
    }

    /** Trang PDF khổ lớn: hạ DPI để ảnh dựng không vượt 40 megapixel. */
    private static float dpiFor(PDPage page) {
        PDRectangle box = page.getCropBox();
        double pixels = (box.getWidth() / 72.0 * PDF_DPI) * (box.getHeight() / 72.0 * PDF_DPI);
        return pixels <= MAX_PIXELS ? PDF_DPI : (float) (PDF_DPI * Math.sqrt(MAX_PIXELS / pixels));
    }

    private static String limitHint(String text, int left) {
        if (text == null || text.isBlank() || left <= 0) {
            return null;
        }
        String trimmed = text.strip();
        return trimmed.substring(0, Math.min(trimmed.length(), Math.min(MAX_HINT_PER_PAGE, left)));
    }

    /** Đọc kích thước từ header trước khi giải mã để chặn ảnh nén "bom" (BR-U07-88). */
    static BufferedImage decodeImage(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = input == null ? null : ImageIO.getImageReaders(input);
            if (readers == null || !readers.hasNext()) {
                throw unreadable();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw ApiException.badRequest("ASSIGNMENT_IMPORT_IMAGE_TOO_LARGE",
                            "Ảnh quá lớn (trên 40 megapixel); hãy thu nhỏ ảnh rồi thử lại");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw unreadable();
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException api) {
                throw api;
            }
            throw unreadable();
        }
    }

    private static ApiException unreadable() {
        return ApiException.badRequest("ASSIGNMENT_IMPORT_UNREADABLE", "Không đọc được tệp; kiểm tra tệp rồi thử lại");
    }

    private static ImportPage encodePage(int pageNo, BufferedImage source, String hint, List<PageWarning> warnings) {
        int longSide = Math.max(source.getWidth(), source.getHeight());
        if (longSide < LOW_RESOLUTION) {
            warnings.add(new PageWarning("LOW_RESOLUTION", pageNo, "Trang " + pageNo
                    + " có độ phân giải thấp, AI có thể đọc sai; nên chụp rõ hơn"));
        }
        double scale = longSide > MAX_LONG_SIDE ? (double) MAX_LONG_SIDE / longSide : 1.0;
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage page = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = page.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return new ImportPage(pageNo, JPEG, width, height, encodeJpeg(page), hint);
    }

    static byte[] encodeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.9f);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new IllegalStateException("Không mã hóa được ảnh trang", e);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /** Xoay/lật ảnh theo EXIF Orientation 1–8 để chữ đứng thẳng như người chụp thấy. */
    static BufferedImage orient(BufferedImage image, int orientation) {
        if (orientation <= 1 || orientation > 8) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swap = orientation >= 5;
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2 -> {
                t.translate(w, 0);
                t.scale(-1, 1);
            }
            case 3 -> {
                t.translate(w, h);
                t.rotate(Math.PI);
            }
            case 4 -> {
                t.translate(0, h);
                t.scale(1, -1);
            }
            case 5 -> {
                t.rotate(Math.PI / 2);
                t.scale(1, -1);
            }
            case 6 -> {
                t.translate(h, 0);
                t.rotate(Math.PI / 2);
            }
            case 7 -> {
                t.scale(-1, 1);
                t.translate(-h, 0);
                t.translate(0, w);
                t.rotate(3 * Math.PI / 2);
            }
            default -> {
                t.translate(0, w);
                t.rotate(3 * Math.PI / 2);
            }
        }
        BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = out.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, out.getWidth(), out.getHeight());
            graphics.drawImage(image, t, null);
        } finally {
            graphics.dispose();
        }
        return out;
    }

    /** Đọc thẻ Orientation (0x0112) trong APP1 Exif của JPEG; không có hoặc không đọc được thì trả 1. */
    static final class JpegOrientation {

        private JpegOrientation() {
        }

        static int read(byte[] jpeg) {
            if (jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
                return 1;
            }
            int at = 2;
            while (at + 4 <= jpeg.length && (jpeg[at] & 0xFF) == 0xFF) {
                int marker = jpeg[at + 1] & 0xFF;
                int length = ((jpeg[at + 2] & 0xFF) << 8) | (jpeg[at + 3] & 0xFF);
                if (marker == 0xDA || length < 2) {
                    return 1;
                }
                if (marker == 0xE1 && at + 10 <= jpeg.length && jpeg[at + 4] == 'E' && jpeg[at + 5] == 'x'
                        && jpeg[at + 6] == 'i' && jpeg[at + 7] == 'f') {
                    return readTiff(jpeg, at + 10, Math.min(jpeg.length, at + 2 + length));
                }
                at += 2 + length;
            }
            return 1;
        }

        private static int readTiff(byte[] b, int start, int end) {
            if (start + 8 > end) {
                return 1;
            }
            boolean little = b[start] == 'I';
            int ifd = start + int32(b, start + 4, little);
            if (ifd < start || ifd + 2 > end) {
                return 1;
            }
            int entries = int16(b, ifd, little);
            for (int i = 0; i < entries; i++) {
                int entry = ifd + 2 + i * 12;
                if (entry + 12 > end) {
                    return 1;
                }
                if (int16(b, entry, little) == 0x0112) {
                    int value = int16(b, entry + 8, little);
                    return value >= 1 && value <= 8 ? value : 1;
                }
            }
            return 1;
        }

        private static int int16(byte[] b, int at, boolean little) {
            return little ? (b[at] & 0xFF) | ((b[at + 1] & 0xFF) << 8) : ((b[at] & 0xFF) << 8) | (b[at + 1] & 0xFF);
        }

        private static int int32(byte[] b, int at, boolean little) {
            return little ? int16(b, at, true) | (int16(b, at + 2, true) << 16)
                    : (int16(b, at, false) << 16) | int16(b, at + 2, false);
        }
    }
}
