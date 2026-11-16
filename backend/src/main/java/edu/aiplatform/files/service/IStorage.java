package edu.aiplatform.files.service;

import edu.aiplatform.files.contract.ArtifactPurpose;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/** Nơi lưu byte tệp đang dùng: Google Drive, thư mục local hoặc "chưa cấu hình" (P5). */
public interface IStorage {

    boolean isAvailable();

    /** Lưu tệp kèm thuộc tính, trả {@code fileId}; lỗi giữa chừng thì tự xóa phần đã tạo trước khi ném lỗi. */
    String putFile(ArtifactPurpose purpose, Path content, StoredMetadata metadata);

    Optional<StoredMetadata> loadMetadata(String fileId);

    InputStream open(String fileId);

    /** Xóa tệp; tệp không còn thì coi như đã xóa. */
    void deleteFile(String fileId);
}
