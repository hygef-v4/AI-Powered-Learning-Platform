package edu.aiplatform.files.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/**
 * Xác định loại tệp từ nội dung (magic bytes) bằng Tika trên 8 KB đầu (BR-U03-03, P3). Tên tệp chỉ là gợi ý:
 * Tika chỉ dùng đuôi để chọn kiểu con khi kiểu đó tương thích với nội dung (ví dụ ZIP → DOCX), không thể biến
 * một tệp thực thi thành ảnh. Header `Content-Type` của trình duyệt không được dùng.
 */
@Component
public class ContentInspector {

    static final int PROBE_BYTES = 8 * 1024;

    private final Tika tika = new Tika();

    public String detect(Path file, String fileName) {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] probe = in.readNBytes(PROBE_BYTES);
            return tika.detect(probe, fileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
