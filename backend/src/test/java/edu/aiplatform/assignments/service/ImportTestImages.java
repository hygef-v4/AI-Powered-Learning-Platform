package edu.aiplatform.assignments.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Ảnh và PDF nhỏ sinh trong test nhập đề, không dùng tệp có sẵn. */
final class ImportTestImages {

    private ImportTestImages() {
    }

    static BufferedImage drawn(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.RED);
        g.fillRect(0, 0, Math.max(1, width / 4), Math.max(1, height / 4));
        g.dispose();
        return image;
    }

    static byte[] png(int width, int height) {
        return encode(drawn(width, height), "png");
    }

    static byte[] jpeg(int width, int height) {
        return encode(drawn(width, height), "jpeg");
    }

    static byte[] encode(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Header PNG khai kích thước rất lớn, không có dữ liệu ảnh: chỉ để kiểm chặn trước khi giải mã. */
    static byte[] pngHeaderOnly(int width, int height) {
        ByteBuffer ihdr = ByteBuffer.allocate(17);
        ihdr.put("IHDR".getBytes()).putInt(width).putInt(height).put((byte) 8).put((byte) 2).put((byte) 0)
                .put((byte) 0).put((byte) 0);
        CRC32 crc = new CRC32();
        crc.update(ihdr.array());
        ByteBuffer out = ByteBuffer.allocate(8 + 4 + 17 + 4);
        out.put(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        out.putInt(13).put(ihdr.array()).putInt((int) crc.getValue());
        return out.array();
    }

    /** JPEG thêm khối APP1 Exif chỉ có thẻ Orientation. */
    static byte[] jpegWithOrientation(int width, int height, int orientation) {
        byte[] jpeg = jpeg(width, height);
        byte[] tiff = {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
                0, 0, 0, 0, 0, 0};
        int length = 2 + 6 + tiff.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF);
        out.write(0xD8);
        out.write(0xFF);
        out.write(0xE1);
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.writeBytes(new byte[]{'E', 'x', 'i', 'f', 0, 0});
        out.writeBytes(tiff);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }

    static byte[] pdf(int pages, String text) {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (text != null) {
                    try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        content.newLineAtOffset(72, 700);
                        content.showText(text + " " + (i + 1));
                        content.endText();
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static byte[] encryptedPdf() {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            var policy = new org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy("owner", "user",
                    new org.apache.pdfbox.pdmodel.encryption.AccessPermission());
            document.protect(policy);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
