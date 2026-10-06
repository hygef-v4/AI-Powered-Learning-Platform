package edu.aiplatform.files;

import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StorageWriteException;
import edu.aiplatform.files.port.StoredFileNotFoundException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/** Dữ liệu tệp mẫu và kho lưu trữ trong bộ nhớ cho test. */
public final class FileFixtures {

    private FileFixtures() {}

    public static byte[] png() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] pdf() {
        return "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n"
                .getBytes(StandardCharsets.US_ASCII);
    }

    public static byte[] svgWithScript() {
        return ("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\">"
                + "<script>alert(1)</script></svg>").getBytes(StandardCharsets.UTF_8);
    }

    /** DOCX tối thiểu: ZIP có `[Content_Types].xml` ở đầu. */
    public static byte[] docx() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
                zip.write("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"/>"
                        .getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("word/document.xml"));
                zip.write("<w:document/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Tệp thực thi Windows (MZ) đổi tên thành ảnh. */
    public static byte[] exe() {
        byte[] b = new byte[512];
        b[0] = 'M';
        b[1] = 'Z';
        return b;
    }

    /** Kho trong bộ nhớ; có thể cho `put` ghi dở hoặc `delete` lỗi. */
    public static class MemoryStorage implements StoragePort {
        public final Map<String, byte[]> data = new HashMap<>();
        public final Map<String, Map<String, String>> meta = new HashMap<>();
        public boolean failMetadataWrite;
        public int deleteFailures;
        public int deleteCalls;

        @Override
        public String put(Path content, Map<String, String> metadata) {
            String id = UUID.randomUUID().toString();
            try {
                data.put(id, Files.readAllBytes(content));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (failMetadataWrite) {
                throw new StorageWriteException("metadata write failed", id, null);
            }
            Map<String, String> m = new LinkedHashMap<>(metadata);
            m.put(BYTE_SIZE, Integer.toString(data.get(id).length));
            meta.put(id, m);
            return id;
        }

        @Override
        public InputStream get(String fileId) {
            byte[] b = data.get(fileId);
            if (b == null) {
                throw new StoredFileNotFoundException("none");
            }
            return new ByteArrayInputStream(b);
        }

        @Override
        public Map<String, String> readMetadata(String fileId) {
            Map<String, String> m = meta.get(fileId);
            if (m == null) {
                throw new StoredFileNotFoundException("none");
            }
            return m;
        }

        @Override
        public void delete(String fileId) {
            deleteCalls++;
            if (deleteFailures > 0) {
                deleteFailures--;
                throw new IllegalStateException("delete failed");
            }
            data.remove(fileId);
            meta.remove(fileId);
        }
    }
}
