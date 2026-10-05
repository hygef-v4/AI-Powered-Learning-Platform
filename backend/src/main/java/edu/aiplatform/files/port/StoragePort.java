package edu.aiplatform.files.port;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Google Drive hoặc thư mục local (P5); metadata đi cùng tệp, không có bảng PostgreSQL.
 * Nhận {@link Path} (file tạm) để adapter gửi lại được khi retry (P6).
 *
 * <p>Lỗi: {@link StorageUnavailableException} (tạm thời/cấu hình sai), {@link StorageWriteException} (đã tạo tệp
 * nhưng ghi dở, kèm `orphanFileId` để dọn), {@link FileUnavailableException} (Drive chặn vì abuse),
 * {@link StoredFileNotFoundException}.
 */
public interface StoragePort {

    /** Khóa metadata bắt buộc khi {@link #put}. */
    String OWNER_ACCOUNT_ID = "ownerAccountId";
    String PURPOSE = "purpose";
    String MEDIA_TYPE = "mediaType";
    String ORIGINAL_FILE_NAME = "originalFileName";
    String SHA256 = "sha256";
    /** Do adapter trả thêm trong {@link #readMetadata}. */
    String BYTE_SIZE = "byteSize";

    String put(Path content, Map<String, String> metadata);

    InputStream get(String fileId);

    Map<String, String> readMetadata(String fileId);

    /** Không có tệp thì coi như đã xóa. Chỉ dùng để dọn tệp upload lỗi (BR-U03-06, 32). */
    void delete(String fileId);
}
