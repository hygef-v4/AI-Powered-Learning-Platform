package edu.aiplatform.files.application;

import edu.aiplatform.files.domain.FileType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/**
 * Nhận dạng loại tệp bằng magic bytes (Tika, P3). Tên tệp chỉ dùng để phân biệt các loại Office cùng nền ZIP;
 * tệp đổi đuôi (ví dụ ảnh PNG đặt tên .pdf) vẫn nhận ra đúng là PNG.
 */
@Component
public class ContentInspector {

    private final Tika tika = new Tika();

    public Optional<FileType> detect(Path file, String originalFileName) {
        try (InputStream in = Files.newInputStream(file)) {
            String mediaType = tika.detect(in, originalFileName);
            return FileType.fromMediaType(mediaType);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
